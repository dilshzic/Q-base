package com.algorithmx.q_base.data.entity

import androidx.room.*

@Entity(tableName = "Ai_Responses")
data class AiResponseEntity(
    @PrimaryKey
    @ColumnInfo(name = "response_id")
    val responseId: String,
    
    @ColumnInfo(name = "topic")
    val topic: String,
    
    @ColumnInfo(name = "raw_json")
    val rawJson: String,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_promoted")
    val isPromoted: Boolean = false
)
