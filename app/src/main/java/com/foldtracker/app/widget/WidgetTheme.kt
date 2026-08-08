package com.foldtracker.app.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/** Color palette for hand-drawn widget chart bitmaps. */
data class ChartPalette(
    val track: Int,
    val mutedBar: Int,
    val gradientStart: Int,
    val gradientEnd: Int,
    val gridline: Int,
    val textPrimary: Int,
    val textSecondary: Int
)

/**
 * Widget bitmap charts are drawn pixel-by-pixel via Canvas, so they don't automatically
 * pick up day/night theming the way RemoteViews' own color-resource attributes do. This
 * explicitly checks system dark mode and hands back a hand-tuned palette for each case,
 * so the charts are guaranteed to look intentional (not just "the light version with
 * lower contrast") in dark mode.
 */
object WidgetTheme {

    fun isNightMode(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun palette(context: Context): ChartPalette {
        return if (isNightMode(context)) {
            ChartPalette(
                track = Color.parseColor("#33333F"),
                mutedBar = Color.parseColor("#5B5FEF"),
                gradientStart = Color.parseColor("#A6A8FF"),
                gradientEnd = Color.parseColor("#8285FF"),
                gridline = Color.parseColor("#3A3A46"),
                textPrimary = Color.parseColor("#F2F2F7"),
                textSecondary = Color.parseColor("#A0A0AC")
            )
        } else {
            ChartPalette(
                track = Color.parseColor("#EDEDF7"),
                mutedBar = Color.parseColor("#9698F5"),
                gradientStart = Color.parseColor("#7B7EF7"),
                gradientEnd = Color.parseColor("#5B5FEF"),
                gridline = Color.parseColor("#E4E4EC"),
                textPrimary = Color.parseColor("#1B1B23"),
                textSecondary = Color.parseColor("#6B6B78")
            )
        }
    }
}
