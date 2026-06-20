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
            WidgetData.writeShopping(ctx, shopping.map { it.title })
            WidgetData.writeTasks(ctx, tasks.map { it.title })
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
