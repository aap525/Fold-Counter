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

data class WeekCount(val weekStartKey: String, val count: Int)

class StatsRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).unfoldDao()
    private val prefs = Prefs(context)

    suspend fun recordUnfold(timestamp: Long = System.currentTimeMillis()) {
        val dayKey = DateUtils.dayKeyFor(timestamp)
        dao.insert(UnfoldEvent(timestamp = timestamp, dayKey = dayKey))
        refreshCache()
    }

    /** Call when the phone is folded closed, to close out the current open session with a duration. */
    suspend fun recordFold(timestamp: Long = System.currentTimeMillis()) {
        val openSession = dao.getLatestOpenSession() ?: return
        val duration = (timestamp - openSession.timestamp).coerceAtLeast(0)
        dao.closeSession(openSession.id, timestamp, duration)
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

    /** Daily counts for the last [days] days, oldest -> newest, zero-filled for gaps. */
    suspend fun getDailyCounts(days: Int): List<DayCount> {
        val startKey = DateUtils.dayKeyDaysAgo(days - 1)
        val raw = dao.getDayCountsSince(startKey).associateBy { it.dayKey }
        return (days - 1 downTo 0).map { offset ->
            val key = DateUtils.dayKeyDaysAgo(offset)
            DayCount(key, raw[key]?.count ?: 0)
        }
    }

    /** Weekly totals for the last [weeks] weeks (Monday-start), oldest -> newest. */
    suspend fun getWeeklyCounts(weeks: Int): List<WeekCount> {
        val startKey = DateUtils.dayKeyDaysAgo(weeks * 7)
        val dayCounts = dao.getDayCountsSince(startKey)
        val byWeek = LinkedHashMap<String, Int>()
        // Pre-fill week buckets so empty weeks show as zero, not missing.
        for (i in (weeks - 1) downTo 0) {
            val anchorDay = DateUtils.dayKeyDaysAgo(i * 7)
            val weekKey = DateUtils.weekStartKeyFor(anchorDay)
            byWeek.putIfAbsent(weekKey, 0)
        }
        dayCounts.forEach { dc ->
            val weekKey = DateUtils.weekStartKeyFor(dc.dayKey)
            byWeek[weekKey] = (byWeek[weekKey] ?: 0) + dc.count
        }
        return byWeek.entries.sortedBy { it.key }.map { WeekCount(it.key, it.value) }
    }

    /** Total unfolds so far in the current (Monday-start) week. */
    suspend fun getCurrentWeekTotal(): Int {
        val weekStart = DateUtils.weekStartKeyFor(DateUtils.todayKey())
        return dao.getDayCountsSince(weekStart).sumOf { it.count }
    }

    /** Most recent fold sessions (with duration if closed) for the History screen. */
    suspend fun getRecentEvents(limit: Int = 200): List<UnfoldEvent> {
        return dao.getRecentEvents(limit)
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
