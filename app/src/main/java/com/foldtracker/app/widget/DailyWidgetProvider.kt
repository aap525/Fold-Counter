package com.foldtracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.foldtracker.app.MainActivity
import com.foldtracker.app.R
import com.foldtracker.app.data.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Always read live from the database, never from a cache, so this
                // widget can never disagree with what the app itself shows.
                val today = StatsRepository(context).getFullStats().today
                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) {
                        updateOne(context, appWidgetManager, id, today)
                    }
                }
            } catch (t: Throwable) {
                // Never let a widget refresh crash the app
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateOne(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, today: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily)
            views.setTextViewText(R.id.widget_daily_count, today.toString())

            val pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_daily_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
