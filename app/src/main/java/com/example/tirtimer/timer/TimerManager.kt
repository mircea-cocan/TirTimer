package com.example.tirtimer.timer

import android.os.CountDownTimer
import com.example.tirtimer.model.TimerConfiguration
import com.example.tirtimer.model.TimerStage
import com.example.tirtimer.model.TimerState

/**
 * Manages the timer logic and state transitions between stages
 * Handles countdown timers for both stages and provides callbacks for UI updates
 * 
 * @property configuration The timer configuration to use
 * @property onTimerUpdate Callback invoked when timer state changes
 * @property onTimerComplete Callback invoked when entire timer sequence completes
 */
class TimerManager(
    private var configuration: TimerConfiguration,
    private val onTimerUpdate: (TimerState) -> Unit,
    private val onTimerComplete: () -> Unit
) {
    
    private var currentTimer: CountDownTimer? = null
    private var currentState = TimerState(
        currentStage = TimerStage.STAGE_ONE,
        remainingTimeSeconds = configuration.stageOneDurationSeconds,
        isRunning = false,
        configuration = configuration
    )
    
    /**
     * Starts the timer from the beginning with stage one
     */
    fun startTimer() {
        stopTimer()
        currentState = TimerState(
            currentStage = TimerStage.STAGE_ONE,
            remainingTimeSeconds = configuration.stageOneDurationSeconds,
            isRunning = true,
            configuration = configuration
        )
        onTimerUpdate(currentState)
        startStageTimer(TimerStage.STAGE_ONE, configuration.stageOneDurationSeconds)
    }
    
    /**
     * Pauses the currently running timer
     */
    fun pauseTimer() {
        currentTimer?.cancel()
        currentState = currentState.copy(isRunning = false)
        onTimerUpdate(currentState)
    }
    
    /**
     * Resumes the paused timer
     */
    fun resumeTimer() {
        if (!currentState.isRunning && currentState.currentStage != TimerStage.COMPLETED) {
            currentState = currentState.copy(isRunning = true)
            onTimerUpdate(currentState)
            startStageTimer(currentState.currentStage, currentState.remainingTimeSeconds)
        }
    }
    
    /**
     * Stops and resets the timer
     */
    fun stopTimer() {
        currentTimer?.cancel()
        currentTimer = null
        currentState = TimerState(
            currentStage = TimerStage.STAGE_ONE,
            remainingTimeSeconds = configuration.stageOneDurationSeconds,
            isRunning = false,
            configuration = configuration
        )
        onTimerUpdate(currentState)
    }
    
    /**
     * Updates the timer configuration and resets the timer
     * @param newConfiguration The new configuration to use
     */
    fun updateConfiguration(newConfiguration: TimerConfiguration) {
        this.configuration = newConfiguration
        stopTimer()
    }
    
    /**
     * Gets the current timer state
     * @return Current timer state
     */
    fun getCurrentState(): TimerState = currentState
    
    /**
     * Starts a countdown timer for the specified stage
     * @param stage The stage to start timer for
     * @param durationSeconds Duration in seconds
     */
    private fun startStageTimer(stage: TimerStage, durationSeconds: Long) {
        currentTimer = object : CountDownTimer(durationSeconds * 1000, 16) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                currentState = currentState.copy(
                    remainingTimeSeconds = secondsRemaining
                )
                onTimerUpdate(currentState)
            }
            
            override fun onFinish() {
                when (stage) {
                    TimerStage.STAGE_ONE -> {
                        // Transition to stage two
                        currentState = currentState.copy(
                            currentStage = TimerStage.STAGE_TWO,
                            remainingTimeSeconds = configuration.stageTwoDurationSeconds
                        )
                        onTimerUpdate(currentState)
                        startStageTimer(TimerStage.STAGE_TWO, configuration.stageTwoDurationSeconds)
                    }
                    TimerStage.STAGE_TWO -> {
                        // Timer completed
                        currentState = currentState.copy(
                            currentStage = TimerStage.COMPLETED,
                            remainingTimeSeconds = 0,
                            isRunning = false
                        )
                        onTimerUpdate(currentState)
                        onTimerComplete()
                    }
                    TimerStage.COMPLETED -> {
                        // Should not happen
                    }
                }
            }
        }
        currentTimer?.start()
    }
    
    /**
     * Cleans up resources when the timer manager is no longer needed
     */
    fun cleanup() {
        currentTimer?.cancel()
        currentTimer = null
    }
}