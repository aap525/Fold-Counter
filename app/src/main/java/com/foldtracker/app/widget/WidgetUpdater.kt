package com.foldtracker.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdater {

    fun updateAllWidgets(context: Context) {
        updateWidget(context, DailyWidgetProvider::class.java)
        updateWidget(context, TotalWidgetProvider::class.java)
        updateWidget(context, CombinedWidgetProvider::class.java)
    }

    private fun updateWidget(context: Context, provider: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, provider)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val intent = Intent(context, provider).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
