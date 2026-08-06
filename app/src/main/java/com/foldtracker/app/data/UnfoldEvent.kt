package com.foldtracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unfold_events")
data class UnfoldEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,      // epoch millis
    val dayKey: String        // yyyy-MM-dd, in device local time, for fast grouping
)
