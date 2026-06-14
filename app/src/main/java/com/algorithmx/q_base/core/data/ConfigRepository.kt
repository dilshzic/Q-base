package com.algorithmx.q_base.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.core_crypto.CryptoManager
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_config")

@Singleton
class ConfigRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val databases: CoreDatabase,
    private val cryptoManager: CryptoManager
) {
    private val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
    private val GROQ_KEY = stringPreferencesKey("groq_api_key")
    private val DEEPSEEK_KEY = stringPreferencesKey("deepseek_api_key")
    private val LAST_FETCH = stringPreferencesKey("last_fetch_timestamp")

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[GEMINI_KEY]
        if (encrypted.isNullOrBlank()) {
            ""
        } else {
            cryptoManager.decryptLocalString(encrypted).getOrDefault("")
        }
    }
    
    val groqApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[GROQ_KEY]
        if (encrypted.isNullOrBlank()) {
            ""
        } else {
            cryptoManager.decryptLocalString(encrypted).getOrDefault("")
        }
    }

    suspend fun saveGeminiKey(key: String) {
        val encrypted = cryptoManager.encryptLocalString(key)
        context.dataStore.edit { prefs ->
            prefs[GEMINI_KEY] = encrypted
        }
    }

    suspend fun saveGroqKey(key: String) {
        val encrypted = cryptoManager.encryptLocalString(key)
        context.dataStore.edit { prefs ->
            prefs[GROQ_KEY] = encrypted
        }
    }

    suspend fun fetchRemoteConfig() {
        try {
            val lastFetch = context.dataStore.data.map { it[LAST_FETCH]?.toLong() ?: 0L }.first()
            val now = System.currentTimeMillis()
            
            // Only fetch once an hour to save battery/bandwidth
            if (now - lastFetch < 3600000 && lastFetch != 0L) return

            val response = databases.getDocument(
                collectionId = "app_config",
                documentId = "global_keys"
            ).getOrThrow() ?: throw IllegalStateException("Config not found")

            context.dataStore.edit { prefs ->
                val gemini = response["gemini_api_key"] as? String
                val groq = response["groq_api_key"] as? String
                val deepseek = response["deepseek_api_key"] as? String

                if (gemini != null) {
                    // Global Decryption: if the database key is GCM-encrypted, decrypt it with the static binary key first, then encrypt with local AEAD
                    val decrypted = cryptoManager.decryptGlobalKey(gemini).getOrNull() ?: gemini
                    prefs[GEMINI_KEY] = cryptoManager.encryptLocalString(decrypted)
                }
                if (groq != null) {
                    val decrypted = cryptoManager.decryptGlobalKey(groq).getOrNull() ?: groq
                    prefs[GROQ_KEY] = cryptoManager.encryptLocalString(decrypted)
                }
                if (deepseek != null) {
                    val decrypted = cryptoManager.decryptGlobalKey(deepseek).getOrNull() ?: deepseek
                    prefs[DEEPSEEK_KEY] = cryptoManager.encryptLocalString(decrypted)
                }
                
                prefs[LAST_FETCH] = now.toString()
            }
        } catch (e: Exception) {
            android.util.Log.e("ConfigRepository", "Failed to fetch remote config", e)
        }
    }
    
    suspend fun getGeminiKeyDirectly(): String = geminiApiKey.first()
    suspend fun getGroqKeyDirectly(): String = groqApiKey.first()

    suspend fun backupKeysToCloud(userId: String) {
        try {
            val rawGemini = getGeminiKeyDirectly()
            val rawGroq = getGroqKeyDirectly()
            
            // Validate that these are user-entered custom keys
            val isCustomGemini = rawGemini.isNotBlank()
            val isCustomGroq = rawGroq.isNotBlank()
            
            if (!isCustomGemini && !isCustomGroq) return
            
            val userPublicKey = cryptoManager.initializeAndGetPublicKey()
            
            val updateData = mutableMapOf<String, Any>()
            if (isCustomGemini) {
                updateData["geminiKeyEncrypted"] = cryptoManager.encryptMessage(rawGemini, userPublicKey)
            }
            if (isCustomGroq) {
                updateData["groqKeyEncrypted"] = cryptoManager.encryptMessage(rawGroq, userPublicKey)
            }
            
            if (updateData.isNotEmpty()) {
                databases.updateDocument(
                    collectionId = "user_private_settings",
                    documentId = userId,
                    data = updateData
                ).getOrThrow()
                android.util.Log.d("ConfigRepository", "Successfully backed up E2EE API keys to Appwrite Cloud.")
            }
        } catch (e: Exception) {
            android.util.Log.e("ConfigRepository", "Failed to backup E2EE API keys to cloud", e)
        }
    }

    suspend fun restoreKeysFromCloud(userId: String) {
        try {
            val response = databases.getDocument(
                collectionId = "user_private_settings",
                documentId = userId
            ).getOrThrow() ?: throw IllegalStateException("Settings not found")
            
            val encryptedGemini = response["geminiKeyEncrypted"] as? String
            val encryptedGroq = response["groqKeyEncrypted"] as? String
            
            if (!encryptedGemini.isNullOrBlank()) {
                cryptoManager.decryptMessage(encryptedGemini).onSuccess { decrypted ->
                    saveGeminiKey(decrypted)
                    android.util.Log.d("ConfigRepository", "Successfully restored Gemini API key from E2EE Cloud.")
                }.onFailure { err ->
                    android.util.Log.e("ConfigRepository", "Failed to decrypt restored Gemini key", err)
                }
            }
            
            if (!encryptedGroq.isNullOrBlank()) {
                cryptoManager.decryptMessage(encryptedGroq).onSuccess { decrypted ->
                    saveGroqKey(decrypted)
                    android.util.Log.d("ConfigRepository", "Successfully restored Groq API key from E2EE Cloud.")
                }.onFailure { err ->
                    android.util.Log.e("ConfigRepository", "Failed to decrypt restored Groq key", err)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ConfigRepository", "Failed to restore E2EE API keys from cloud", e)
        }
    }
}