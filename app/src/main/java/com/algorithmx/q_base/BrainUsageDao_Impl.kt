package com.algorithmx.q_base.`data`.ai

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.algorithmx.androidmodules.coreai.brain.models.BrainProvider
import com.algorithmx.q_base.`data`.util.TypeConverters
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
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
public class BrainUsageDao_Impl(
  __db: RoomDatabase,
) : BrainUsageDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBrainUsageEntity: EntityInsertAdapter<BrainUsageEntity>

  private val __typeConverters: TypeConverters = TypeConverters()
  init {
    this.__db = __db
    this.__insertAdapterOfBrainUsageEntity = object : EntityInsertAdapter<BrainUsageEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `brain_usage_history` (`id`,`taskId`,`timestampMs`,`provider`,`modelUsed`,`tokensEstimated`,`isSuccess`,`errorMessage`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BrainUsageEntity) {
        statement.bindText(1, entity.id)
        val _tmp: String = __typeConverters.fromBrainTask(entity.taskId)
        statement.bindText(2, _tmp)
        statement.bindLong(3, entity.timestampMs)
        val _tmp_1: String = __typeConverters.fromBrainProvider(entity.provider)
        statement.bindText(4, _tmp_1)
        statement.bindText(5, entity.modelUsed)
        statement.bindLong(6, entity.tokensEstimated.toLong())
        val _tmp_2: Int = if (entity.isSuccess) 1 else 0
        statement.bindLong(7, _tmp_2.toLong())
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpErrorMessage)
        }
      }
    }
  }

  public override suspend fun insertUsageRecord(record: BrainUsageEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBrainUsageEntity.insert(_connection, record)
  }

  public override fun getRecentUsage(limit: Int): Flow<List<BrainUsageEntity>> {
    val _sql: String = "SELECT * FROM brain_usage_history ORDER BY timestampMs DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("brain_usage_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskId: Int = getColumnIndexOrThrow(_stmt, "taskId")
        val _columnIndexOfTimestampMs: Int = getColumnIndexOrThrow(_stmt, "timestampMs")
        val _columnIndexOfProvider: Int = getColumnIndexOrThrow(_stmt, "provider")
        val _columnIndexOfModelUsed: Int = getColumnIndexOrThrow(_stmt, "modelUsed")
        val _columnIndexOfTokensEstimated: Int = getColumnIndexOrThrow(_stmt, "tokensEstimated")
        val _columnIndexOfIsSuccess: Int = getColumnIndexOrThrow(_stmt, "isSuccess")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: MutableList<BrainUsageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BrainUsageEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTaskId: BrainTask
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfTaskId)
          _tmpTaskId = __typeConverters.toBrainTask(_tmp)
          val _tmpTimestampMs: Long
          _tmpTimestampMs = _stmt.getLong(_columnIndexOfTimestampMs)
          val _tmpProvider: BrainProvider
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfProvider)
          _tmpProvider = __typeConverters.toBrainProvider(_tmp_1)
          val _tmpModelUsed: String
          _tmpModelUsed = _stmt.getText(_columnIndexOfModelUsed)
          val _tmpTokensEstimated: Int
          _tmpTokensEstimated = _stmt.getLong(_columnIndexOfTokensEstimated).toInt()
          val _tmpIsSuccess: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsSuccess).toInt()
          _tmpIsSuccess = _tmp_2 != 0
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _item = BrainUsageEntity(_tmpId,_tmpTaskId,_tmpTimestampMs,_tmpProvider,_tmpModelUsed,_tmpTokensEstimated,_tmpIsSuccess,_tmpErrorMessage)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalSuccessfulTokens(): Flow<Int?> {
    val _sql: String = "SELECT SUM(tokensEstimated) FROM brain_usage_history WHERE isSuccess = 1"
    return createFlow(__db, false, arrayOf("brain_usage_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalTokensForTask(taskId: BrainTask): Flow<Int?> {
    val _sql: String = "SELECT SUM(tokensEstimated) FROM brain_usage_history WHERE taskId = ? AND isSuccess = 1"
    return createFlow(__db, false, arrayOf("brain_usage_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String = __typeConverters.fromBrainTask(taskId)
        _stmt.bindText(_argIndex, _tmp)
        val _result: Int?
        if (_stmt.step()) {
          val _tmp_1: Int?
          if (_stmt.isNull(0)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getLong(0).toInt()
          }
          _result = _tmp_1
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pruneOldRecords(olderThanMs: Long) {
    val _sql: String = "DELETE FROM brain_usage_history WHERE timestampMs < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, olderThanMs)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllBrainUsage() {
    val _sql: String = "DELETE FROM brain_usage_history"
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
