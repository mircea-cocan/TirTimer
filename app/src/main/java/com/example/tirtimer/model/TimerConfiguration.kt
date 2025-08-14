package com.example.tirtimer.model

/**
 * Configuration class that holds the duration settings for both timer stages
 * 
 * @property stageOneDurationSeconds Duration of the first stage in seconds
 * @property stageTwoDurationSeconds Duration of the second stage in seconds
 */
data class TimerConfiguration(
    val stageOneDurationSeconds: Long = 300L, // Default: 5 minutes
    val stageTwoDurationSeconds: Long = 180L  // Default: 3 minutes
) {
    
    /**
     * Gets the total duration of both stages combined
     * @return Total duration in seconds
     */
    fun getTotalDuration(): Long {
        return stageOneDurationSeconds + stageTwoDurationSeconds
    }
    
    /**
     * Validates that both stage durations are positive
     * @return true if configuration is valid, false otherwise
     */
    fun isValid(): Boolean {
        return stageOneDurationSeconds > 0 && stageTwoDurationSeconds > 0
    }
}