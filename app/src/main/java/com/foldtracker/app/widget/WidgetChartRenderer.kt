package com.foldtracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * Draws a polished bar chart into a Bitmap. Widgets (RemoteViews) can't host arbitrary
 * custom views or charting libraries, so we render the chart ourselves and hand the
 * widget a plain bitmap via ImageView.setImageBitmap.
 *
 * Design: each bar sits on a faint full-height "track" (a subtle capacity indicator);
 * past bars are shown muted; the most recent bar (today / this week) gets a bright
 * gradient fill with a soft glow beneath it and its value printed above it, so it draws
 * the eye immediately. Colors come from WidgetTheme, which explicitly branches on
 * light/dark mode rather than relying on automatic resource resolution.
 */
object WidgetChartRenderer {

    fun renderBarChart(
        context: Context,
        values: List<Int>,
        labels: List<String> = emptyList(),
        widthPx: Int,
        heightPx: Int,
        showAxis: Boolean = true,
        highlightLast: Boolean = true,
        showValueOnHighlight: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = WidgetTheme.palette(context)

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.track
            style = Paint.Style.FILL
        }
        val barMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.mutedBar
            alpha = 140
            style = Paint.Style.FILL
        }
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.gridline
            strokeWidth = heightPx * 0.012f
        }
        val gridDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.gridline
            strokeWidth = heightPx * 0.007f
            alpha = 170
            pathEffect = DashPathEffect(floatArrayOf(heightPx * 0.02f, heightPx * 0.02f), 0f)
        }
        val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textAlign = Paint.Align.RIGHT
            textSize = heightPx * 0.105f
        }
        val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textAlign = Paint.Align.CENTER
            textSize = heightPx * 0.10f
        }
        val valueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textAlign = Paint.Align.CENTER
            textSize = heightPx * 0.14f
            isFakeBoldText = true
        }

        val hasXLabels = labels.isNotEmpty() && labels.size == values.size
        val xLabelAreaHeight = if (hasXLabels) heightPx * 0.16f else heightPx * 0.04f
        val axisAreaWidth = if (showAxis) widthPx * 0.15f else 0f
        val topPadding = heightPx * (if (showValueOnHighlight) 0.20f else 0.10f)
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
            val radius = (barWidth * 0.4f).coerceAtMost(10f)

            // Faint full-height track behind every bar - gives a sense of scale/capacity
            canvas.drawRoundRect(RectF(left, chartTop, right, chartBottom), radius, radius, trackPaint)

            val ratio = value.toFloat() / maxValue.toFloat()
            val barHeight = (chartHeight * ratio).coerceAtLeast(if (value > 0) 3f else 0f)
            if (barHeight > 0f) {
                val top = chartBottom - barHeight
                val isLast = highlightLast && index == values.lastIndex

                if (isLast) {
                    // Soft glow beneath the highlighted bar
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = palette.gradientEnd
                        setShadowLayer(heightPx * 0.06f, 0f, heightPx * 0.015f, withAlpha(palette.gradientEnd, 130))
                    }
                    canvas.drawRoundRect(RectF(left, top, right, chartBottom), radius, radius, glowPaint)

                    val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        shader = LinearGradient(
                            0f, top, 0f, chartBottom,
                            palette.gradientStart, palette.gradientEnd,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRoundRect(RectF(left, top, right, chartBottom), radius, radius, gradientPaint)

                    if (showValueOnHighlight) {
                        canvas.drawText(value.toString(), left + barWidth / 2f, top - heightPx * 0.035f, valueLabelPaint)
                    }
                } else {
                    canvas.drawRoundRect(RectF(left, top, right, chartBottom), radius, radius, barMutedPaint)
                }
            }

            if (hasXLabels) {
                canvas.drawText(labels[index], left + barWidth / 2f, heightPx.toFloat() - 2f, xLabelPaint)
            }
        }

        return bitmap
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }
}
