package com.kinly.famapp.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kinly.famapp.R
import com.kinly.famapp.data.models.Notification
import com.kinly.famapp.data.models.ShoppingItem
import com.kinly.famapp.data.models.Task
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import java.util.concurrent.TimeUnit

object WidgetWork {
    private const val UNIQUE = "famapp_widget_refresh"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Разовое немедленное обновление (например, при добавлении виджета). */
    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun supabase(): SupabaseClient
}

/** Отметить товар купленным / задачу выполненной прямо из виджета. */
class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ItemIdKey] ?: return
        val kind = parameters[ItemKindKey] ?: return

        // 1) Сразу убираем пункт из виджета — не дожидаясь сети, чтобы галочка не «отскакивала».
        WidgetData.removeItem(context, kind, id)
        FamilyWidget().updateAll(context)

        // 2) Фиксируем изменение в базе и подтягиваем актуальный список.
        val supabase = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .supabase()
        runCatching {
            supabase.auth.awaitInitialization()
            if (supabase.auth.currentSessionOrNull() == null) return
            if (kind == "task") {
                supabase.postgrest.rpc("complete_task", buildJsonObject { put("p_task_id", id) })
            } else {
                supabase.postgrest.rpc("mark_shopping_item_checked", buildJsonObject {
                    put("p_item_id", id); put("p_checked", true)
                })
            }
            val familyId = WidgetData.readFamilyId(context)
            if (familyId != null) {
                val shopping = supabase.postgrest["shopping_items"].select {
                    filter { eq("family_id", familyId); eq("is_checked", false) }
                }.decodeList<ShoppingItem>()
                val tasks = supabase.postgrest["tasks"].select {
                    filter { eq("family_id", familyId); eq("is_completed", false) }
                }.decodeList<Task>()
                WidgetData.writeShopping(context, shopping.map { WidgetItem(it.id, it.title) })
                WidgetData.writeTasks(context, tasks.map { WidgetItem(it.id, it.title) })
                FamilyWidget().updateAll(context)
            }
        }
    }
}

/** Фоновое обновление снимка виджета из Supabase (по расписанию, без открытия приложения). */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val familyId = WidgetData.readFamilyId(ctx) ?: return Result.success()
        val supabase = EntryPointAccessors
            .fromApplication(ctx, WidgetEntryPoint::class.java)
            .supabase()

        return runCatching {
            // Дожидаемся восстановления сессии из хранилища; без авторизации RLS не отдаст данные.
            supabase.auth.awaitInitialization()
            if (supabase.auth.currentSessionOrNull() == null) return Result.success()

            // 1) Данные для виджета
            val shopping = supabase.postgrest["shopping_items"].select {
                filter { eq("family_id", familyId); eq("is_checked", false) }
            }.decodeList<ShoppingItem>()
            val tasks = supabase.postgrest["tasks"].select {
                filter { eq("family_id", familyId); eq("is_completed", false) }
            }.decodeList<Task>()
            WidgetData.writeShopping(ctx, shopping.map { WidgetItem(it.id, it.title) })
            WidgetData.writeTasks(ctx, tasks.map { WidgetItem(it.id, it.title) })
            FamilyWidget().updateAll(ctx)

            // 2) Новые уведомления -> в шторку телефона
            pushNewNotifications(ctx, supabase)

            Result.success()
        }.getOrElse { Result.retry() }
    }

    private suspend fun pushNewNotifications(ctx: Context, supabase: SupabaseClient) {
        val userId = WidgetData.readUserId(ctx) ?: return
        val lastSeen = WidgetData.readLastSeen(ctx)

        val fresh = supabase.postgrest["notifications"].select {
            filter {
                eq("user_id", userId)
                if (lastSeen != null) gt("created_at", lastSeen)
            }
            order("created_at", Order.ASCENDING)
            limit(20)
        }.decodeList<Notification>()

        if (fresh.isEmpty()) {
            // Первый запуск: запоминаем точку отсчёта, чтобы не сыпать старыми.
            if (lastSeen == null) WidgetData.writeLastSeen(ctx, nowIso())
            return
        }

        ensureChannel(ctx)
        val canPost = ContextCompat.checkSelfPermission(
            ctx, "android.permission.POST_NOTIFICATIONS"
        ) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            val manager = NotificationManagerCompat.from(ctx)
            fresh.forEach { n ->
                val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(n.title)
                    .setContentText(n.body ?: "")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(n.body ?: ""))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                runCatching { manager.notify(n.id.hashCode(), notif) }
            }
        }
        // Сдвигаем точку отсчёта на самое свежее, даже если показать не смогли.
        fresh.lastOrNull()?.createdAt?.let { WidgetData.writeLastSeen(ctx, it) }
    }

    private fun ensureChannel(ctx: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Семья", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Товары, задачи и события семьи"
                    }
                )
            }
        }
    }

    private fun nowIso(): String =
        java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString()

    companion object {
        const val CHANNEL_ID = "famapp_family"
    }
}
