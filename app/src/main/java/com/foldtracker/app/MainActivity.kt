package com.foldtracker.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foldtracker.app.data.DateUtils
import com.foldtracker.app.data.Prefs
import com.foldtracker.app.data.StatsRepository
import com.foldtracker.app.databinding.ActivityMainBinding
import com.foldtracker.app.service.FoldDetectionService
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: StatsRepository
    private lateinit var prefs: Prefs

    private val notifPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* no-op, either way we proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = StatsRepository(this)
        prefs = Prefs(this)

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.batteryBanner.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        binding.resumeTrackingButton.setOnClickListener {
            prefs.trackingEnabled = true
            ensureServiceRunning()
            refreshUi()
        }

        requestNotificationPermissionIfNeeded()
        ensureServiceRunning()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        updateBatteryBannerVisibility()
    }

    private fun ensureServiceRunning() {
        if (prefs.trackingEnabled) {
            FoldDetectionService.start(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun updateBatteryBannerVisibility() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val ignoring = powerManager.isIgnoringBatteryOptimizations(packageName)
        binding.batteryBanner.visibility = if (ignoring) View.GONE else View.VISIBLE
        binding.pausedBanner.visibility = if (prefs.trackingEnabled) View.GONE else View.VISIBLE
    }

    private fun refreshUi() {
        lifecycleScope.launch {
            val stats = repo.getFullStats()

            binding.todayCount.text = stats.today.toString()
            binding.totalCount.text = stats.total.toString()
            binding.averageCount.text = String.format("%.1f", stats.averagePerDay)

            if (prefs.streaksEnabled) {
                binding.streakCard.visibility = View.VISIBLE
                binding.streakValue.text = getString(R.string.streak_days, stats.currentStreak)
            } else {
                binding.streakCard.visibility = View.GONE
            }

            if (prefs.dailyGoalEnabled) {
                binding.goalCard.visibility = View.VISIBLE
                val goal = prefs.dailyGoal
                binding.goalProgress.max = goal
                binding.goalProgress.progress = stats.today.coerceAtMost(goal)
                binding.goalText.text = getString(R.string.daily_goal_progress, stats.today, goal)
            } else {
                binding.goalCard.visibility = View.GONE
            }

            binding.bestDayText.text = getString(R.string.best_day, stats.bestDayCount)
            binding.trackedDaysText.text = getString(R.string.tracked_days, stats.trackedDays)

            renderChart(stats.last7Days.map { it.dayKey to it.count })
        }
    }

    private fun renderChart(data: List<Pair<String, Int>>) {
        val entries = data.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val dataSet = BarDataSet(entries, "").apply {
            color = getColor(R.color.brand_primary)
            setDrawValues(true)
            valueTextSize = 10f
        }
        binding.weekChart.apply {
            this.data = BarData(dataSet).apply { barWidth = 0.6f }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(data.map { DateUtils.weekdayLabel(it.first) })
                granularity = 1f
                setDrawGridLines(false)
                textColor = getColor(R.color.text_secondary)
            }
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                textColor = getColor(R.color.text_secondary)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setTouchEnabled(false)
            animateY(500)
            invalidate()
        }
    }
}
