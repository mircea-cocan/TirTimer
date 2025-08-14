package com.example.tirtimer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tirtimer.adapter.PresetAdapter
import com.example.tirtimer.model.TimerConfiguration
import com.example.tirtimer.model.TimerPreset
import com.example.tirtimer.utils.PresetManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PresetActivity : AppCompatActivity() {
    
    private lateinit var recyclerViewPresets: RecyclerView
    private lateinit var btnAddPreset: MaterialButton
    private lateinit var btnBack: MaterialButton
    
    private lateinit var presetManager: PresetManager
    private lateinit var presetAdapter: PresetAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presets)
        
        initViews()
        initPresetManager()
        setupRecyclerView()
        setupClickListeners()
        loadPresets()
    }
    
    private fun initViews() {
        recyclerViewPresets = findViewById(R.id.recyclerViewPresets)
        btnAddPreset = findViewById(R.id.btnAddPreset)
        btnBack = findViewById(R.id.btnBack)
    }
    
    private fun initPresetManager() {
        presetManager = PresetManager(this)
    }
    
    private fun setupRecyclerView() {
        presetAdapter = PresetAdapter { preset, action ->
            when (action) {
                PresetAdapter.Action.SELECT -> selectPreset(preset)
                PresetAdapter.Action.EDIT -> editPreset(preset)
                PresetAdapter.Action.DELETE -> deletePreset(preset)
            }
        }
        
        recyclerViewPresets.apply {
            layoutManager = LinearLayoutManager(this@PresetActivity)
            adapter = presetAdapter
        }
    }
    
    private fun setupClickListeners() {
        btnAddPreset.setOnClickListener {
            showPresetDialog(null)
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadPresets() {
        val presets = presetManager.getAllPresets()
        val currentPresetId = presetManager.getCurrentPresetId()
        presetAdapter.updatePresets(presets, currentPresetId)
    }
    
    private fun selectPreset(preset: TimerPreset) {
        presetManager.setCurrentPreset(preset)
        Toast.makeText(this, "Selected: ${preset.name}", Toast.LENGTH_SHORT).show()
        loadPresets() // Refresh to show selection
    }
    
    private fun editPreset(preset: TimerPreset) {
        if (preset.isDefault) {
            Toast.makeText(this, "Cannot edit default presets", Toast.LENGTH_SHORT).show()
            return
        }
        showPresetDialog(preset)
    }
    
    private fun deletePreset(preset: TimerPreset) {
        if (preset.isDefault) {
            Toast.makeText(this, "Cannot delete default presets", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Delete Preset")
            .setMessage("Are you sure you want to delete '${preset.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                if (presetManager.deletePreset(preset.id)) {
                    Toast.makeText(this, "Preset deleted", Toast.LENGTH_SHORT).show()
                    loadPresets()
                } else {
                    Toast.makeText(this, "Failed to delete preset", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPresetDialog(preset: TimerPreset?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_preset, null)
        
        val etPresetName = dialogView.findViewById<TextInputEditText>(R.id.etPresetName)
        val etPrepMinutes = dialogView.findViewById<TextInputEditText>(R.id.etPrepMinutes)
        val etPrepSeconds = dialogView.findViewById<TextInputEditText>(R.id.etPrepSeconds)
        val etShootMinutes = dialogView.findViewById<TextInputEditText>(R.id.etShootMinutes)
        val etShootSeconds = dialogView.findViewById<TextInputEditText>(R.id.etShootSeconds)
        val btnDialogCancel = dialogView.findViewById<MaterialButton>(R.id.btnDialogCancel)
        val btnDialogSave = dialogView.findViewById<MaterialButton>(R.id.btnDialogSave)
        
        // Pre-fill if editing
        preset?.let { p ->
            etPresetName.setText(p.name)
            etPrepMinutes.setText((p.configuration.stageOneDurationSeconds / 60).toString())
            etPrepSeconds.setText((p.configuration.stageOneDurationSeconds % 60).toString())
            etShootMinutes.setText((p.configuration.stageTwoDurationSeconds / 60).toString())
            etShootSeconds.setText((p.configuration.stageTwoDurationSeconds % 60).toString())
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnDialogSave.setOnClickListener {
            val name = etPresetName.text?.toString()?.trim() ?: ""
            val prepMinutesStr = etPrepMinutes.text?.toString() ?: "0"
            val prepSecondsStr = etPrepSeconds.text?.toString() ?: "0"
            val shootMinutesStr = etShootMinutes.text?.toString() ?: "0"
            val shootSecondsStr = etShootSeconds.text?.toString() ?: "0"
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a preset name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (presetManager.isPresetNameExists(name, preset?.id)) {
                Toast.makeText(this, "A preset with this name already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                val prepMinutes = prepMinutesStr.toLongOrNull() ?: 0L
                val prepSeconds = prepSecondsStr.toLongOrNull() ?: 0L
                val shootMinutes = shootMinutesStr.toLongOrNull() ?: 0L
                val shootSeconds = shootSecondsStr.toLongOrNull() ?: 0L
                
                if (prepSeconds >= 60 || shootSeconds >= 60) {
                    Toast.makeText(this, "Seconds must be less than 60", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val prepTotalSeconds = prepMinutes * 60 + prepSeconds
                val shootTotalSeconds = shootMinutes * 60 + shootSeconds
                
                if (prepTotalSeconds <= 0 || shootTotalSeconds <= 0) {
                    Toast.makeText(this, "Durations must be greater than 0", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val configuration = TimerConfiguration(
                    stageOneDurationSeconds = prepTotalSeconds,
                    stageTwoDurationSeconds = shootTotalSeconds
                )
                
                val newPreset = if (preset != null) {
                    // Editing existing preset
                    preset.copy(name = name, configuration = configuration)
                } else {
                    // Creating new preset
                    TimerPreset(
                        name = name,
                        configuration = configuration,
                        isDefault = false
                    )
                }
                
                if (presetManager.savePreset(newPreset)) {
                    Toast.makeText(this, if (preset != null) "Preset updated" else "Preset saved", Toast.LENGTH_SHORT).show()
                    loadPresets()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Failed to save preset", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid time values", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
}