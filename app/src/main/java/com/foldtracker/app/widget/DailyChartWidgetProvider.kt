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

class DailyChartWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = StatsRepository(context)
                val daily = repo.getDailyCounts(7)
                val today = daily.lastOrNull()?.count ?: 0

                withContext(Dispatchers.Main) {
                    for (id in appWidgetIds) {
                        updateOne(context, appWidgetManager, id, daily.map { it.count }, daily.map { DateUtils.weekdayLabel(it.dayKey).take(1) }, today)
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
            today: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_chart)
            views.setTextViewText(R.id.widget_daily_chart_header, context.getString(R.string.widget_chart_today_header, today))

            val bitmap = WidgetChartRenderer.renderBarChart(
                context = context,
                values = values,
                labels = labels,
                widthPx = 320,
                heightPx = 150
            )
            views.setImageViewBitmap(R.id.widget_daily_chart_image, bitmap)

            val pendingIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_daily_chart_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
