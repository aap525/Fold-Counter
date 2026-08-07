package com.foldtracker.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foldtracker.app.data.Prefs
import com.foldtracker.app.data.StatsRepository
import com.foldtracker.app.databinding.ActivitySettingsBinding
import com.foldtracker.app.service.DailySummaryReceiver
import com.foldtracker.app.service.FoldDetectionService
import com.foldtracker.app.widget.WidgetUpdater
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs
    private lateinit var repo: StatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        repo = StatsRepository(this)

        binding.backButton.setOnClickListener { finish() }

        setupSwitches()
        setupBatteryRow()
        setupResetRow()
    }

    override fun onResume() {
        super.onResume()
        updateBatteryStatusText()
    }

    private fun setupSwitches() {
        binding.trackingSwitch.isChecked = prefs.trackingEnabled
        binding.trackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.trackingEnabled = isChecked
            if (isChecked) {
                FoldDetectionService.start(this)
            } else {
                FoldDetectionService.stop(this)
            }
        }

        binding.streaksSwitch.isChecked = prefs.streaksEnabled
        binding.streaksSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.streaksEnabled = isChecked
        }

        binding.goalSwitch.isChecked = prefs.dailyGoalEnabled
        binding.goalSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.dailyGoalEnabled = isChecked
        }

        setupGoalTargetInput()

        binding.notificationsSwitch.isChecked = prefs.notificationsEnabled
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.notificationsEnabled = isChecked
            if (isChecked) {
                DailySummaryReceiver.scheduleNext(this)
            } else {
                DailySummaryReceiver.cancel(this)
            }
        }
    }

    private fun setupGoalTargetInput() {
        binding.goalTargetInput.setText(prefs.dailyGoal.toString())
        binding.goalTargetInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveGoalTarget()
        }
        binding.goalTargetInput.setOnEditorActionListener { _, _, _ ->
            saveGoalTarget()
            binding.goalTargetInput.clearFocus()
            true
        }
    }

    private fun saveGoalTarget() {
        val entered = binding.goalTargetInput.text.toString().toIntOrNull()
        val validated = (entered ?: prefs.dailyGoal).coerceIn(1, 999)
        prefs.dailyGoal = validated
        binding.goalTargetInput.setText(validated.toString())
    }

    private fun setupBatteryRow() {
        binding.batteryRow.setOnClickListener {
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
        updateBatteryStatusText()
    }

    private fun updateBatteryStatusText() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val ignoring = powerManager.isIgnoringBatteryOptimizations(packageName)
        binding.batteryStatusText.text = if (ignoring) "Enabled" else "Not enabled"
    }

    private fun setupResetRow() {
        binding.resetRow.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(R.string.reset_confirm_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset) { _, _ ->
                    lifecycleScope.launch {
                        repo.resetAllData()
                        WidgetUpdater.updateAllWidgets(this@SettingsActivity)
                    }
                }
                .show()
        }
    }
}
