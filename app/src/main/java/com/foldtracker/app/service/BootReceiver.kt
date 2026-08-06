package com.foldtracker.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foldtracker.app.data.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = Prefs(context)
            if (prefs.trackingEnabled) {
                FoldDetectionService.start(context)
            }
        }
    }
}
