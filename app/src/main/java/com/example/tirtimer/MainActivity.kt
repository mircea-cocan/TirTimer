package com.example.tirtimer

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import com.example.tirtimer.databinding.ActivityMainBinding
import com.example.tirtimer.model.TimerConfiguration
import com.example.tirtimer.model.TimerStage
import com.example.tirtimer.model.TimerState
import com.example.tirtimer.UnifiedSettingsActivity
import com.example.tirtimer.timer.TimerManager
import com.example.tirtimer.utils.PresetManager
import com.example.tirtimer.utils.TimerPreferences

/**
 * Main activity that displays the timer interface
 * Manages the timer display, controls, and stage transitions
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var timerManager: TimerManager
    private lateinit var preferences: TimerPreferences
    private lateinit var presetManager: PresetManager
    private var textToSpeech: TextToSpeech? = null
    private var previousStage: TimerStage? = null
    
    companion object {
        private const val STAGE_ONE_COLOR = "#FF8A80" // Light Red (Preparation)
        private const val STAGE_TWO_COLOR = "#81C784" // Light Green (Shooting)
        private const val COMPLETED_COLOR = "#9C27B0" // Purple
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = TimerPreferences(this)
        presetManager = PresetManager(this)
        initializeTimer()
        setupClickListeners()
        updateTimerDisplay(timerManager.getCurrentState())
        
        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)
    }
    
    /**
     * Initializes the timer manager with saved configuration or current preset
     */
    private fun initializeTimer() {
        val configuration = getCurrentConfiguration()
        timerManager = TimerManager(
            configuration = configuration,
            onTimerUpdate = { state -> updateTimerDisplay(state) },
            onTimerComplete = { onTimerCompleted() }
        )
    }
    
    /**
     * Gets the current timer configuration from preset or settings
     */
    private fun getCurrentConfiguration(): TimerConfiguration {
        val currentPreset = presetManager.getCurrentPreset()
        if (currentPreset != null) {
            return currentPreset.configuration
        }
        
        // If no preset is selected, select the first default preset
        val allPresets = presetManager.getAllPresets()
        if (allPresets.isNotEmpty()) {
            presetManager.setCurrentPreset(allPresets[0])
            return allPresets[0].configuration
        }
        
        // Fallback to preferences if no presets exist (shouldn't happen)
        return preferences.getTimerConfiguration()
    }
    
    /**
     * Sets up click listeners for all buttons
     */
    private fun setupClickListeners() {
        binding.btnStartPause.setOnClickListener {
            handleStartPauseClick()
        }
        
        binding.btnStop.setOnClickListener {
            timerManager.stopTimer()
        }
        
        binding.btnSettings.setOnClickListener {
            openSettings()
        }
    }
    
    /**
     * Handles the start/pause button click based on current timer state
     */
    private fun handleStartPauseClick() {
        val currentState = timerManager.getCurrentState()
        when {
            currentState.currentStage == TimerStage.COMPLETED -> {
                // Reset to beginning
                timerManager.stopTimer()
            }
            !currentState.isRunning && currentState.currentStage == TimerStage.STAGE_ONE && 
            currentState.remainingTimeSeconds == currentState.configuration.stageOneDurationSeconds -> {
                // Start from beginning - announce preparation phase
                timerManager.startTimer()
                speakText("PREPARATION!")
            }
            currentState.isRunning -> {
                // Pause
                timerManager.pauseTimer()
            }
            else -> {
                // Resume
                timerManager.resumeTimer()
                // Don't announce when resuming - only announce on fresh start or stage transitions
            }
        }
    }
    
    /**
     * Updates the timer display based on the current state
     * @param state The current timer state
     */
    private fun updateTimerDisplay(state: TimerState) {
        runOnUiThread {
            // Check for stage transition and announce if needed
            if (previousStage != null && previousStage != state.currentStage) {
                when (state.currentStage) {
                    TimerStage.STAGE_TWO -> {
                        // Transition from preparation to shooting
                        speakText("SHOOT!")
                    }
                    TimerStage.COMPLETED -> {
                        // Transition from shooting to completed - end of shooting phase
                        speakText("STOP!")
                    }
                    else -> {
                        // Other transitions don't need announcements
                    }
                }
            }
            previousStage = state.currentStage
            
            // Update time display
            binding.tvTimerDisplay.text = state.getFormattedTime()
            
            // Update stage indicator and colors
            when (state.currentStage) {
                TimerStage.STAGE_ONE -> {
                    binding.tvStageIndicator.text = "Preparation"
                    // Only show preparation gradient if timer is running or has been started
                    if (state.isRunning || state.remainingTimeSeconds < state.configuration.stageOneDurationSeconds) {
                        updateUIColors(STAGE_ONE_COLOR)
                    } else {
                        updateUIColors(COMPLETED_COLOR) // Show purple when not started
                    }
                }
                TimerStage.STAGE_TWO -> {
                    binding.tvStageIndicator.text = "Shooting"
                    updateUIColors(STAGE_TWO_COLOR)
                }
                TimerStage.COMPLETED -> {
                    binding.tvStageIndicator.text = "Completed!"
                    updateUIColors(COMPLETED_COLOR)
                }
            }
            
            // Update progress bar
            val progress = (state.getCurrentStageProgress() * 100).toInt()
            binding.progressBar.progress = progress
            
            // Update start/pause button text
            binding.btnStartPause.text = when {
                state.currentStage == TimerStage.COMPLETED -> "Reset"
                state.isRunning -> "Pause"
                else -> if (state.remainingTimeSeconds == state.configuration.stageOneDurationSeconds && 
                           state.currentStage == TimerStage.STAGE_ONE) "Start" else "Resume"
            }
            
            // Update stage info display
            updateStageInfoDisplay(state.configuration)
        }
    }
    
    /**
     * Updates UI colors based on current stage
     * @param colorHex The color to apply as hex string
     */
    private fun updateUIColors(colorHex: String) {
        // Update background gradient and progress bar color based on stage
        val (backgroundDrawable, progressColor) = when (colorHex) {
            STAGE_ONE_COLOR -> Pair(
                ContextCompat.getDrawable(this, R.drawable.gradient_preparation),
                ContextCompat.getColor(this, R.color.stage_preparation)
            )
            STAGE_TWO_COLOR -> Pair(
                ContextCompat.getDrawable(this, R.drawable.gradient_shooting),
                ContextCompat.getColor(this, R.color.stage_shooting)
            )
            else -> Pair(
                ContextCompat.getDrawable(this, R.drawable.gradient_background),
                ContextCompat.getColor(this, R.color.timer_primary)
            )
        }
        
        // Set the background gradient
        binding.main.background = backgroundDrawable
        
        // Set the progress bar color
        binding.progressBar.progressTintList = ColorStateList.valueOf(progressColor)
    }
    
    /**
     * Updates the stage information display
     * @param configuration The current timer configuration
     */
    private fun updateStageInfoDisplay(configuration: TimerConfiguration) {
        val stage1Minutes = configuration.stageOneDurationSeconds / 60
        val stage1Seconds = configuration.stageOneDurationSeconds % 60
        val stage2Minutes = configuration.stageTwoDurationSeconds / 60
        val stage2Seconds = configuration.stageTwoDurationSeconds % 60
        
        binding.tvStageOneInfo.text = String.format("Preparation: %d:%02d", stage1Minutes, stage1Seconds)
        binding.tvStageTwoInfo.text = String.format("Shooting: %d:%02d", stage2Minutes, stage2Seconds)
    }
    
    /**
     * Called when the timer sequence completes
     */
    private fun onTimerCompleted() {
        // Could add sound/vibration notifications here
        runOnUiThread {
            binding.btnStartPause.text = "Reset"
        }
    }
    
    /**
     * Opens the settings activity
     */
    private fun openSettings() {
        val intent = Intent(this, UnifiedSettingsActivity::class.java)
        startActivity(intent)
    }
    
    
    /**
     * TextToSpeech initialization callback
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Language not supported, but TTS will still work with default
                }
            }
        }
    }
    
    /**
     * Speaks the given text using TextToSpeech
     * @param text The text to speak
     */
    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    override fun onResume() {
        super.onResume()
        // Reload configuration in case preset was changed
        val newConfiguration = getCurrentConfiguration()
        if (!timerManager.getCurrentState().isRunning) {
            timerManager.updateConfiguration(newConfiguration)
            updateTimerDisplay(timerManager.getCurrentState())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager.cleanup()
        textToSpeech?.shutdown()
    }
}