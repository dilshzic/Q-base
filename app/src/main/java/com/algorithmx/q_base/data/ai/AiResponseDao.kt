package com.algorithmx.q_base.data.ai

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiResponseDao {
    @Query("SELECT * FROM Ai_Responses ORDER BY timestamp DESC")
    fun getAllResponses(): Flow<List<AiResponseEntity>>

    @Query("SELECT * FROM Ai_Responses WHERE response_id = :responseId")
    suspend fun getResponseById(responseId: String): AiResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: AiResponseEntity)

    @Update
    suspend fun updateResponse(response: AiResponseEntity)

    @Query("DELETE FROM Ai_Responses WHERE response_id = :responseId")
    suspend fun deleteResponseById(responseId: String)

    @Query("DELETE FROM Ai_Responses")
    suspend fun deleteAllAiResponses()
}
