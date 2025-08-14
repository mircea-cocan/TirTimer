package com.example.tirtimer.model

/**
 * Represents the current state of the timer
 * 
 * @property currentStage The current stage the timer is in
 * @property remainingTimeSeconds Time remaining in the current stage
 * @property isRunning Whether the timer is currently running
 * @property configuration The timer configuration being used
 */
data class TimerState(
    val currentStage: TimerStage = TimerStage.STAGE_ONE,
    val remainingTimeSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val configuration: TimerConfiguration = TimerConfiguration()
) {
    
    /**
     * Formats the remaining time as MM:SS
     * @return Formatted time string
     */
    fun getFormattedTime(): String {
        val minutes = remainingTimeSeconds / 60
        val seconds = remainingTimeSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Gets the progress percentage for the current stage (0.0 to 1.0)
     * @return Progress as a float between 0 and 1
     */
    fun getCurrentStageProgress(): Float {
        val totalStageTime = when (currentStage) {
            TimerStage.STAGE_ONE -> configuration.stageOneDurationSeconds
            TimerStage.STAGE_TWO -> configuration.stageTwoDurationSeconds
            TimerStage.COMPLETED -> 1L
        }
        
        if (totalStageTime == 0L) return 1f
        return 1f - (remainingTimeSeconds.toFloat() / totalStageTime.toFloat())
    }
}