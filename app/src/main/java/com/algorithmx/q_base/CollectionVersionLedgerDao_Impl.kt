package com.algorithmx.q_base.`data`.collections

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CollectionVersionLedgerDao_Impl(
  __db: RoomDatabase,
) : CollectionVersionLedgerDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCollectionVersionLedgerEntity:
      EntityInsertAdapter<CollectionVersionLedgerEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCollectionVersionLedgerEntity = object : EntityInsertAdapter<CollectionVersionLedgerEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `collection_version_ledger` (`collectionId`,`currentRevisionId`,`lastAppliedTimestamp`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CollectionVersionLedgerEntity) {
        statement.bindText(1, entity.collectionId)
        statement.bindLong(2, entity.currentRevisionId.toLong())
        statement.bindLong(3, entity.lastAppliedTimestamp)
      }
    }
  }

  public override suspend fun insertLedger(ledger: CollectionVersionLedgerEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCollectionVersionLedgerEntity.insert(_connection, ledger)
  }

  public override suspend fun getLedgerForCollection(collectionId: String): CollectionVersionLedgerEntity? {
    val _sql: String = "SELECT * FROM collection_version_ledger WHERE collectionId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collectionId")
        val _columnIndexOfCurrentRevisionId: Int = getColumnIndexOrThrow(_stmt, "currentRevisionId")
        val _columnIndexOfLastAppliedTimestamp: Int = getColumnIndexOrThrow(_stmt, "lastAppliedTimestamp")
        val _result: CollectionVersionLedgerEntity?
        if (_stmt.step()) {
          val _tmpCollectionId: String
          _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          val _tmpCurrentRevisionId: Int
          _tmpCurrentRevisionId = _stmt.getLong(_columnIndexOfCurrentRevisionId).toInt()
          val _tmpLastAppliedTimestamp: Long
          _tmpLastAppliedTimestamp = _stmt.getLong(_columnIndexOfLastAppliedTimestamp)
          _result = CollectionVersionLedgerEntity(_tmpCollectionId,_tmpCurrentRevisionId,_tmpLastAppliedTimestamp)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteLedger(collectionId: String) {
    val _sql: String = "DELETE FROM collection_version_ledger WHERE collectionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
