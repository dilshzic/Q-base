package com.algorithmx.q_base.`data`.sessions

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.algorithmx.q_base.`data`.collections.Question
import com.algorithmx.q_base.`data`.collections.QuestionSet
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SessionDao_Impl(
  __db: RoomDatabase,
) : SessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfStudySession: EntityInsertAdapter<StudySession>

  private val __insertAdapterOfQuestionSet: EntityInsertAdapter<QuestionSet>

  private val __insertAdapterOfSessionAttempt: EntityInsertAdapter<SessionAttempt>

  private val __updateAdapterOfStudySession: EntityDeleteOrUpdateAdapter<StudySession>

  private val __updateAdapterOfSessionAttempt: EntityDeleteOrUpdateAdapter<SessionAttempt>
  init {
    this.__db = __db
    this.__insertAdapterOfStudySession = object : EntityInsertAdapter<StudySession>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Study_Sessions` (`session_id`,`title`,`time_limit_seconds`,`score_achieved`,`created_timestamp`,`is_completed`,`timing_type`,`is_random`,`collection_id`,`last_question_index`,`is_admin_only`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StudySession) {
        statement.bindText(1, entity.sessionId)
        statement.bindText(2, entity.title)
        val _tmpTimeLimitSeconds: Int? = entity.timeLimitSeconds
        if (_tmpTimeLimitSeconds == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpTimeLimitSeconds.toLong())
        }
        statement.bindDouble(4, entity.scoreAchieved.toDouble())
        statement.bindLong(5, entity.createdTimestamp)
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindText(7, entity.timingType)
        val _tmp_1: Int = if (entity.isRandom) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        val _tmpCollectionId: String? = entity.collectionId
        if (_tmpCollectionId == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpCollectionId)
        }
        statement.bindLong(10, entity.lastQuestionIndex.toLong())
        val _tmp_2: Int = if (entity.isAdminOnly) 1 else 0
        statement.bindLong(11, _tmp_2.toLong())
      }
    }
    this.__insertAdapterOfQuestionSet = object : EntityInsertAdapter<QuestionSet>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Question_Sets` (`set_id`,`title`,`parent_collection_id`,`description`,`created_timestamp`,`is_user_created`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: QuestionSet) {
        statement.bindText(1, entity.setId)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.parentCollectionId)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpDescription)
        }
        statement.bindLong(5, entity.createdTimestamp)
        val _tmp: Int = if (entity.isUserCreated) 1 else 0
        statement.bindLong(6, _tmp.toLong())
      }
    }
    this.__insertAdapterOfSessionAttempt = object : EntityInsertAdapter<SessionAttempt>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Session_Attempts` (`attempt_id`,`session_id`,`question_id`,`attempt_status`,`user_selected_answers`,`time_spent_seconds`,`marks_obtained`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SessionAttempt) {
        statement.bindLong(1, entity.attemptId.toLong())
        statement.bindText(2, entity.sessionId)
        statement.bindText(3, entity.questionId)
        statement.bindText(4, entity.attemptStatus)
        statement.bindText(5, entity.userSelectedAnswers)
        statement.bindLong(6, entity.timeSpentSeconds.toLong())
        statement.bindDouble(7, entity.marksObtained.toDouble())
      }
    }
    this.__updateAdapterOfStudySession = object : EntityDeleteOrUpdateAdapter<StudySession>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `Study_Sessions` SET `session_id` = ?,`title` = ?,`time_limit_seconds` = ?,`score_achieved` = ?,`created_timestamp` = ?,`is_completed` = ?,`timing_type` = ?,`is_random` = ?,`collection_id` = ?,`last_question_index` = ?,`is_admin_only` = ? WHERE `session_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: StudySession) {
        statement.bindText(1, entity.sessionId)
        statement.bindText(2, entity.title)
        val _tmpTimeLimitSeconds: Int? = entity.timeLimitSeconds
        if (_tmpTimeLimitSeconds == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmpTimeLimitSeconds.toLong())
        }
        statement.bindDouble(4, entity.scoreAchieved.toDouble())
        statement.bindLong(5, entity.createdTimestamp)
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindText(7, entity.timingType)
        val _tmp_1: Int = if (entity.isRandom) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        val _tmpCollectionId: String? = entity.collectionId
        if (_tmpCollectionId == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpCollectionId)
        }
        statement.bindLong(10, entity.lastQuestionIndex.toLong())
        val _tmp_2: Int = if (entity.isAdminOnly) 1 else 0
        statement.bindLong(11, _tmp_2.toLong())
        statement.bindText(12, entity.sessionId)
      }
    }
    this.__updateAdapterOfSessionAttempt = object : EntityDeleteOrUpdateAdapter<SessionAttempt>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `Session_Attempts` SET `attempt_id` = ?,`session_id` = ?,`question_id` = ?,`attempt_status` = ?,`user_selected_answers` = ?,`time_spent_seconds` = ?,`marks_obtained` = ? WHERE `attempt_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SessionAttempt) {
        statement.bindLong(1, entity.attemptId.toLong())
        statement.bindText(2, entity.sessionId)
        statement.bindText(3, entity.questionId)
        statement.bindText(4, entity.attemptStatus)
        statement.bindText(5, entity.userSelectedAnswers)
        statement.bindLong(6, entity.timeSpentSeconds.toLong())
        statement.bindDouble(7, entity.marksObtained.toDouble())
        statement.bindLong(8, entity.attemptId.toLong())
      }
    }
  }

  public override suspend fun insertSession(session: StudySession): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStudySession.insert(_connection, session)
  }

  public override suspend fun insertSet(`set`: QuestionSet): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestionSet.insert(_connection, set)
  }

  public override suspend fun insertAttempts(attempts: List<SessionAttempt>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSessionAttempt.insert(_connection, attempts)
  }

  public override suspend fun updateSession(session: StudySession): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfStudySession.handle(_connection, session)
  }

  public override suspend fun updateAttempt(attempt: SessionAttempt): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfSessionAttempt.handle(_connection, attempt)
  }

  public override fun getAllSessions(): Flow<List<StudySession>> {
    val _sql: String = "SELECT * FROM Study_Sessions ORDER BY created_timestamp DESC"
    return createFlow(__db, false, arrayOf("Study_Sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfTimeLimitSeconds: Int = getColumnIndexOrThrow(_stmt, "time_limit_seconds")
        val _columnIndexOfScoreAchieved: Int = getColumnIndexOrThrow(_stmt, "score_achieved")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "is_completed")
        val _columnIndexOfTimingType: Int = getColumnIndexOrThrow(_stmt, "timing_type")
        val _columnIndexOfIsRandom: Int = getColumnIndexOrThrow(_stmt, "is_random")
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfLastQuestionIndex: Int = getColumnIndexOrThrow(_stmt, "last_question_index")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _result: MutableList<StudySession> = mutableListOf()
        while (_stmt.step()) {
          val _item: StudySession
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpTimeLimitSeconds: Int?
          if (_stmt.isNull(_columnIndexOfTimeLimitSeconds)) {
            _tmpTimeLimitSeconds = null
          } else {
            _tmpTimeLimitSeconds = _stmt.getLong(_columnIndexOfTimeLimitSeconds).toInt()
          }
          val _tmpScoreAchieved: Float
          _tmpScoreAchieved = _stmt.getDouble(_columnIndexOfScoreAchieved).toFloat()
          val _tmpCreatedTimestamp: Long
          _tmpCreatedTimestamp = _stmt.getLong(_columnIndexOfCreatedTimestamp)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpTimingType: String
          _tmpTimingType = _stmt.getText(_columnIndexOfTimingType)
          val _tmpIsRandom: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRandom).toInt()
          _tmpIsRandom = _tmp_1 != 0
          val _tmpCollectionId: String?
          if (_stmt.isNull(_columnIndexOfCollectionId)) {
            _tmpCollectionId = null
          } else {
            _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          }
          val _tmpLastQuestionIndex: Int
          _tmpLastQuestionIndex = _stmt.getLong(_columnIndexOfLastQuestionIndex).toInt()
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          _item = StudySession(_tmpSessionId,_tmpTitle,_tmpTimeLimitSeconds,_tmpScoreAchieved,_tmpCreatedTimestamp,_tmpIsCompleted,_tmpTimingType,_tmpIsRandom,_tmpCollectionId,_tmpLastQuestionIndex,_tmpIsAdminOnly)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getOngoingSessions(): Flow<List<StudySession>> {
    val _sql: String = "SELECT * FROM Study_Sessions WHERE is_completed = 0 ORDER BY created_timestamp DESC"
    return createFlow(__db, false, arrayOf("Study_Sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfTimeLimitSeconds: Int = getColumnIndexOrThrow(_stmt, "time_limit_seconds")
        val _columnIndexOfScoreAchieved: Int = getColumnIndexOrThrow(_stmt, "score_achieved")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "is_completed")
        val _columnIndexOfTimingType: Int = getColumnIndexOrThrow(_stmt, "timing_type")
        val _columnIndexOfIsRandom: Int = getColumnIndexOrThrow(_stmt, "is_random")
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfLastQuestionIndex: Int = getColumnIndexOrThrow(_stmt, "last_question_index")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _result: MutableList<StudySession> = mutableListOf()
        while (_stmt.step()) {
          val _item: StudySession
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpTimeLimitSeconds: Int?
          if (_stmt.isNull(_columnIndexOfTimeLimitSeconds)) {
            _tmpTimeLimitSeconds = null
          } else {
            _tmpTimeLimitSeconds = _stmt.getLong(_columnIndexOfTimeLimitSeconds).toInt()
          }
          val _tmpScoreAchieved: Float
          _tmpScoreAchieved = _stmt.getDouble(_columnIndexOfScoreAchieved).toFloat()
          val _tmpCreatedTimestamp: Long
          _tmpCreatedTimestamp = _stmt.getLong(_columnIndexOfCreatedTimestamp)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpTimingType: String
          _tmpTimingType = _stmt.getText(_columnIndexOfTimingType)
          val _tmpIsRandom: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRandom).toInt()
          _tmpIsRandom = _tmp_1 != 0
          val _tmpCollectionId: String?
          if (_stmt.isNull(_columnIndexOfCollectionId)) {
            _tmpCollectionId = null
          } else {
            _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          }
          val _tmpLastQuestionIndex: Int
          _tmpLastQuestionIndex = _stmt.getLong(_columnIndexOfLastQuestionIndex).toInt()
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          _item = StudySession(_tmpSessionId,_tmpTitle,_tmpTimeLimitSeconds,_tmpScoreAchieved,_tmpCreatedTimestamp,_tmpIsCompleted,_tmpTimingType,_tmpIsRandom,_tmpCollectionId,_tmpLastQuestionIndex,_tmpIsAdminOnly)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSessionById(sessionId: String): StudySession? {
    val _sql: String = "SELECT * FROM Study_Sessions WHERE session_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfTimeLimitSeconds: Int = getColumnIndexOrThrow(_stmt, "time_limit_seconds")
        val _columnIndexOfScoreAchieved: Int = getColumnIndexOrThrow(_stmt, "score_achieved")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "is_completed")
        val _columnIndexOfTimingType: Int = getColumnIndexOrThrow(_stmt, "timing_type")
        val _columnIndexOfIsRandom: Int = getColumnIndexOrThrow(_stmt, "is_random")
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfLastQuestionIndex: Int = getColumnIndexOrThrow(_stmt, "last_question_index")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _result: StudySession?
        if (_stmt.step()) {
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpTimeLimitSeconds: Int?
          if (_stmt.isNull(_columnIndexOfTimeLimitSeconds)) {
            _tmpTimeLimitSeconds = null
          } else {
            _tmpTimeLimitSeconds = _stmt.getLong(_columnIndexOfTimeLimitSeconds).toInt()
          }
          val _tmpScoreAchieved: Float
          _tmpScoreAchieved = _stmt.getDouble(_columnIndexOfScoreAchieved).toFloat()
          val _tmpCreatedTimestamp: Long
          _tmpCreatedTimestamp = _stmt.getLong(_columnIndexOfCreatedTimestamp)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpTimingType: String
          _tmpTimingType = _stmt.getText(_columnIndexOfTimingType)
          val _tmpIsRandom: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRandom).toInt()
          _tmpIsRandom = _tmp_1 != 0
          val _tmpCollectionId: String?
          if (_stmt.isNull(_columnIndexOfCollectionId)) {
            _tmpCollectionId = null
          } else {
            _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          }
          val _tmpLastQuestionIndex: Int
          _tmpLastQuestionIndex = _stmt.getLong(_columnIndexOfLastQuestionIndex).toInt()
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          _result = StudySession(_tmpSessionId,_tmpTitle,_tmpTimeLimitSeconds,_tmpScoreAchieved,_tmpCreatedTimestamp,_tmpIsCompleted,_tmpTimingType,_tmpIsRandom,_tmpCollectionId,_tmpLastQuestionIndex,_tmpIsAdminOnly)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllSets(): Flow<List<QuestionSet>> {
    val _sql: String = "SELECT * FROM Question_Sets ORDER BY created_timestamp DESC"
    return createFlow(__db, false, arrayOf("Question_Sets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSetId: Int = getColumnIndexOrThrow(_stmt, "set_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfParentCollectionId: Int = getColumnIndexOrThrow(_stmt, "parent_collection_id")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _result: MutableList<QuestionSet> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestionSet
          val _tmpSetId: String
          _tmpSetId = _stmt.getText(_columnIndexOfSetId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpParentCollectionId: String
          _tmpParentCollectionId = _stmt.getText(_columnIndexOfParentCollectionId)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpCreatedTimestamp: Long
          _tmpCreatedTimestamp = _stmt.getLong(_columnIndexOfCreatedTimestamp)
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          _item = QuestionSet(_tmpSetId,_tmpTitle,_tmpParentCollectionId,_tmpDescription,_tmpCreatedTimestamp,_tmpIsUserCreated)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getRecentSets(): Flow<List<QuestionSet>> {
    val _sql: String = "SELECT * FROM Question_Sets ORDER BY created_timestamp DESC LIMIT 5"
    return createFlow(__db, false, arrayOf("Question_Sets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSetId: Int = getColumnIndexOrThrow(_stmt, "set_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfParentCollectionId: Int = getColumnIndexOrThrow(_stmt, "parent_collection_id")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _result: MutableList<QuestionSet> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestionSet
          val _tmpSetId: String
          _tmpSetId = _stmt.getText(_columnIndexOfSetId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpParentCollectionId: String
          _tmpParentCollectionId = _stmt.getText(_columnIndexOfParentCollectionId)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpCreatedTimestamp: Long
          _tmpCreatedTimestamp = _stmt.getLong(_columnIndexOfCreatedTimestamp)
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          _item = QuestionSet(_tmpSetId,_tmpTitle,_tmpParentCollectionId,_tmpDescription,_tmpCreatedTimestamp,_tmpIsUserCreated)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPinnedQuestions(): Flow<List<Question>> {
    val _sql: String = "SELECT * FROM Questions WHERE is_pinned = 1"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfCollection: Int = getColumnIndexOrThrow(_stmt, "master_category")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfQuestionType: Int = getColumnIndexOrThrow(_stmt, "question_type")
        val _columnIndexOfStem: Int = getColumnIndexOrThrow(_stmt, "stem")
        val _columnIndexOfIsPinned: Int = getColumnIndexOrThrow(_stmt, "is_pinned")
        val _result: MutableList<Question> = mutableListOf()
        while (_stmt.step()) {
          val _item: Question
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpCollection: String?
          if (_stmt.isNull(_columnIndexOfCollection)) {
            _tmpCollection = null
          } else {
            _tmpCollection = _stmt.getText(_columnIndexOfCollection)
          }
          val _tmpCategory: String?
          if (_stmt.isNull(_columnIndexOfCategory)) {
            _tmpCategory = null
          } else {
            _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          }
          val _tmpTags: String?
          if (_stmt.isNull(_columnIndexOfTags)) {
            _tmpTags = null
          } else {
            _tmpTags = _stmt.getText(_columnIndexOfTags)
          }
          val _tmpQuestionType: String?
          if (_stmt.isNull(_columnIndexOfQuestionType)) {
            _tmpQuestionType = null
          } else {
            _tmpQuestionType = _stmt.getText(_columnIndexOfQuestionType)
          }
          val _tmpStem: String
          _tmpStem = _stmt.getText(_columnIndexOfStem)
          val _tmpIsPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPinned).toInt()
          _tmpIsPinned = _tmp != 0
          _item = Question(_tmpQuestionId,_tmpCollection,_tmpCategory,_tmpTags,_tmpQuestionType,_tmpStem,_tmpIsPinned)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>> {
    val _sql: String = "SELECT * FROM Session_Attempts WHERE session_id = ?"
    return createFlow(__db, false, arrayOf("Session_Attempts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        val _columnIndexOfAttemptId: Int = getColumnIndexOrThrow(_stmt, "attempt_id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfAttemptStatus: Int = getColumnIndexOrThrow(_stmt, "attempt_status")
        val _columnIndexOfUserSelectedAnswers: Int = getColumnIndexOrThrow(_stmt, "user_selected_answers")
        val _columnIndexOfTimeSpentSeconds: Int = getColumnIndexOrThrow(_stmt, "time_spent_seconds")
        val _columnIndexOfMarksObtained: Int = getColumnIndexOrThrow(_stmt, "marks_obtained")
        val _result: MutableList<SessionAttempt> = mutableListOf()
        while (_stmt.step()) {
          val _item: SessionAttempt
          val _tmpAttemptId: Int
          _tmpAttemptId = _stmt.getLong(_columnIndexOfAttemptId).toInt()
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpAttemptStatus: String
          _tmpAttemptStatus = _stmt.getText(_columnIndexOfAttemptStatus)
          val _tmpUserSelectedAnswers: String
          _tmpUserSelectedAnswers = _stmt.getText(_columnIndexOfUserSelectedAnswers)
          val _tmpTimeSpentSeconds: Int
          _tmpTimeSpentSeconds = _stmt.getLong(_columnIndexOfTimeSpentSeconds).toInt()
          val _tmpMarksObtained: Float
          _tmpMarksObtained = _stmt.getDouble(_columnIndexOfMarksObtained).toFloat()
          _item = SessionAttempt(_tmpAttemptId,_tmpSessionId,_tmpQuestionId,_tmpAttemptStatus,_tmpUserSelectedAnswers,_tmpTimeSpentSeconds,_tmpMarksObtained)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAttemptsForSessionOnce(sessionId: String): List<SessionAttempt> {
    val _sql: String = "SELECT * FROM Session_Attempts WHERE session_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        val _columnIndexOfAttemptId: Int = getColumnIndexOrThrow(_stmt, "attempt_id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfAttemptStatus: Int = getColumnIndexOrThrow(_stmt, "attempt_status")
        val _columnIndexOfUserSelectedAnswers: Int = getColumnIndexOrThrow(_stmt, "user_selected_answers")
        val _columnIndexOfTimeSpentSeconds: Int = getColumnIndexOrThrow(_stmt, "time_spent_seconds")
        val _columnIndexOfMarksObtained: Int = getColumnIndexOrThrow(_stmt, "marks_obtained")
        val _result: MutableList<SessionAttempt> = mutableListOf()
        while (_stmt.step()) {
          val _item: SessionAttempt
          val _tmpAttemptId: Int
          _tmpAttemptId = _stmt.getLong(_columnIndexOfAttemptId).toInt()
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpAttemptStatus: String
          _tmpAttemptStatus = _stmt.getText(_columnIndexOfAttemptStatus)
          val _tmpUserSelectedAnswers: String
          _tmpUserSelectedAnswers = _stmt.getText(_columnIndexOfUserSelectedAnswers)
          val _tmpTimeSpentSeconds: Int
          _tmpTimeSpentSeconds = _stmt.getLong(_columnIndexOfTimeSpentSeconds).toInt()
          val _tmpMarksObtained: Float
          _tmpMarksObtained = _stmt.getDouble(_columnIndexOfMarksObtained).toFloat()
          _item = SessionAttempt(_tmpAttemptId,_tmpSessionId,_tmpQuestionId,_tmpAttemptStatus,_tmpUserSelectedAnswers,_tmpTimeSpentSeconds,_tmpMarksObtained)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAttempt(sessionId: String, questionId: String): SessionAttempt? {
    val _sql: String = "SELECT * FROM Session_Attempts WHERE session_id = ? AND question_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        _argIndex = 2
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfAttemptId: Int = getColumnIndexOrThrow(_stmt, "attempt_id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfAttemptStatus: Int = getColumnIndexOrThrow(_stmt, "attempt_status")
        val _columnIndexOfUserSelectedAnswers: Int = getColumnIndexOrThrow(_stmt, "user_selected_answers")
        val _columnIndexOfTimeSpentSeconds: Int = getColumnIndexOrThrow(_stmt, "time_spent_seconds")
        val _columnIndexOfMarksObtained: Int = getColumnIndexOrThrow(_stmt, "marks_obtained")
        val _result: SessionAttempt?
        if (_stmt.step()) {
          val _tmpAttemptId: Int
          _tmpAttemptId = _stmt.getLong(_columnIndexOfAttemptId).toInt()
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpAttemptStatus: String
          _tmpAttemptStatus = _stmt.getText(_columnIndexOfAttemptStatus)
          val _tmpUserSelectedAnswers: String
          _tmpUserSelectedAnswers = _stmt.getText(_columnIndexOfUserSelectedAnswers)
          val _tmpTimeSpentSeconds: Int
          _tmpTimeSpentSeconds = _stmt.getLong(_columnIndexOfTimeSpentSeconds).toInt()
          val _tmpMarksObtained: Float
          _tmpMarksObtained = _stmt.getDouble(_columnIndexOfMarksObtained).toFloat()
          _result = SessionAttempt(_tmpAttemptId,_tmpSessionId,_tmpQuestionId,_tmpAttemptStatus,_tmpUserSelectedAnswers,_tmpTimeSpentSeconds,_tmpMarksObtained)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getLastSessionForStudyCollection(collectionId: String): Flow<StudySession?> {
    val _sql: String = "SELECT * FROM Study_Sessions WHERE collection_id = ? AND is_completed = 0 ORDER BY created_timestamp DESC LIMIT 1"
    return createFlow(__db, false, arrayOf("Study_Sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "session_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfTimeLimitSeconds: Int = getColumnIndexOrThrow(_stmt, "time_limit_seconds")
        val _columnIndexOfScoreAchieved: Int = getColumnIndexOrThrow(_stmt, "score_achieved")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "is_completed")
        val _columnIndexOfTimingType: Int = getColumnIndexOrThrow(_stmt, "timing_type")
        val _columnIndexOfIsRandom: Int = getColumnIndexOrThrow(_stmt, "is_random")
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfLastQuestionIndex: Int = getColumnIndexOrThrow(_stmt, "last_question_index")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _result: StudySession?
        if (_stmt.step()) {
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpTimeLimitSeconds: Int?
          if (_stmt.isNull(_columnIndexOfTimeLimitSeconds)) {
            _tmpTimeLimitSeconds = null
          } else {
            _tmpTimeLimitSeconds = _stmt.getLong(_columnIndexOfTimeLimitSeconds).toInt()
          }
          val _tmpScoreAchieved: Float
          _tmpScoreAchieved = _stmt.getDouble(_columnIndexOfScoreAchieved).toFloat()
          val _tmpCreatedTimestamp: Long
          _tmpCreatedTimestamp = _stmt.getLong(_columnIndexOfCreatedTimestamp)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpTimingType: String
          _tmpTimingType = _stmt.getText(_columnIndexOfTimingType)
          val _tmpIsRandom: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRandom).toInt()
          _tmpIsRandom = _tmp_1 != 0
          val _tmpCollectionId: String?
          if (_stmt.isNull(_columnIndexOfCollectionId)) {
            _tmpCollectionId = null
          } else {
            _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          }
          val _tmpLastQuestionIndex: Int
          _tmpLastQuestionIndex = _stmt.getLong(_columnIndexOfLastQuestionIndex).toInt()
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          _result = StudySession(_tmpSessionId,_tmpTitle,_tmpTimeLimitSeconds,_tmpScoreAchieved,_tmpCreatedTimestamp,_tmpIsCompleted,_tmpTimingType,_tmpIsRandom,_tmpCollectionId,_tmpLastQuestionIndex,_tmpIsAdminOnly)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSessionsByIds(sessionIds: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM Study_Sessions WHERE session_id IN (")
    val _inputSize: Int = sessionIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in sessionIds) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAttemptsForSessions(sessionIds: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM Session_Attempts WHERE session_id IN (")
    val _inputSize: Int = sessionIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in sessionIds) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllSessions() {
    val _sql: String = "DELETE FROM Study_Sessions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllAttempts() {
    val _sql: String = "DELETE FROM Session_Attempts"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllSets() {
    val _sql: String = "DELETE FROM Question_Sets"
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
