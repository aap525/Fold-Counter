package com.foldtracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unfold_events")
data class UnfoldEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,               // epoch millis when unfolded (session start)
    val dayKey: String,                // yyyy-MM-dd, in device local time, for fast grouping
    val closeTimestamp: Long? = null,  // epoch millis when folded back closed; null while still open
    val durationMillis: Long? = null   // closeTimestamp - timestamp, filled in once closed
)
