package com.foldtracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.ContextCompat
import com.foldtracker.app.R

/**
 * Draws a small, polished bar chart into a Bitmap. Widgets (RemoteViews) can't host
 * arbitrary custom views or charting libraries, so we render the chart ourselves and
 * hand the widget a plain bitmap via ImageView.setImageBitmap.
 *
 * Design: each bar sits on a faint full-height "track" (a subtle capacity indicator),
 * past bars are shown muted, and the most recent bar (today / this week) is rendered
 * with a bright gradient fill so it draws the eye - a common, clean pattern in
 * analytics-style widgets. A light dashed midline and axis labels (0 / max) round it out.
 */
object WidgetChartRenderer {

    fun renderBarChart(
        context: Context,
        values: List<Int>,
        labels: List<String> = emptyList(),
        widthPx: Int,
        heightPx: Int,
        showAxis: Boolean = true,
        highlightLast: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accentColor = ContextCompat.getColor(context, R.color.brand_primary)
        val trackColor = ContextCompat.getColor(context, R.color.widget_divider)
        val labelColor = ContextCompat.getColor(context, R.color.widget_text_secondary)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            alpha = 70
            style = Paint.Style.FILL
        }
        val barMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 100
            style = Paint.Style.FILL
        }
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            strokeWidth = heightPx * 0.012f
        }
        val gridDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            strokeWidth = heightPx * 0.007f
            alpha = 110
            pathEffect = DashPathEffect(floatArrayOf(heightPx * 0.02f, heightPx * 0.02f), 0f)
        }
        val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.RIGHT
            textSize = heightPx * 0.105f
        }
        val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.CENTER
            textSize = heightPx * 0.10f
        }

        val hasXLabels = labels.isNotEmpty() && labels.size == values.size
        val xLabelAreaHeight = if (hasXLabels) heightPx * 0.16f else heightPx * 0.04f
        val axisAreaWidth = if (showAxis) widthPx * 0.15f else 0f
        val topPadding = heightPx * 0.10f
        val rightPadding = widthPx * 0.02f

        val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)

        val chartLeft = axisAreaWidth
        val chartRight = widthPx.toFloat() - rightPadding
        val chartTop = topPadding
        val chartBottom = heightPx - xLabelAreaHeight
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)

        if (showAxis) {
            val midY = chartTop + chartHeight / 2f
            canvas.drawLine(chartLeft, midY, chartRight, midY, gridDashPaint)
            canvas.drawText("0", axisAreaWidth - (widthPx * 0.025f), chartBottom + axisTextPaint.textSize * 0.32f, axisTextPaint)
            canvas.drawText(maxValue.toString(), axisAreaWidth - (widthPx * 0.025f), chartTop + axisTextPaint.textSize * 0.35f, axisTextPaint)
        }
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, baselinePaint)

        val count = values.size.coerceAtLeast(1)
        val spacing = chartWidth * 0.16f / count
        val totalSpacing = spacing * (count + 1)
        val barWidth = ((chartWidth - totalSpacing) / count).coerceAtLeast(2f)

        values.forEachIndexed { index, value ->
            val left = chartLeft + spacing + index * (barWidth + spacing)
            val right = left + barWidth
            val radius = (barWidth * 0.35f).coerceAtMost(9f)

            // Faint full-height track behind every bar - gives a sense of scale/capacity
            canvas.drawRoundRect(RectF(left, chartTop, right, chartBottom), radius, radius, trackPaint)

            val ratio = value.toFloat() / maxValue.toFloat()
            val barHeight = (chartHeight * ratio).coerceAtLeast(if (value > 0) 3f else 0f)
            if (barHeight > 0f) {
                val top = chartBottom - barHeight
                val isLast = highlightLast && index == values.lastIndex
                val paint = if (isLast) {
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        shader = LinearGradient(
                            0f, top, 0f, chartBottom,
                            lighten(accentColor, 0.30f), accentColor,
                            Shader.TileMode.CLAMP
                        )
                    }
                } else {
                    barMutedPaint
                }
                canvas.drawRoundRect(RectF(left, top, right, chartBottom), radius, radius, paint)
            }

            if (hasXLabels) {
                canvas.drawText(labels[index], left + barWidth / 2f, heightPx.toFloat() - 2f, xLabelPaint)
            }
        }

        return bitmap
    }

    private fun lighten(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
