package com.foldtracker.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.foldtracker.app.R

/**
 * Draws a small, dependency-free bar chart into a Bitmap. Widgets (RemoteViews) can't
 * host arbitrary custom views or charting libraries, so we render the chart ourselves
 * and hand the widget a plain bitmap via ImageView.setImageBitmap. Includes a light
 * Y-axis (0 and max gridlines with labels) so the chart is readable on its own.
 */
object WidgetChartRenderer {

    fun renderBarChart(
        context: Context,
        values: List<Int>,
        labels: List<String> = emptyList(),
        widthPx: Int,
        heightPx: Int,
        showAxis: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val barColor = ContextCompat.getColor(context, R.color.brand_primary)
        val gridColor = ContextCompat.getColor(context, R.color.widget_divider)
        val labelColor = ContextCompat.getColor(context, R.color.widget_text_secondary)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
            style = Paint.Style.FILL
        }
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            strokeWidth = heightPx * 0.012f
        }
        val gridlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            strokeWidth = heightPx * 0.008f
            alpha = 120
        }
        val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.RIGHT
            textSize = heightPx * 0.12f
        }
        val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.CENTER
            textSize = heightPx * 0.12f
        }

        val hasXLabels = labels.isNotEmpty() && labels.size == values.size
        val xLabelAreaHeight = if (hasXLabels) heightPx * 0.18f else heightPx * 0.03f
        val axisAreaWidth = if (showAxis) widthPx * 0.16f else 0f
        val topPadding = heightPx * 0.08f

        val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)

        val chartLeft = axisAreaWidth
        val chartRight = widthPx.toFloat()
        val chartTop = topPadding
        val chartBottom = heightPx - xLabelAreaHeight
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)

        if (showAxis) {
            // Midpoint gridline (subtle)
            val midY = chartTop + chartHeight / 2f
            canvas.drawLine(chartLeft, midY, chartRight, midY, gridlinePaint)

            // Baseline (0) - solid
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, baselinePaint)

            // Axis labels: 0, half, max
            canvas.drawText("0", axisAreaWidth - (widthPx * 0.02f), chartBottom + axisTextPaint.textSize * 0.32f, axisTextPaint)
            canvas.drawText(maxValue.toString(), axisAreaWidth - (widthPx * 0.02f), chartTop + axisTextPaint.textSize * 0.35f, axisTextPaint)
        } else {
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, baselinePaint)
        }

        val count = values.size.coerceAtLeast(1)
        val spacing = chartWidth * 0.10f / count
        val totalSpacing = spacing * (count + 1)
        val barWidth = ((chartWidth - totalSpacing) / count).coerceAtLeast(2f)

        values.forEachIndexed { index, value ->
            val ratio = value.toFloat() / maxValue.toFloat()
            val barHeight = (chartHeight * ratio).coerceAtLeast(if (value > 0) 3f else 1f)
            val left = chartLeft + spacing + index * (barWidth + spacing)
            val right = left + barWidth
            val top = chartBottom - barHeight
            val bottom = chartBottom
            val radius = (barWidth * 0.28f).coerceAtMost(6f)
            canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, barPaint)

            if (hasXLabels) {
                canvas.drawText(labels[index], left + barWidth / 2f, heightPx.toFloat() - 2f, xLabelPaint)
            }
        }

        return bitmap
    }
}
