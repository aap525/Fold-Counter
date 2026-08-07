package com.foldtracker.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * All date/time handling for the app. Deliberately built on java.time (LocalDate,
 * Instant, DateTimeFormatter) rather than the older SimpleDateFormat/Calendar APIs.
 * SimpleDateFormat instances are NOT thread-safe, and this app calls into date logic
 * concurrently from several places at once (the background service, multiple widgets
 * updating in parallel, and the UI) - a shared SimpleDateFormat under that kind of
 * concurrent access can silently corrupt and return the wrong date. java.time's types
 * are immutable and safe to share across threads with no such risk.
 */
object DateUtils {
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private fun zone(): ZoneId = ZoneId.systemDefault()

    fun dayKeyFor(timestampMillis: Long): String =
        Instant.ofEpochMilli(timestampMillis).atZone(zone()).toLocalDate().format(dayFormatter)

    fun todayKey(): String = LocalDate.now(zone()).format(dayFormatter)

    fun dayKeyDaysAgo(daysAgo: Int): String =
        LocalDate.now(zone()).minusDays(daysAgo.toLong()).format(dayFormatter)

    /** Short weekday label e.g. "Mon" for a yyyy-MM-dd key. */
    fun weekdayLabel(dayKey: String): String {
        return try {
            LocalDate.parse(dayKey, dayFormatter).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        } catch (e: Exception) {
            dayKey
        }
    }

    fun daysBetween(startDayKey: String, endDayKey: String): Int {
        return try {
            val start = LocalDate.parse(startDayKey, dayFormatter)
            val end = LocalDate.parse(endDayKey, dayFormatter)
            ChronoUnit.DAYS.between(start, end).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /** Returns the Monday-of-week key (yyyy-MM-dd) that the given dayKey falls into. */
    fun weekStartKeyFor(dayKey: String): String {
        return try {
            val date = LocalDate.parse(dayKey, dayFormatter)
            val daysSinceMonday = date.dayOfWeek.value - 1 // Monday=1..Sunday=7
            date.minusDays(daysSinceMonday.toLong()).format(dayFormatter)
        } catch (e: Exception) {
            dayKey
        }
    }

    /** Short display label for a week, e.g. "Jul 28" for the Monday of that week. */
    fun weekLabel(weekStartDayKey: String): String {
        return try {
            val date = LocalDate.parse(weekStartDayKey, dayFormatter)
            date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        } catch (e: Exception) {
            weekStartDayKey
        }
    }

    /** Short calendar date label e.g. "Jul 15" for any dayKey - used for daily chart axes
     *  where a plain weekday letter would be ambiguous (both Tue/Thu show "T", etc). */
    fun dateLabel(dayKey: String): String = weekLabel(dayKey)

    fun formatDate(timestampMillis: Long): String {
        val date = Instant.ofEpochMilli(timestampMillis).atZone(zone()).toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }

    fun formatTime(timestampMillis: Long): String {
        val time = Instant.ofEpochMilli(timestampMillis).atZone(zone())
        return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
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
