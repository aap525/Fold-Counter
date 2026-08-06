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
 * and hand the widget a plain bitmap via ImageView.setImageBitmap.
 */
object WidgetChartRenderer {

    fun renderBarChart(
        context: Context,
        values: List<Int>,
        labels: List<String> = emptyList(),
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val barColor = ContextCompat.getColor(context, R.color.brand_primary)
        val labelColor = ContextCompat.getColor(context, R.color.widget_text_secondary)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor
            textAlign = Paint.Align.CENTER
            textSize = heightPx * 0.13f
        }

        val hasLabels = labels.isNotEmpty() && labels.size == values.size
        val labelAreaHeight = if (hasLabels) heightPx * 0.22f else 0f
        val chartHeight = heightPx - labelAreaHeight
        val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        val count = values.size.coerceAtLeast(1)

        val spacing = widthPx * 0.05f
        val totalSpacing = spacing * (count + 1)
        val barWidth = ((widthPx - totalSpacing) / count).coerceAtLeast(2f)

        values.forEachIndexed { index, value ->
            val ratio = value.toFloat() / maxValue.toFloat()
            val barHeight = (chartHeight * ratio).coerceAtLeast(if (value > 0) 3f else 1f)
            val left = spacing + index * (barWidth + spacing)
            val right = left + barWidth
            val top = chartHeight - barHeight
            val bottom = chartHeight
            val radius = (barWidth * 0.3f).coerceAtMost(5f)
            canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, barPaint)

            if (hasLabels) {
                canvas.drawText(labels[index], left + barWidth / 2f, heightPx.toFloat() - 2f, textPaint)
            }
        }

        return bitmap
    }
}
