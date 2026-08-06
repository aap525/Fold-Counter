package com.foldtracker.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FoldTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Fold tracking (background)",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Quiet ongoing notification while unfold tracking runs in the background"
            setShowBadge(false)
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_SUMMARY,
            "Daily summary",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "A once-a-day summary of your unfold count"
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(summaryChannel)
    }

    companion object {
        const val CHANNEL_SERVICE = "fold_service_channel"
        const val CHANNEL_SUMMARY = "fold_summary_channel"
    }
}
