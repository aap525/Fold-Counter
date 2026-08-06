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
}
