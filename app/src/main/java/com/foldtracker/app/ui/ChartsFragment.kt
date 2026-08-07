package com.foldtracker.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.foldtracker.app.R
import com.foldtracker.app.data.DateUtils
import com.foldtracker.app.data.StatsRepository
import com.foldtracker.app.databinding.FragmentChartsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: StatsRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StatsRepository(requireContext())
        loadCharts()
    }

    override fun onResume() {
        super.onResume()
        loadCharts()
    }

    private fun loadCharts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val daily = repo.getDailyCounts(30)
            val weekly = repo.getWeeklyCounts(8)
            if (_binding == null) return@launch

            renderBarChart(
                chart = binding.dailyChart,
                values = daily.map { it.count.toFloat() },
                labels = daily.map { DateUtils.dateLabel(it.dayKey) },
                everyNthLabel = 5
            )
            renderBarChart(
                chart = binding.weeklyChart,
                values = weekly.map { it.count.toFloat() },
                labels = weekly.map { DateUtils.weekLabel(it.weekStartKey) },
                everyNthLabel = 1
            )
        }
    }

    private fun renderBarChart(chart: BarChart, values: List<Float>, labels: List<String>, everyNthLabel: Int) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "").apply {
            color = resources.getColor(R.color.brand_primary, requireContext().theme)
            setDrawValues(false)
        }
        chart.apply {
            data = BarData(dataSet).apply { barWidth = 0.7f }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = everyNthLabel.toFloat()
                setDrawGridLines(false)
                textColor = resources.getColor(R.color.text_secondary, requireContext().theme)
                labelRotationAngle = if (everyNthLabel == 1) -45f else -30f
            }
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(true)
                textColor = resources.getColor(R.color.text_secondary, requireContext().theme)
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toInt().toString()
                }
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
