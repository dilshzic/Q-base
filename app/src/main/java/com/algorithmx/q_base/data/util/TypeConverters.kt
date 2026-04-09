package com.algorithmx.q_base.data.util

import androidx.room.TypeConverter
import com.algorithmx.q_base.core_ai.brain.models.BrainProvider
import com.algorithmx.q_base.core_ai.brain.models.BrainTask

class TypeConverters {
    @TypeConverter
    fun fromBrainTask(task: BrainTask): String {
        return task.name
    }

    @TypeConverter
    fun toBrainTask(name: String): BrainTask {
        return try {
            BrainTask.valueOf(name)
        } catch (e: Exception) {
            BrainTask.CHAT_BOT
        }
    }

    @TypeConverter
    fun fromBrainProvider(provider: BrainProvider): String {
        return provider.name
    }

    @TypeConverter
    fun toBrainProvider(name: String): BrainProvider {
        return try {
            BrainProvider.valueOf(name)
        } catch (e: Exception) {
            BrainProvider.GEMINI
        }
    }
}
