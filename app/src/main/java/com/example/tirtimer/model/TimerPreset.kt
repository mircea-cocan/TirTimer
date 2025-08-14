package com.example.tirtimer.model

import java.util.UUID

/**
 * Represents a saved timer preset with custom name and configuration
 * 
 * @property id Unique identifier for the preset
 * @property name Custom name for the preset (e.g., "Competition", "Practice", etc.)
 * @property configuration The timer configuration for this preset
 * @property isDefault Whether this is a built-in default preset
 */
data class TimerPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val configuration: TimerConfiguration,
    val isDefault: Boolean = false
) {
    
    /**
     * Creates a formatted description of the preset timing
     * @return String like "Prep: 5:00, Shoot: 3:00"
     */
    fun getTimingDescription(): String {
        val prepMinutes = configuration.stageOneDurationSeconds / 60
        val prepSeconds = configuration.stageOneDurationSeconds % 60
        val shootMinutes = configuration.stageTwoDurationSeconds / 60
        val shootSeconds = configuration.stageTwoDurationSeconds % 60
        
        return "Prep: %d:%02d, Shoot: %d:%02d".format(
            prepMinutes, prepSeconds, shootMinutes, shootSeconds
        )
    }
    
    /**
     * Validates that the preset has valid data
     * @return true if preset is valid
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && configuration.isValid()
    }
    
    companion object {
        /**
         * Creates default presets that come with the app
         */
        fun getDefaultPresets(): List<TimerPreset> {
            return listOf(
                TimerPreset(
                    id = "default_competition",
                    name = "Competition",
                    configuration = TimerConfiguration(
                        stageOneDurationSeconds = 300L, // 5:00
                        stageTwoDurationSeconds = 180L  // 3:00
                    ),
                    isDefault = true
                ),
                TimerPreset(
                    id = "default_practice",
                    name = "Practice",
                    configuration = TimerConfiguration(
                        stageOneDurationSeconds = 180L, // 3:00
                        stageTwoDurationSeconds = 120L  // 2:00
                    ),
                    isDefault = true
                ),
                TimerPreset(
                    id = "default_quick",
                    name = "Quick Session",
                    configuration = TimerConfiguration(
                        stageOneDurationSeconds = 60L,  // 1:00
                        stageTwoDurationSeconds = 30L   // 0:30
                    ),
                    isDefault = true
                )
            )
        }
    }
}