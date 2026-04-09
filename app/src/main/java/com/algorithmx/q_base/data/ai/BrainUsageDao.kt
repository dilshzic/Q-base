package com.algorithmx.q_base.data.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageRecord(record: BrainUsageEntity)

    @Query("SELECT * FROM brain_usage_history ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentUsage(limit: Int = 100): Flow<List<BrainUsageEntity>>

    @Query("SELECT SUM(tokensEstimated) FROM brain_usage_history WHERE isSuccess = 1")
    fun getTotalSuccessfulTokens(): Flow<Int?>

    @Query("SELECT SUM(tokensEstimated) FROM brain_usage_history WHERE taskId = :taskId AND isSuccess = 1")
    fun getTotalTokensForTask(taskId: BrainTask): Flow<Int?>

    @Query("DELETE FROM brain_usage_history WHERE timestampMs < :olderThanMs")
    suspend fun pruneOldRecords(olderThanMs: Long)

    @Query("DELETE FROM brain_usage_history")
    suspend fun deleteAllBrainUsage()
}
