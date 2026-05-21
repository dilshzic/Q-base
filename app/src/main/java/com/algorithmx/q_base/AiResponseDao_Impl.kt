package com.algorithmx.q_base.`data`.ai

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AiResponseDao_Impl(
  __db: RoomDatabase,
) : AiResponseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAiResponseEntity: EntityInsertAdapter<AiResponseEntity>

  private val __updateAdapterOfAiResponseEntity: EntityDeleteOrUpdateAdapter<AiResponseEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAiResponseEntity = object : EntityInsertAdapter<AiResponseEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Ai_Responses` (`response_id`,`topic`,`raw_json`,`timestamp`,`is_promoted`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AiResponseEntity) {
        statement.bindText(1, entity.responseId)
        statement.bindText(2, entity.topic)
        statement.bindText(3, entity.rawJson)
        statement.bindLong(4, entity.timestamp)
        val _tmp: Int = if (entity.isPromoted) 1 else 0
        statement.bindLong(5, _tmp.toLong())
      }
    }
    this.__updateAdapterOfAiResponseEntity = object : EntityDeleteOrUpdateAdapter<AiResponseEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `Ai_Responses` SET `response_id` = ?,`topic` = ?,`raw_json` = ?,`timestamp` = ?,`is_promoted` = ? WHERE `response_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: AiResponseEntity) {
        statement.bindText(1, entity.responseId)
        statement.bindText(2, entity.topic)
        statement.bindText(3, entity.rawJson)
        statement.bindLong(4, entity.timestamp)
        val _tmp: Int = if (entity.isPromoted) 1 else 0
        statement.bindLong(5, _tmp.toLong())
        statement.bindText(6, entity.responseId)
      }
    }
  }

  public override suspend fun insertResponse(response: AiResponseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAiResponseEntity.insert(_connection, response)
  }

  public override suspend fun updateResponse(response: AiResponseEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfAiResponseEntity.handle(_connection, response)
  }

  public override fun getAllResponses(): Flow<List<AiResponseEntity>> {
    val _sql: String = "SELECT * FROM Ai_Responses ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("Ai_Responses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfResponseId: Int = getColumnIndexOrThrow(_stmt, "response_id")
        val _columnIndexOfTopic: Int = getColumnIndexOrThrow(_stmt, "topic")
        val _columnIndexOfRawJson: Int = getColumnIndexOrThrow(_stmt, "raw_json")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsPromoted: Int = getColumnIndexOrThrow(_stmt, "is_promoted")
        val _result: MutableList<AiResponseEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AiResponseEntity
          val _tmpResponseId: String
          _tmpResponseId = _stmt.getText(_columnIndexOfResponseId)
          val _tmpTopic: String
          _tmpTopic = _stmt.getText(_columnIndexOfTopic)
          val _tmpRawJson: String
          _tmpRawJson = _stmt.getText(_columnIndexOfRawJson)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsPromoted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPromoted).toInt()
          _tmpIsPromoted = _tmp != 0
          _item = AiResponseEntity(_tmpResponseId,_tmpTopic,_tmpRawJson,_tmpTimestamp,_tmpIsPromoted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getResponseById(responseId: String): AiResponseEntity? {
    val _sql: String = "SELECT * FROM Ai_Responses WHERE response_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, responseId)
        val _columnIndexOfResponseId: Int = getColumnIndexOrThrow(_stmt, "response_id")
        val _columnIndexOfTopic: Int = getColumnIndexOrThrow(_stmt, "topic")
        val _columnIndexOfRawJson: Int = getColumnIndexOrThrow(_stmt, "raw_json")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfIsPromoted: Int = getColumnIndexOrThrow(_stmt, "is_promoted")
        val _result: AiResponseEntity?
        if (_stmt.step()) {
          val _tmpResponseId: String
          _tmpResponseId = _stmt.getText(_columnIndexOfResponseId)
          val _tmpTopic: String
          _tmpTopic = _stmt.getText(_columnIndexOfTopic)
          val _tmpRawJson: String
          _tmpRawJson = _stmt.getText(_columnIndexOfRawJson)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpIsPromoted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPromoted).toInt()
          _tmpIsPromoted = _tmp != 0
          _result = AiResponseEntity(_tmpResponseId,_tmpTopic,_tmpRawJson,_tmpTimestamp,_tmpIsPromoted)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteResponseById(responseId: String) {
    val _sql: String = "DELETE FROM Ai_Responses WHERE response_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, responseId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllAiResponses() {
    val _sql: String = "DELETE FROM Ai_Responses"
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
