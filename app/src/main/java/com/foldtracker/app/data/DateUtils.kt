package com.foldtracker.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun dayKeyFor(timestampMillis: Long): String = dayFormat.format(timestampMillis)

    fun todayKey(): String = dayFormat.format(System.currentTimeMillis())

    fun dayKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dayFormat.format(cal.time)
    }

    /** Short weekday label e.g. "Mon" for a yyyy-MM-dd key, used in the 7-day chart. */
    fun weekdayLabel(dayKey: String): String {
        return try {
            val date = dayFormat.parse(dayKey) ?: return dayKey
            SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            dayKey
        }
    }

    fun daysBetween(startDayKey: String, endDayKey: String): Int {
        return try {
            val start = dayFormat.parse(startDayKey)?.time ?: return 0
            val end = dayFormat.parse(endDayKey)?.time ?: return 0
            ((end - start) / (24 * 60 * 60 * 1000L)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /** Returns the Monday-of-week key (yyyy-MM-dd) that the given dayKey falls into. */
    fun weekStartKeyFor(dayKey: String): String {
        return try {
            val date = dayFormat.parse(dayKey) ?: return dayKey
            val cal = Calendar.getInstance()
            cal.time = date
            cal.firstDayOfWeek = Calendar.MONDAY
            val currentDow = cal.get(Calendar.DAY_OF_WEEK)
            val daysSinceMonday = ((currentDow - Calendar.MONDAY) + 7) % 7
            cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            dayFormat.format(cal.time)
        } catch (e: Exception) {
            dayKey
        }
    }

    /** Short display label for a week, e.g. "Jul 28" for the Monday of that week. */
    fun weekLabel(weekStartDayKey: String): String {
        return try {
            val date = dayFormat.parse(weekStartDayKey) ?: return weekStartDayKey
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            weekStartDayKey
        }
    }

    fun formatDate(timestampMillis: Long): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(timestampMillis)
    }

    fun formatTime(timestampMillis: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestampMillis)
    }

    /** Human-readable duration like "2h 14m" or "45s" for a duration in millis. */
    fun formatDuration(durationMillis: Long): String {
        if (durationMillis < 1000) return "<1s"
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
