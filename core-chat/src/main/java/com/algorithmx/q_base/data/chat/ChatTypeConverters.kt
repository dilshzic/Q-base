package com.algorithmx.q_base.core.data.chat

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object ChatTypeConverters {
    private val json = Json

    @TypeConverter
    @JvmStatic
    fun fromAdminIds(adminIds: List<String>?): String {
        return json.encodeToString(adminIds ?: emptyList())
    }

    @TypeConverter
    @JvmStatic
    fun toAdminIds(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            // Fallback: parse comma-separated
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
