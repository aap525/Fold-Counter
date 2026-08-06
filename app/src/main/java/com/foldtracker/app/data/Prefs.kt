package com.foldtracker.app.data

import android.content.Context
import androidx.core.content.edit

/**
 * Lightweight settings + fast-cache store. Widgets read cached counts from here
 * so they can update instantly without waiting on a Room query.
 */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("foldtracker_prefs", Context.MODE_PRIVATE)

    var trackingEnabled: Boolean
        get() = sp.getBoolean(KEY_TRACKING_ENABLED, true)
        set(value) = sp.edit { putBoolean(KEY_TRACKING_ENABLED, value) }

    var streaksEnabled: Boolean
        get() = sp.getBoolean(KEY_STREAKS_ENABLED, true)
        set(value) = sp.edit { putBoolean(KEY_STREAKS_ENABLED, value) }

    var dailyGoalEnabled: Boolean
        get() = sp.getBoolean(KEY_GOAL_ENABLED, true)
        set(value) = sp.edit { putBoolean(KEY_GOAL_ENABLED, value) }

    var notificationsEnabled: Boolean
        get() = sp.getBoolean(KEY_NOTIF_ENABLED, true)
        set(value) = sp.edit { putBoolean(KEY_NOTIF_ENABLED, value) }

    var dailyGoal: Int
        get() = sp.getInt(KEY_DAILY_GOAL, 50)
        set(value) = sp.edit { putInt(KEY_DAILY_GOAL, value) }

    var lastKnownStreak: Int
        get() = sp.getInt(KEY_LAST_STREAK, 0)
        set(value) = sp.edit { putInt(KEY_LAST_STREAK, value) }

    var lastStreakDayKey: String
        get() = sp.getString(KEY_LAST_STREAK_DAY, "") ?: ""
        set(value) = sp.edit { putString(KEY_LAST_STREAK_DAY, value) }

    // Fast cache for widgets (avoids DB hit on every widget redraw)
    var cachedTodayCount: Int
        get() = sp.getInt(KEY_CACHE_TODAY, 0)
        set(value) = sp.edit { putInt(KEY_CACHE_TODAY, value) }

    var cachedTotalCount: Int
        get() = sp.getInt(KEY_CACHE_TOTAL, 0)
        set(value) = sp.edit { putInt(KEY_CACHE_TOTAL, value) }

    var cachedDayKey: String
        get() = sp.getString(KEY_CACHE_DAY, "") ?: ""
        set(value) = sp.edit { putString(KEY_CACHE_DAY, value) }

    var lastFoldStateOpen: Boolean
        get() = sp.getBoolean(KEY_LAST_FOLD_STATE, false)
        set(value) = sp.edit { putBoolean(KEY_LAST_FOLD_STATE, value) }

    companion object {
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_STREAKS_ENABLED = "streaks_enabled"
        private const val KEY_GOAL_ENABLED = "goal_enabled"
        private const val KEY_NOTIF_ENABLED = "notif_enabled"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_LAST_STREAK = "last_streak"
        private const val KEY_LAST_STREAK_DAY = "last_streak_day"
        private const val KEY_CACHE_TODAY = "cache_today"
        private const val KEY_CACHE_TOTAL = "cache_total"
        private const val KEY_CACHE_DAY = "cache_day"
        private const val KEY_LAST_FOLD_STATE = "last_fold_state_open"
    }
}
