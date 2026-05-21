package com.algorithmx.q_base.data.sync

import androidx.room.*

@Dao
interface ActionQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: OfflineActionEntity)

    @Query("SELECT * FROM Offline_Actions ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<OfflineActionEntity>

    @Delete
    suspend fun deleteAction(action: OfflineActionEntity)

    @Update
    suspend fun updateAction(action: OfflineActionEntity)

    @Query("DELETE FROM Offline_Actions")
    suspend fun clearAllActions()
}