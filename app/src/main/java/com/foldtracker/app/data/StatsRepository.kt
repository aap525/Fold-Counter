package com.foldtracker.app.data

import android.content.Context

data class StatsSnapshot(
    val today: Int,
    val total: Int,
    val averagePerDay: Double,
    val last7Days: List<DayCount>,   // oldest -> newest, 7 entries, zero-filled
    val currentStreak: Int,
    val bestDayCount: Int,
    val trackedDays: Int
)

class StatsRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).unfoldDao()
    private val prefs = Prefs(context)

    suspend fun recordUnfold(timestamp: Long = System.currentTimeMillis()) {
        val dayKey = DateUtils.dayKeyFor(timestamp)
        dao.insert(UnfoldEvent(timestamp = timestamp, dayKey = dayKey))
        refreshCache()
    }

    /** Recomputes and stores the fast-access cache widgets read from. */
    suspend fun refreshCache() {
        val todayKey = DateUtils.todayKey()
        val today = dao.getCountForDay(todayKey)
        val total = dao.getTotalCount()
        prefs.cachedTodayCount = today
        prefs.cachedTotalCount = total
        prefs.cachedDayKey = todayKey
        updateStreak(today, todayKey)
    }

    private fun updateStreak(todayCount: Int, todayKey: String) {
        if (todayCount <= 0) return
        val lastDay = prefs.lastStreakDayKey
        if (lastDay == todayKey) return // already counted today
        val yesterday = DateUtils.dayKeyDaysAgo(1)
        prefs.lastKnownStreak = if (lastDay == yesterday) prefs.lastKnownStreak + 1 else 1
        prefs.lastStreakDayKey = todayKey
    }

    suspend fun getFullStats(): StatsSnapshot {
        val todayKey = DateUtils.todayKey()
        val today = dao.getCountForDay(todayKey)
        val total = dao.getTotalCount()
        val firstDayKey = dao.getFirstDayKey()
        val distinctDays = dao.getDistinctDayCount().coerceAtLeast(1)
        val average = if (distinctDays > 0) total.toDouble() / distinctDays else 0.0

        val startKey = DateUtils.dayKeyDaysAgo(6)
        val raw = dao.getDayCountsSince(startKey).associateBy { it.dayKey }
        val last7 = (6 downTo 0).map { offset ->
            val key = DateUtils.dayKeyDaysAgo(offset)
            DayCount(key, raw[key]?.count ?: 0)
        }

        val allDays = dao.getAllDayCounts()
        val bestDay = allDays.maxOfOrNull { it.count } ?: 0

        return StatsSnapshot(
            today = today,
            total = total,
            averagePerDay = average,
            last7Days = last7,
            currentStreak = prefs.lastKnownStreak,
            bestDayCount = bestDay,
            trackedDays = distinctDays
        )
    }

    suspend fun resetAllData() {
        dao.clearAll()
        prefs.cachedTodayCount = 0
        prefs.cachedTotalCount = 0
        prefs.lastKnownStreak = 0
        prefs.lastStreakDayKey = ""
        refreshCache()
    }
}
