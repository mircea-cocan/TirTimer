package com.example.tirtimer.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tirtimer.databinding.ActivitySettingsBinding
import com.example.tirtimer.model.TimerConfiguration
import com.example.tirtimer.utils.TimerPreferences

/**
 * Settings activity that allows users to configure timer stage durations
 * Provides input validation and saves configuration to preferences
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: TimerPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = TimerPreferences(this)
        loadCurrentSettings()
        setupClickListeners()
    }
    
    /**
     * Loads the current timer configuration and populates the input fields
     */
    private fun loadCurrentSettings() {
        val config = preferences.getTimerConfiguration()
        
        // Stage 1
        val stage1Minutes = config.stageOneDurationSeconds / 60
        val stage1Seconds = config.stageOneDurationSeconds % 60
        binding.etStageOneMinutes.setText(stage1Minutes.toString())
        binding.etStageOneSeconds.setText(stage1Seconds.toString())
        
        // Stage 2
        val stage2Minutes = config.stageTwoDurationSeconds / 60
        val stage2Seconds = config.stageTwoDurationSeconds % 60
        binding.etStageTwoMinutes.setText(stage2Minutes.toString())
        binding.etStageTwoSeconds.setText(stage2Seconds.toString())
    }
    
    /**
     * Sets up click listeners for save and cancel buttons
     */
    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    /**
     * Validates and saves the timer configuration
     */
    private fun saveSettings() {
        try {
            // Parse stage 1 input
            val stage1Minutes = binding.etStageOneMinutes.text.toString().toLongOrNull() ?: 0
            val stage1Seconds = binding.etStageOneSeconds.text.toString().toLongOrNull() ?: 0
            val stage1TotalSeconds = stage1Minutes * 60 + stage1Seconds
            
            // Parse stage 2 input
            val stage2Minutes = binding.etStageTwoMinutes.text.toString().toLongOrNull() ?: 0
            val stage2Seconds = binding.etStageTwoSeconds.text.toString().toLongOrNull() ?: 0
            val stage2TotalSeconds = stage2Minutes * 60 + stage2Seconds
            
            // Validate input
            if (!isValidInput(stage1Minutes, stage1Seconds, stage2Minutes, stage2Seconds)) {
                return
            }
            
            // Create and validate configuration
            val configuration = TimerConfiguration(
                stageOneDurationSeconds = stage1TotalSeconds,
                stageTwoDurationSeconds = stage2TotalSeconds
            )
            
            if (!configuration.isValid()) {
                showError("Both stages must have a duration greater than 0 seconds")
                return
            }
            
            // Save configuration
            preferences.saveTimerConfiguration(configuration)
            
            // Show success message and return result
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
            
        } catch (e: NumberFormatException) {
            showError("Please enter valid numbers for all fields")
        } catch (e: Exception) {
            showError("An error occurred while saving settings: ${e.message}")
        }
    }
    
    /**
     * Validates the input values for both stages
     * @param stage1Minutes Minutes for stage 1
     * @param stage1Seconds Seconds for stage 1
     * @param stage2Minutes Minutes for stage 2
     * @param stage2Seconds Seconds for stage 2
     * @return true if input is valid, false otherwise
     */
    private fun isValidInput(
        stage1Minutes: Long, 
        stage1Seconds: Long, 
        stage2Minutes: Long, 
        stage2Seconds: Long
    ): Boolean {
        
        // Check for negative values
        if (stage1Minutes < 0 || stage1Seconds < 0 || stage2Minutes < 0 || stage2Seconds < 0) {
            showError("Time values cannot be negative")
            return false
        }
        
        // Check seconds are within valid range
        if (stage1Seconds >= 60 || stage2Seconds >= 60) {
            showError("Seconds must be between 0 and 59")
            return false
        }
        
        // Check that at least one stage has some duration
        val stage1Total = stage1Minutes * 60 + stage1Seconds
        val stage2Total = stage2Minutes * 60 + stage2Seconds
        
        if (stage1Total == 0L && stage2Total == 0L) {
            showError("At least one stage must have a duration greater than 0")
            return false
        }
        
        // Check maximum reasonable duration (e.g., 24 hours)
        val maxDurationSeconds = 24 * 60 * 60
        if (stage1Total > maxDurationSeconds || stage2Total > maxDurationSeconds) {
            showError("Maximum duration per stage is 24 hours")
            return false
        }
        
        return true
    }
    
    /**
     * Shows an error message to the user
     * @param message The error message to display
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}