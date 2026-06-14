package com.algorithmx.q_base.core.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionAiMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: QuestionAiMessageEntity)

    @Query("SELECT * FROM question_ai_messages WHERE questionId = :questionId ORDER BY timestamp ASC")
    fun getMessagesForQuestion(questionId: String): Flow<List<QuestionAiMessageEntity>>
}
