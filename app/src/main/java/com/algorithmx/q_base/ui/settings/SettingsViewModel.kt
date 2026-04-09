package com.algorithmx.q_base.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core_ai.brain.BrainDataStoreManager
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.core_ai.brain.models.BrainCategory
import com.algorithmx.q_base.core_ai.brain.models.BrainProvider
import com.algorithmx.q_base.core_ai.brain.models.StoredBrainConfig
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import com.algorithmx.q_base.core_ai.brain.models.TaskConfig
import com.algorithmx.q_base.core_ai.brain.registry.BrainRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: BrainDataStoreManager,
    private val database: AppDatabase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val brainConfig: StateFlow<StoredBrainConfig> = dataStoreManager.brainConfigFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StoredBrainConfig(BrainProvider.GEMINI, "gemini-2.5-flash", "", 0, 0)
        )

    val dbSizeMb: StateFlow<Double> = dataStoreManager.brainConfigFlow.map {
        calculateDbSize()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val availableModels: List<String> = BrainRegistry.categoryMap.values.flatten().distinct()

    fun updateModel(newModel: String) {
        viewModelScope.launch {
            val provider = BrainRegistry.getProviderForModel(newModel)
            dataStoreManager.saveEngineConfiguration(
                provider = provider,
                modelName = newModel,
                category = BrainCategory.TEXT_TO_TEXT,
                systemInstruction = brainConfig.value.systemInstruction
            )
        }
    }

    fun updateSystemInstruction(instruction: String) {
        viewModelScope.launch {
            dataStoreManager.saveEngineConfiguration(
                provider = brainConfig.value.provider,
                modelName = brainConfig.value.modelName,
                category = brainConfig.value.category,
                systemInstruction = instruction
            )
        }
    }

    fun saveTaskConfig(task: BrainTask, config: TaskConfig) {
        viewModelScope.launch {
            dataStoreManager.saveTaskConfig(task, config)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveNotificationsEnabled(enabled)
        }
    }

    fun updateTheme(mode: String) {
        viewModelScope.launch {
            dataStoreManager.saveThemeMode(mode)
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                database.clearAllTables()
            }
            onComplete()
        }
    }

    private fun calculateDbSize(): Double {
        val dbFile = context.getDatabasePath("Qbase.db")
        return if (dbFile.exists()) {
            dbFile.length().toDouble() / (1024.0 * 1024.0)
        } else {
            0.0
        }
    }
}
