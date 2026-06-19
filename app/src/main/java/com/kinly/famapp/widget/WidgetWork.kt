package com.kinly.famapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kinly.famapp.data.models.ShoppingItem
import com.kinly.famapp.data.models.Task
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.util.concurrent.TimeUnit

object WidgetWork {
    private const val UNIQUE = "famapp_widget_refresh"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build()
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
        val familyId = WidgetData.readFamilyId(applicationContext) ?: return Result.success()
        val supabase = EntryPointAccessors
            .fromApplication(applicationContext, WidgetEntryPoint::class.java)
            .supabase()

        return runCatching {
            // Дожидаемся восстановления сессии из хранилища; без авторизации RLS не отдаст данные.
            supabase.auth.awaitInitialization()
            if (supabase.auth.currentSessionOrNull() == null) return Result.success()

            val shopping = supabase.postgrest["shopping_items"].select {
                filter { eq("family_id", familyId); eq("is_checked", false) }
            }.decodeList<ShoppingItem>()

            val tasks = supabase.postgrest["tasks"].select {
                filter { eq("family_id", familyId); eq("is_completed", false) }
            }.decodeList<Task>()

            WidgetData.writeShopping(applicationContext, shopping.map { it.title })
            WidgetData.writeTasks(applicationContext, tasks.map { it.title })
            FamilyWidget().updateAll(applicationContext)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
