package com.algorithmx.q_base.`data`.collections

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ProblemReportDao_Impl(
  __db: RoomDatabase,
) : ProblemReportDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfProblemReport: EntityInsertAdapter<ProblemReport>
  init {
    this.__db = __db
    this.__insertAdapterOfProblemReport = object : EntityInsertAdapter<ProblemReport>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Problem_Reports` (`report_id`,`question_id`,`explanation`,`timestamp`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProblemReport) {
        statement.bindLong(1, entity.reportId)
        statement.bindText(2, entity.questionId)
        statement.bindText(3, entity.explanation)
        statement.bindLong(4, entity.timestamp)
      }
    }
  }

  public override suspend fun insertReport(report: ProblemReport): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfProblemReport.insert(_connection, report)
  }

  public override fun getAllReports(): Flow<List<ProblemReport>> {
    val _sql: String = "SELECT * FROM Problem_Reports"
    return createFlow(__db, false, arrayOf("Problem_Reports")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfReportId: Int = getColumnIndexOrThrow(_stmt, "report_id")
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfExplanation: Int = getColumnIndexOrThrow(_stmt, "explanation")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<ProblemReport> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProblemReport
          val _tmpReportId: Long
          _tmpReportId = _stmt.getLong(_columnIndexOfReportId)
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpExplanation: String
          _tmpExplanation = _stmt.getText(_columnIndexOfExplanation)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = ProblemReport(_tmpReportId,_tmpQuestionId,_tmpExplanation,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getReportsForQuestion(questionId: String): Flow<List<ProblemReport>> {
    val _sql: String = "SELECT * FROM Problem_Reports WHERE question_id = ?"
    return createFlow(__db, false, arrayOf("Problem_Reports")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfReportId: Int = getColumnIndexOrThrow(_stmt, "report_id")
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfExplanation: Int = getColumnIndexOrThrow(_stmt, "explanation")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<ProblemReport> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProblemReport
          val _tmpReportId: Long
          _tmpReportId = _stmt.getLong(_columnIndexOfReportId)
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpExplanation: String
          _tmpExplanation = _stmt.getText(_columnIndexOfExplanation)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = ProblemReport(_tmpReportId,_tmpQuestionId,_tmpExplanation,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
