package com.foldtracker.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DayCount(val dayKey: String, val count: Int)

@Dao
interface UnfoldDao {

    @Insert
    suspend fun insert(event: UnfoldEvent): Long

    @Query("SELECT COUNT(*) FROM unfold_events")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM unfold_events")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM unfold_events WHERE dayKey = :dayKey")
    suspend fun getCountForDay(dayKey: String): Int

    @Query("SELECT COUNT(*) FROM unfold_events WHERE dayKey = :dayKey")
    fun getCountForDayFlow(dayKey: String): Flow<Int>

    @Query("SELECT dayKey, COUNT(*) as count FROM unfold_events GROUP BY dayKey ORDER BY dayKey DESC")
    suspend fun getAllDayCounts(): List<DayCount>

    @Query("SELECT dayKey, COUNT(*) as count FROM unfold_events WHERE dayKey >= :startDayKey GROUP BY dayKey ORDER BY dayKey ASC")
    suspend fun getDayCountsSince(startDayKey: String): List<DayCount>

    @Query("SELECT dayKey, COUNT(*) as count FROM unfold_events WHERE dayKey >= :startDayKey GROUP BY dayKey ORDER BY dayKey ASC")
    fun getDayCountsSinceFlow(startDayKey: String): Flow<List<DayCount>>

    @Query("SELECT MIN(dayKey) FROM unfold_events")
    suspend fun getFirstDayKey(): String?

    @Query("SELECT COUNT(DISTINCT dayKey) FROM unfold_events")
    suspend fun getDistinctDayCount(): Int

    @Query("SELECT * FROM unfold_events WHERE closeTimestamp IS NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestOpenSession(): UnfoldEvent?

    @Query("UPDATE unfold_events SET closeTimestamp = :closeTimestamp, durationMillis = :durationMillis WHERE id = :id")
    suspend fun closeSession(id: Long, closeTimestamp: Long, durationMillis: Long)

    @Query("SELECT * FROM unfold_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<UnfoldEvent>

    @Query("DELETE FROM unfold_events")
    suspend fun clearAll(): Int
}
