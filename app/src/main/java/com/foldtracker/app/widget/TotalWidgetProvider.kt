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

class TotalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val total = StatsRepository(context).getFullStats().total
                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) {
                        updateOne(context, appWidgetManager, id, total)
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
        fun updateOne(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, total: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_total)
            views.setTextViewText(R.id.widget_total_count, total.toString())

            val pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_total_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
