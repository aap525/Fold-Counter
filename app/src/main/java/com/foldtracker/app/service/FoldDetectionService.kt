package com.foldtracker.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.foldtracker.app.FoldTrackerApp
import com.foldtracker.app.MainActivity
import com.foldtracker.app.R
import com.foldtracker.app.data.Prefs
import com.foldtracker.app.data.StatsRepository
import com.foldtracker.app.widget.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * Foreground service that detects fold/unfold using Android's core Configuration API,
 * specifically `smallestScreenWidthDp`. This value is the smaller of the device's two
 * screen dimensions and, by definition, does NOT change on simple rotation (it's always
 * the smaller side regardless of which way the phone is held) but DOES change sharply
 * when a foldable's screen area changes, i.e. when it's folded or unfolded.
 *
 * This deliberately avoids the Jetpack WindowManager library's FoldingFeature/WindowInfoTracker
 * API, which relies on a vendor-provided "extension" library on the device accessed via
 * reflection. On some Samsung firmware builds that binding can throw uncaught Errors (not
 * just Exceptions), which will crash the whole app if unguarded. Configuration monitoring
 * uses only long-stable, first-party Android APIs (Context.registerComponentCallbacks,
 * available since API 14) and needs no special window context, so it works reliably from
 * a plain background service.
 *
 * Every meaningful jump in smallestScreenWidthDp is treated as a fold state transition:
 * a big increase = unfolded (open), a big drop = folded (closed).
 */
class FoldDetectionService : LifecycleService(), ComponentCallbacks2 {

    private lateinit var repo: StatsRepository
    private lateinit var prefs: Prefs
    private var lastEventTime = 0L
    private var lastSmallestWidthDp = 0

    override fun onCreate() {
        super.onCreate()
        try {
            repo = StatsRepository(applicationContext)
            prefs = Prefs(applicationContext)
            // Baseline reflects whatever physical fold state we're already in, so a
            // service restart never causes a spurious count.
            lastSmallestWidthDp = resources.configuration.smallestScreenWidthDp
            registerComponentCallbacks(this)
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize FoldDetectionService", t)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        try {
            handleConfigurationChange(newConfig)
        } catch (t: Throwable) {
            Log.e(TAG, "Error handling configuration change", t)
        }
    }

    override fun onLowMemory() { /* no-op, required by ComponentCallbacks2 */ }

    override fun onTrimMemory(level: Int) { /* no-op, required by ComponentCallbacks2 */ }

    private fun handleConfigurationChange(newConfig: Configuration) {
        val newWidth = newConfig.smallestScreenWidthDp
        val delta = newWidth - lastSmallestWidthDp

        when {
            delta > FOLD_THRESHOLD_DP -> {
                lastSmallestWidthDp = newWidth
                onFoldTransition(isOpen = true)
            }
            delta < -FOLD_THRESHOLD_DP -> {
                lastSmallestWidthDp = newWidth
                onFoldTransition(isOpen = false)
            }
            else -> {
                // Small change (rotation doesn't move this value at all; minor changes
                // here are usually multi-window/display-density noise) - just re-baseline.
                lastSmallestWidthDp = newWidth
            }
        }
    }

    private fun onFoldTransition(isOpen: Boolean) {
        if (!::prefs.isInitialized || !prefs.trackingEnabled) return

        val now = System.currentTimeMillis()
        if (now - lastEventTime < DEBOUNCE_MS) return
        lastEventTime = now

        if (isOpen) {
            lifecycleScope.launch {
                try {
                    repo.recordUnfold(now)
                    WidgetUpdater.updateAllWidgets(applicationContext)
                    updateNotification()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to record unfold event", t)
                }
            }
        } else {
            lifecycleScope.launch {
                try {
                    repo.recordFold(now)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to record fold-close event", t)
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val today = if (::prefs.isInitialized) prefs.cachedTodayCount else 0
        return NotificationCompat.Builder(this, FoldTrackerApp.CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text, today))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to update notification", t)
        }
    }

    override fun onDestroy() {
        try {
            unregisterComponentCallbacks(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to unregister callbacks", t)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        private const val TAG = "FoldDetectionService"
        private const val NOTIFICATION_ID = 1001
        private const val DEBOUNCE_MS = 800L

        /** Minimum jump in smallestScreenWidthDp to count as a real fold transition,
         *  not rotation (which never moves this value) or minor display noise. */
        private const val FOLD_THRESHOLD_DP = 150

        fun start(context: Context) {
            try {
                val intent = Intent(context, FoldDetectionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to start service", t)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, FoldDetectionService::class.java))
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to stop service", t)
            }
        }
    }
}
