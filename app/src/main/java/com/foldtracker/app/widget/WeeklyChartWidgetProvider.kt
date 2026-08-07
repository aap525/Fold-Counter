package com.foldtracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.foldtracker.app.MainActivity
import com.foldtracker.app.R
import com.foldtracker.app.data.DateUtils
import com.foldtracker.app.data.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeeklyChartWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = StatsRepository(context)
                val weekly = repo.getWeeklyCounts(6)
                val thisWeek = repo.getCurrentWeekTotal()

                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) {
                        updateOne(
                            context, appWidgetManager, id,
                            weekly.map { it.count },
                            weekly.map { DateUtils.weekLabel(it.weekStartKey) },
                            thisWeek
                        )
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
        fun updateOne(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            values: List<Int>,
            labels: List<String>,
            thisWeek: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_weekly_chart)
            views.setTextViewText(R.id.widget_weekly_chart_header, context.getString(R.string.widget_chart_week_header, thisWeek))

            // Weekly labels are date-like ("Jul 28"), too wide for a compact widget - skip
            // on-chart labels here and rely on the header text for context instead.
            val bitmap = WidgetChartRenderer.renderBarChart(
                context = context,
                values = values,
                labels = emptyList(),
                widthPx = 480,
                heightPx = 220
            )
            views.setImageViewBitmap(R.id.widget_weekly_chart_image, bitmap)

            val pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_weekly_chart_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
