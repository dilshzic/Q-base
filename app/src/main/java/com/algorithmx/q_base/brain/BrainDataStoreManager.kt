package com.algorithmx.q_base.brain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.algorithmx.q_base.brain.models.BrainCategory
import com.algorithmx.q_base.brain.models.BrainProvider
import com.algorithmx.q_base.brain.models.StoredBrainConfig
import com.algorithmx.q_base.brain.models.BrainTask
import com.algorithmx.q_base.brain.models.TaskConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brain_settings")

@Singleton
class BrainDataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val PROVIDER = stringPreferencesKey("provider")
        val MODEL = stringPreferencesKey("model")
        val CATEGORY = stringPreferencesKey("category")
        val SYSTEM_INSTRUCTION = stringPreferencesKey("system_instruction")
        val TOTAL_REQUESTS = intPreferencesKey("total_requests")
        val TOTAL_TOKENS = intPreferencesKey("total_tokens")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MASTER_AI_FREEZE = booleanPreferencesKey("master_ai_freeze")
        val TASK_CONFIGS_JSON = stringPreferencesKey("task_configs_json")
        val IS_SEED_APPLIED = booleanPreferencesKey("is_seed_applied")
    }

    val brainConfigFlow: Flow<StoredBrainConfig> = context.dataStore.data.map { preferences ->
        val providerName = preferences[PreferencesKeys.PROVIDER] ?: BrainProvider.GEMINI.name
        val categoryName = preferences[PreferencesKeys.CATEGORY] ?: BrainCategory.TEXT_TO_TEXT.name
        
        val taskConfigsJson = preferences[PreferencesKeys.TASK_CONFIGS_JSON]
        val taskConfigsMap = try {
            if (taskConfigsJson.isNullOrEmpty()) emptyMap()
            else json.decodeFromString<Map<BrainTask, TaskConfig>>(taskConfigsJson)
        } catch (e: Exception) {
            emptyMap()
        }
        
        StoredBrainConfig(
            provider = try { BrainProvider.valueOf(providerName) } catch (e: Exception) { BrainProvider.GEMINI },
            modelName = preferences[PreferencesKeys.MODEL] ?: "gemini-3.1-flash-lite-preview",
            systemInstruction = preferences[PreferencesKeys.SYSTEM_INSTRUCTION] ?: "You are a helpful knowledge assistant.",
            totalRequests = preferences[PreferencesKeys.TOTAL_REQUESTS] ?: 0,
            totalTokens = preferences[PreferencesKeys.TOTAL_TOKENS] ?: 0,
            category = try { BrainCategory.valueOf(categoryName) } catch (e: Exception) { BrainCategory.TEXT_TO_TEXT },
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            isMasterAiFreeze = preferences[PreferencesKeys.MASTER_AI_FREEZE] ?: false,
            taskConfigs = taskConfigsMap
        )
    }

    val isSeedAppliedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_SEED_APPLIED] ?: false
    }

    suspend fun markSeedAsApplied() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SEED_APPLIED] = true
        }
    }

    suspend fun saveEngineConfiguration(
        provider: BrainProvider,
        modelName: String,
        category: BrainCategory,
        systemInstruction: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROVIDER] = provider.name
            preferences[PreferencesKeys.MODEL] = modelName
            preferences[PreferencesKeys.CATEGORY] = category.name
            preferences[PreferencesKeys.SYSTEM_INSTRUCTION] = systemInstruction
        }
    }

    suspend fun incrementUsageStats(tokens: Int) {
        context.dataStore.edit { preferences ->
            val currentRequests = preferences[PreferencesKeys.TOTAL_REQUESTS] ?: 0
            val currentTokens = preferences[PreferencesKeys.TOTAL_TOKENS] ?: 0
            preferences[PreferencesKeys.TOTAL_REQUESTS] = currentRequests + 1
            preferences[PreferencesKeys.TOTAL_TOKENS] = currentTokens + tokens
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveTaskConfig(task: BrainTask, config: TaskConfig) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.TASK_CONFIGS_JSON]
            val currentMap = try {
                if (currentJson.isNullOrEmpty()) mutableMapOf<BrainTask, TaskConfig>()
                else json.decodeFromString<MutableMap<BrainTask, TaskConfig>>(currentJson)
            } catch (e: Exception) {
                mutableMapOf<BrainTask, TaskConfig>()
            }
            
            currentMap[task] = config
            preferences[PreferencesKeys.TASK_CONFIGS_JSON] = json.encodeToString<Map<BrainTask, TaskConfig>>(currentMap)
        }
    }

    suspend fun setMasterAiFreeze(freeze: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MASTER_AI_FREEZE] = freeze
        }
    }
}
