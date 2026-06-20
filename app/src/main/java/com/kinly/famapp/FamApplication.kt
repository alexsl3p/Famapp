package com.kinly.famapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kinly.famapp.widget.WidgetRefreshWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Канал уведомлений нужен заранее, иначе системный тумблер «Разрешение уведомлений» серый.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(WidgetRefreshWorker.CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        WidgetRefreshWorker.CHANNEL_ID,
                        "Семья",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Товары, задачи и события семьи" }
                )
            }
        }
    }
}
