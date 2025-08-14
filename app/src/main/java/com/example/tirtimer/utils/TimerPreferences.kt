package com.example.tirtimer.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.tirtimer.model.TimerConfiguration

/**
 * Utility class for managing timer configuration preferences
 * Handles saving and loading of timer settings using SharedPreferences
 * 
 * @property context Application context for accessing SharedPreferences
 */
class TimerPreferences(private val context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "timer_preferences"
        private const val KEY_STAGE_ONE_DURATION = "stage_one_duration_seconds"
        private const val KEY_STAGE_TWO_DURATION = "stage_two_duration_seconds"
        
        // Default values
        private const val DEFAULT_STAGE_ONE_DURATION = 300L // 5 minutes
        private const val DEFAULT_STAGE_TWO_DURATION = 180L // 3 minutes
    }
    
    /**
     * Saves the timer configuration to preferences
     * @param configuration The timer configuration to save
     */
    fun saveTimerConfiguration(configuration: TimerConfiguration) {
        preferences.edit()
            .putLong(KEY_STAGE_ONE_DURATION, configuration.stageOneDurationSeconds)
            .putLong(KEY_STAGE_TWO_DURATION, configuration.stageTwoDurationSeconds)
            .apply()
    }
    
    /**
     * Loads the timer configuration from preferences
     * @return The saved timer configuration, or default values if none exists
     */
    fun getTimerConfiguration(): TimerConfiguration {
        val stageOneDuration = preferences.getLong(KEY_STAGE_ONE_DURATION, DEFAULT_STAGE_ONE_DURATION)
        val stageTwoDuration = preferences.getLong(KEY_STAGE_TWO_DURATION, DEFAULT_STAGE_TWO_DURATION)
        
        return TimerConfiguration(
            stageOneDurationSeconds = stageOneDuration,
            stageTwoDurationSeconds = stageTwoDuration
        )
    }
    
    /**
     * Resets the timer configuration to default values
     */
    fun resetToDefaults() {
        val defaultConfig = TimerConfiguration(
            stageOneDurationSeconds = DEFAULT_STAGE_ONE_DURATION,
            stageTwoDurationSeconds = DEFAULT_STAGE_TWO_DURATION
        )
        saveTimerConfiguration(defaultConfig)
    }
    
    /**
     * Checks if timer configuration has been customized from defaults
     * @return true if configuration differs from defaults, false otherwise
     */
    fun hasCustomConfiguration(): Boolean {
        val current = getTimerConfiguration()
        return current.stageOneDurationSeconds != DEFAULT_STAGE_ONE_DURATION ||
               current.stageTwoDurationSeconds != DEFAULT_STAGE_TWO_DURATION
    }
}