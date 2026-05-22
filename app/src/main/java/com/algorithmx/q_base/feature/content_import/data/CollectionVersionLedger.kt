package com.algorithmx.q_base.feature.content_import.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "collection_version_ledger")
data class CollectionVersionLedgerEntity(
    @PrimaryKey val collectionId: String,
    val currentRevisionId: Int,
    val lastAppliedTimestamp: Long
)

@Dao
interface CollectionVersionLedgerDao {
    @Query("SELECT * FROM collection_version_ledger WHERE collectionId = :collectionId")
    suspend fun getLedgerForCollection(collectionId: String): CollectionVersionLedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedger(ledger: CollectionVersionLedgerEntity)

    @Query("DELETE FROM collection_version_ledger WHERE collectionId = :collectionId")
    suspend fun deleteLedger(collectionId: String)
}