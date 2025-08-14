package com.example.tirtimer.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.tirtimer.model.TimerConfiguration
import com.example.tirtimer.model.TimerPreset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages timer presets including saving, loading, and organizing presets
 * Handles both default presets and user-created custom presets
 * 
 * @property context Application context for SharedPreferences access
 */
class PresetManager(private val context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "timer_presets"
        private const val KEY_CUSTOM_PRESETS = "custom_presets"
        private const val KEY_CURRENT_PRESET_ID = "current_preset_id"
    }
    
    /**
     * Gets all presets (default + custom, with custom overrides replacing defaults)
     * @return List of all available presets
     */
    fun getAllPresets(): List<TimerPreset> {
        val deletedDefaults = getDeletedDefaultPresets()
        val defaultPresets = TimerPreset.getDefaultPresets().filterNot { it.id in deletedDefaults }
        val customPresets = getCustomPresets()
        
        // Get IDs of custom presets that override defaults
        val customPresetIds = customPresets.map { it.id }.toSet()
        
        // Filter out default presets that have custom overrides
        val availableDefaultPresets = defaultPresets.filterNot { it.id in customPresetIds }
        
        return availableDefaultPresets + customPresets
    }
    
    /**
     * Gets only custom user-created presets
     * @return List of custom presets
     */
    fun getCustomPresets(): List<TimerPreset> {
        val presetsJson = preferences.getString(KEY_CUSTOM_PRESETS, "[]")
        return parsePresetsFromJson(presetsJson ?: "[]")
    }
    
    /**
     * Saves a preset (custom or modified default)
     * @param preset The preset to save
     * @param isEditing Whether this is editing an existing preset
     * @return true if saved successfully
     */
    fun savePreset(preset: TimerPreset, isEditing: Boolean = false): Boolean {
        return try {
            if (!preset.isValid()) return false
            
            val customPresets = getCustomPresets().toMutableList()
            
            // If this is a default preset being edited, we need special handling
            val defaultPresets = TimerPreset.getDefaultPresets()
            val isDefaultPreset = defaultPresets.any { it.id == preset.id }
            
            val presetToSave = if (isDefaultPreset && isEditing) {
                // When editing a default preset, keep the same ID but mark as modified
                // This effectively "overrides" the default preset
                preset.copy(isDefault = false)
            } else {
                preset
            }
            
            // Check if preset with same ID exists and update it
            val existingIndex = customPresets.indexOfFirst { it.id == presetToSave.id }
            if (existingIndex >= 0) {
                customPresets[existingIndex] = presetToSave
            } else {
                customPresets.add(presetToSave)
            }
            
            val jsonArray = JSONArray()
            customPresets.forEach { jsonArray.put(presetToJson(it)) }
            
            preferences.edit()
                .putString(KEY_CUSTOM_PRESETS, jsonArray.toString())
                .apply()
            
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Deletes a preset (custom or default)
     * @param presetId The ID of the preset to delete
     * @return true if deleted successfully
     */
    fun deletePreset(presetId: String): Boolean {
        return try {
            // Check if it's a default preset
            val defaultPresets = TimerPreset.getDefaultPresets()
            val defaultPreset = defaultPresets.find { it.id == presetId }
            
            if (defaultPreset != null) {
                // Store deleted default preset IDs
                val deletedDefaults = getDeletedDefaultPresets().toMutableSet()
                deletedDefaults.add(presetId)
                saveDeletedDefaultPresets(deletedDefaults)
            } else {
                // Handle custom preset deletion
                val customPresets = getCustomPresets().toMutableList()
                val removed = customPresets.removeIf { it.id == presetId }
                
                if (removed) {
                    val jsonArray = JSONArray()
                    customPresets.forEach { jsonArray.put(presetToJson(it)) }
                    
                    preferences.edit()
                        .putString(KEY_CUSTOM_PRESETS, jsonArray.toString())
                        .apply()
                }
            }
            
            // If we deleted the current preset, clear the current selection
            if (getCurrentPresetId() == presetId) {
                setCurrentPreset(null)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the set of deleted default preset IDs
     */
    private fun getDeletedDefaultPresets(): Set<String> {
        val deletedJson = preferences.getString("deleted_default_presets", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(deletedJson)
            val deletedSet = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                deletedSet.add(jsonArray.getString(i))
            }
            deletedSet
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Saves the set of deleted default preset IDs
     */
    private fun saveDeletedDefaultPresets(deletedIds: Set<String>) {
        val jsonArray = JSONArray()
        deletedIds.forEach { jsonArray.put(it) }
        preferences.edit()
            .putString("deleted_default_presets", jsonArray.toString())
            .apply()
    }
    
    /**
     * Gets a preset by ID
     * @param presetId The preset ID to find
     * @return The preset if found, null otherwise
     */
    fun getPresetById(presetId: String): TimerPreset? {
        return getAllPresets().find { it.id == presetId }
    }
    
    /**
     * Sets the currently active preset
     * @param preset The preset to set as current (null to clear)
     */
    fun setCurrentPreset(preset: TimerPreset?) {
        preferences.edit()
            .putString(KEY_CURRENT_PRESET_ID, preset?.id)
            .apply()
    }
    
    /**
     * Gets the ID of the currently active preset
     * @return Current preset ID or null if none selected
     */
    fun getCurrentPresetId(): String? {
        return preferences.getString(KEY_CURRENT_PRESET_ID, null)
    }
    
    /**
     * Gets the currently active preset
     * @return Current preset or null if none selected
     */
    fun getCurrentPreset(): TimerPreset? {
        val currentId = getCurrentPresetId()
        return if (currentId != null) getPresetById(currentId) else null
    }
    
    /**
     * Creates a new preset from the current timer configuration
     * @param name Name for the new preset
     * @param configuration Current timer configuration
     * @return The created preset
     */
    fun createPresetFromConfiguration(name: String, configuration: TimerConfiguration): TimerPreset {
        return TimerPreset(
            name = name,
            configuration = configuration,
            isDefault = false
        )
    }
    
    /**
     * Checks if a preset name already exists
     * @param name The name to check
     * @param excludeId Preset ID to exclude from check (for editing)
     * @return true if name exists
     */
    fun isPresetNameExists(name: String, excludeId: String? = null): Boolean {
        return getAllPresets().any { it.name.equals(name, ignoreCase = true) && it.id != excludeId }
    }
    
    // Private helper methods
    
    private fun parsePresetsFromJson(jsonString: String): List<TimerPreset> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val presets = mutableListOf<TimerPreset>()
            
            for (i in 0 until jsonArray.length()) {
                val presetJson = jsonArray.getJSONObject(i)
                val preset = presetFromJson(presetJson)
                if (preset.isValid()) {
                    presets.add(preset)
                }
            }
            
            presets
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun presetToJson(preset: TimerPreset): JSONObject {
        return JSONObject().apply {
            put("id", preset.id)
            put("name", preset.name)
            put("stageOneDuration", preset.configuration.stageOneDurationSeconds)
            put("stageTwoDuration", preset.configuration.stageTwoDurationSeconds)
            put("isDefault", preset.isDefault)
        }
    }
    
    private fun presetFromJson(json: JSONObject): TimerPreset {
        return TimerPreset(
            id = json.getString("id"),
            name = json.getString("name"),
            configuration = TimerConfiguration(
                stageOneDurationSeconds = json.getLong("stageOneDuration"),
                stageTwoDurationSeconds = json.getLong("stageTwoDuration")
            ),
            isDefault = json.optBoolean("isDefault", false)
        )
    }
}