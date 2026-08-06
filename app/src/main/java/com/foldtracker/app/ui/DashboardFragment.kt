package com.foldtracker.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.foldtracker.app.R
import com.foldtracker.app.SettingsActivity
import com.foldtracker.app.data.DateUtils
import com.foldtracker.app.data.Prefs
import com.foldtracker.app.data.StatsRepository
import com.foldtracker.app.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: StatsRepository
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        repo = StatsRepository(ctx)
        prefs = Prefs(ctx)

        binding.batteryBanner.setOnClickListener { requestIgnoreBatteryOptimizations() }
        binding.resumeTrackingButton.setOnClickListener {
            prefs.trackingEnabled = true
            refreshUi()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        updateBannerVisibility()
    }

    private fun requestIgnoreBatteryOptimizations() {
        val ctx = requireContext()
        val powerManager = ctx.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(ctx.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${ctx.packageName}")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun updateBannerVisibility() {
        val ctx = requireContext()
        val powerManager = ctx.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        val ignoring = powerManager.isIgnoringBatteryOptimizations(ctx.packageName)
        binding.batteryBanner.visibility = if (ignoring) View.GONE else View.VISIBLE
        binding.pausedBanner.visibility = if (prefs.trackingEnabled) View.GONE else View.VISIBLE
    }

    private fun refreshUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = repo.getFullStats()
            if (_binding == null) return@launch

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
        if (_binding == null) return
        val entries = data.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val dataSet = BarDataSet(entries, "").apply {
            color = resources.getColor(R.color.brand_primary, requireContext().theme)
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
                textColor = resources.getColor(R.color.text_secondary, requireContext().theme)
            }
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                textColor = resources.getColor(R.color.text_secondary, requireContext().theme)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            setTouchEnabled(false)
            animateY(500)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
