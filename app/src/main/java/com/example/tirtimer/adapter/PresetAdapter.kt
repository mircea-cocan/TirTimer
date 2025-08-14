package com.example.tirtimer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tirtimer.R
import com.example.tirtimer.model.TimerPreset
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PresetAdapter(
    private val onPresetAction: (TimerPreset, Action) -> Unit
) : RecyclerView.Adapter<PresetAdapter.PresetViewHolder>() {
    
    enum class Action {
        SELECT, EDIT, DELETE
    }
    
    private var presets: List<TimerPreset> = emptyList()
    private var currentPresetId: String? = null
    
    fun updatePresets(newPresets: List<TimerPreset>, selectedPresetId: String?) {
        presets = newPresets
        currentPresetId = selectedPresetId
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preset, parent, false)
        return PresetViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(presets[position])
    }
    
    override fun getItemCount(): Int = presets.size
    
    inner class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardPreset) ?: 
            itemView as MaterialCardView
        private val tvPresetName: TextView = itemView.findViewById(R.id.tvPresetName)
        private val tvPresetTiming: TextView = itemView.findViewById(R.id.tvPresetTiming)
        private val tvPresetType: TextView = itemView.findViewById(R.id.tvPresetType)
        private val btnSelectPreset: MaterialButton = itemView.findViewById(R.id.btnSelectPreset)
        private val btnEditPreset: MaterialButton = itemView.findViewById(R.id.btnEditPreset)
        private val btnDeletePreset: MaterialButton = itemView.findViewById(R.id.btnDeletePreset)
        
        fun bind(preset: TimerPreset) {
            tvPresetName.text = preset.name
            tvPresetTiming.text = preset.getTimingDescription()
            
            // Show/hide type indicator for different preset types
            val defaultPresets = listOf("default_competition", "default_practice", "default_quick")
            when {
                preset.isDefault -> {
                    tvPresetType.visibility = View.VISIBLE
                    tvPresetType.text = "Default"
                    tvPresetType.setTextColor(itemView.context.getColor(R.color.stage_shooting))
                }
                preset.id in defaultPresets -> {
                    tvPresetType.visibility = View.VISIBLE
                    tvPresetType.text = "Modified"
                    tvPresetType.setTextColor(itemView.context.getColor(R.color.stage_preparation))
                }
                else -> {
                    tvPresetType.visibility = View.GONE
                }
            }
            
            // Highlight if this is the current preset
            val isSelected = preset.id == currentPresetId
            if (isSelected) {
                cardView.strokeWidth = 4
                cardView.strokeColor = itemView.context.getColor(R.color.timer_primary)
                btnSelectPreset.text = "Selected"
                btnSelectPreset.isEnabled = false
            } else {
                cardView.strokeWidth = 0
                btnSelectPreset.text = "Select"
                btnSelectPreset.isEnabled = true
            }
            
            // Setup click listeners
            btnSelectPreset.setOnClickListener {
                onPresetAction(preset, Action.SELECT)
            }
            
            btnEditPreset.setOnClickListener {
                onPresetAction(preset, Action.EDIT)
            }
            
            btnDeletePreset.setOnClickListener {
                onPresetAction(preset, Action.DELETE)
            }
            
            // Enable edit/delete for all presets
            btnEditPreset.isEnabled = true
            btnEditPreset.alpha = 1.0f
            btnDeletePreset.isEnabled = true
            btnDeletePreset.alpha = 1.0f
        }
    }
}