package com.algorithmx.q_base.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.algorithmx.q_base.brain.models.BrainProvider
import com.algorithmx.q_base.brain.models.BrainTask

@Entity(tableName = "brain_usage_history")
data class BrainUsageEntity(
    @PrimaryKey
    val id: String,
    val taskId: BrainTask,
    val timestampMs: Long,
    val provider: BrainProvider,
    val modelUsed: String,
    val tokensEstimated: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
