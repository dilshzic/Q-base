package com.algorithmx.q_base.`data`.sync

import androidx.room.EntityDeleteOrUpdateAdapter
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
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ActionQueueDao_Impl(
  __db: RoomDatabase,
) : ActionQueueDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfOfflineActionEntity: EntityInsertAdapter<OfflineActionEntity>

  private val __deleteAdapterOfOfflineActionEntity: EntityDeleteOrUpdateAdapter<OfflineActionEntity>

  private val __updateAdapterOfOfflineActionEntity: EntityDeleteOrUpdateAdapter<OfflineActionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfOfflineActionEntity = object : EntityInsertAdapter<OfflineActionEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Offline_Actions` (`actionId`,`actionType`,`payloadJson`,`timestamp`,`retryCount`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: OfflineActionEntity) {
        statement.bindText(1, entity.actionId)
        statement.bindText(2, entity.actionType)
        statement.bindText(3, entity.payloadJson)
        statement.bindLong(4, entity.timestamp)
        statement.bindLong(5, entity.retryCount.toLong())
      }
    }
    this.__deleteAdapterOfOfflineActionEntity = object : EntityDeleteOrUpdateAdapter<OfflineActionEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `Offline_Actions` WHERE `actionId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: OfflineActionEntity) {
        statement.bindText(1, entity.actionId)
      }
    }
    this.__updateAdapterOfOfflineActionEntity = object : EntityDeleteOrUpdateAdapter<OfflineActionEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `Offline_Actions` SET `actionId` = ?,`actionType` = ?,`payloadJson` = ?,`timestamp` = ?,`retryCount` = ? WHERE `actionId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: OfflineActionEntity) {
        statement.bindText(1, entity.actionId)
        statement.bindText(2, entity.actionType)
        statement.bindText(3, entity.payloadJson)
        statement.bindLong(4, entity.timestamp)
        statement.bindLong(5, entity.retryCount.toLong())
        statement.bindText(6, entity.actionId)
      }
    }
  }

  public override suspend fun insertAction(action: OfflineActionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfOfflineActionEntity.insert(_connection, action)
  }

  public override suspend fun deleteAction(action: OfflineActionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfOfflineActionEntity.handle(_connection, action)
  }

  public override suspend fun updateAction(action: OfflineActionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfOfflineActionEntity.handle(_connection, action)
  }

  public override suspend fun getPendingActions(): List<OfflineActionEntity> {
    val _sql: String = "SELECT * FROM Offline_Actions ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfActionId: Int = getColumnIndexOrThrow(_stmt, "actionId")
        val _columnIndexOfActionType: Int = getColumnIndexOrThrow(_stmt, "actionType")
        val _columnIndexOfPayloadJson: Int = getColumnIndexOrThrow(_stmt, "payloadJson")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfRetryCount: Int = getColumnIndexOrThrow(_stmt, "retryCount")
        val _result: MutableList<OfflineActionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OfflineActionEntity
          val _tmpActionId: String
          _tmpActionId = _stmt.getText(_columnIndexOfActionId)
          val _tmpActionType: String
          _tmpActionType = _stmt.getText(_columnIndexOfActionType)
          val _tmpPayloadJson: String
          _tmpPayloadJson = _stmt.getText(_columnIndexOfPayloadJson)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpRetryCount: Int
          _tmpRetryCount = _stmt.getLong(_columnIndexOfRetryCount).toInt()
          _item = OfflineActionEntity(_tmpActionId,_tmpActionType,_tmpPayloadJson,_tmpTimestamp,_tmpRetryCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAllActions() {
    val _sql: String = "DELETE FROM Offline_Actions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
