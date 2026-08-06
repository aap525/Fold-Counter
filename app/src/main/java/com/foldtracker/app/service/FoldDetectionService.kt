package com.foldtracker.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.foldtracker.app.FoldTrackerApp
import com.foldtracker.app.MainActivity
import com.foldtracker.app.R
import com.foldtracker.app.data.Prefs
import com.foldtracker.app.data.StatsRepository
import com.foldtracker.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that observes the device's folding state using the Jetpack
 * WindowManager library (androidx.window). This is the officially supported way
 * to read fold/unfold posture changes on foldable devices such as the Galaxy Z Fold.
 *
 * IMPORTANT: androidx.window's WindowInfoTracker requires a UI Context (something
 * associated with a window/display), not a plain Context like a bare Service has.
 * We create a lightweight "window context" (TYPE_APPLICATION) tied to the default
 * display, which satisfies this requirement without needing any special overlay
 * permission and without showing any actual window on screen.
 *
 * Every transition from CLOSED (folded) -> not-CLOSED (unfolded/flat/half-open) is
 * counted as one "unfold" event, debounced to avoid double counting rapid hinge jitter.
 */
class FoldDetectionService : LifecycleService() {

    private lateinit var repo: StatsRepository
    private lateinit var prefs: Prefs
    private var lastEventTime = 0L

    override fun onCreate() {
        super.onCreate()
        repo = StatsRepository(applicationContext)
        prefs = Prefs(applicationContext)
        startForeground(NOTIFICATION_ID, buildNotification())
        observeFoldingState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun observeFoldingState() {
        val uiContext = createUiContext()
        if (uiContext == null) {
            // Device/OS combination can't give us a UI context to observe fold state from
            // a background service. Fail quietly rather than crashing the app.
            Log.w(TAG, "No UI context available; fold detection disabled on this device.")
            return
        }

        val tracker = WindowInfoTracker.getOrCreate(uiContext)
        lifecycleScope.launch {
            try {
                tracker.windowLayoutInfo(uiContext).collectLatest { info: WindowLayoutInfo ->
                    handleLayoutInfo(info)
                }
            } catch (e: Exception) {
                // Defensive: never let a failure here take down the whole app process.
                Log.e(TAG, "Fold state observation failed", e)
            }
        }
    }

    /**
     * Creates a minimal, invisible window context so WindowInfoTracker will accept
     * this service as an observer. Requires API 30+ (Android 11); on older versions
     * we simply can't observe fold state from the background this way.
     */
    private fun createUiContext(): Context? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION, null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create UI context", e)
            null
        }
    }

    private fun handleLayoutInfo(info: WindowLayoutInfo) {
        val foldingFeature = info.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull()

        // If there's no folding feature reported, we can't determine state; ignore.
        val isOpen = when {
            foldingFeature == null -> return
            foldingFeature.state == FoldingFeature.State.FLAT -> true
            foldingFeature.state == FoldingFeature.State.HALF_OPENED -> true
            else -> false // FoldingFeature.State can also represent CLOSED via occlusion type below
        }

        val wasOpen = prefs.lastFoldStateOpen
        prefs.lastFoldStateOpen = isOpen

        val now = System.currentTimeMillis()
        val debounced = now - lastEventTime < DEBOUNCE_MS

        if (!wasOpen && isOpen && !debounced && prefs.trackingEnabled) {
            lastEventTime = now
            lifecycleScope.launch {
                try {
                    repo.recordUnfold(now)
                    WidgetUpdater.updateAllWidgets(applicationContext)
                    updateNotification()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record unfold event", e)
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
        val today = prefs.cachedTodayCount
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
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        private const val TAG = "FoldDetectionService"
        private const val NOTIFICATION_ID = 1001
        private const val DEBOUNCE_MS = 800L

        fun start(context: Context) {
            try {
                val intent = Intent(context, FoldDetectionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FoldDetectionService::class.java))
        }
    }
}
