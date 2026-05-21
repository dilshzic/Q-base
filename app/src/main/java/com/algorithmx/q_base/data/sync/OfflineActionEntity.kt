package com.algorithmx.q_base.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Offline_Actions")
data class OfflineActionEntity(
    @PrimaryKey val actionId: String = UUID.randomUUID().toString(),
    val actionType: String,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)