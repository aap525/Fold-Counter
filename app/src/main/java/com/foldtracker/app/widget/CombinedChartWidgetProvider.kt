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

class CombinedChartWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = StatsRepository(context)
                val daily = repo.getDailyCounts(7)
                val weekly = repo.getWeeklyCounts(6)
                val today = daily.lastOrNull()?.count ?: 0
                val thisWeek = repo.getCurrentWeekTotal()

                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) {
                        updateOne(
                            context, appWidgetManager, id,
                            dailyValues = daily.map { it.count },
                            weeklyValues = weekly.map { it.count },
                            today = today,
                            thisWeek = thisWeek
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
            dailyValues: List<Int>,
            weeklyValues: List<Int>,
            today: Int,
            thisWeek: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_combined_chart)
            views.setTextViewText(
                R.id.widget_combined_chart_header,
                context.getString(R.string.widget_chart_combined_header, today, thisWeek)
            )

            val dailyBitmap = WidgetChartRenderer.renderBarChart(
                context = context,
                values = dailyValues,
                labels = emptyList(),
                widthPx = 300,
                heightPx = 260
            )
            val weeklyBitmap = WidgetChartRenderer.renderBarChart(
                context = context,
                values = weeklyValues,
                labels = emptyList(),
                widthPx = 300,
                heightPx = 260
            )
            views.setImageViewBitmap(R.id.widget_combined_daily_chart_image, dailyBitmap)
            views.setImageViewBitmap(R.id.widget_combined_weekly_chart_image, weeklyBitmap)

            val pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_combined_chart_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
