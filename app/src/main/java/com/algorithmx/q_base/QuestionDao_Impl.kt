package com.algorithmx.q_base.`data`.collections

import androidx.collection.ArrayMap
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchArrayMap
import androidx.sqlite.SQLiteConnection
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
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class QuestionDao_Impl(
  __db: RoomDatabase,
) : QuestionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfQuestion: EntityInsertAdapter<Question>

  private val __insertAdapterOfQuestionOption: EntityInsertAdapter<QuestionOption>

  private val __insertAdapterOfAnswer: EntityInsertAdapter<Answer>

  private val __insertAdapterOfQuestionSet: EntityInsertAdapter<QuestionSet>

  private val __insertAdapterOfSetQuestionCrossRef: EntityInsertAdapter<SetQuestionCrossRef>

  private val __updateAdapterOfQuestion: EntityDeleteOrUpdateAdapter<Question>
  init {
    this.__db = __db
    this.__insertAdapterOfQuestion = object : EntityInsertAdapter<Question>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Questions` (`question_id`,`master_category`,`category`,`tags`,`question_type`,`stem`,`is_pinned`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Question) {
        statement.bindText(1, entity.questionId)
        val _tmpCollection: String? = entity.collection
        if (_tmpCollection == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpCollection)
        }
        val _tmpCategory: String? = entity.category
        if (_tmpCategory == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpCategory)
        }
        val _tmpTags: String? = entity.tags
        if (_tmpTags == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpTags)
        }
        val _tmpQuestionType: String? = entity.questionType
        if (_tmpQuestionType == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpQuestionType)
        }
        statement.bindText(6, entity.stem)
        val _tmp: Int = if (entity.isPinned) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
    this.__insertAdapterOfQuestionOption = object : EntityInsertAdapter<QuestionOption>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Question_Options` (`question_id`,`option_letter`,`option_text`,`option_explanation`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: QuestionOption) {
        statement.bindText(1, entity.questionId)
        statement.bindText(2, entity.optionLetter)
        val _tmpOptionText: String? = entity.optionText
        if (_tmpOptionText == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpOptionText)
        }
        val _tmpOptionExplanation: String? = entity.optionExplanation
        if (_tmpOptionExplanation == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpOptionExplanation)
        }
      }
    }
    this.__insertAdapterOfAnswer = object : EntityInsertAdapter<Answer>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Answers` (`question_id`,`correct_answer_string`,`general_explanation`,`references`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Answer) {
        statement.bindText(1, entity.questionId)
        val _tmpCorrectAnswerString: String? = entity.correctAnswerString
        if (_tmpCorrectAnswerString == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpCorrectAnswerString)
        }
        val _tmpGeneralExplanation: String? = entity.generalExplanation
        if (_tmpGeneralExplanation == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpGeneralExplanation)
        }
        val _tmpReferences: String? = entity.references
        if (_tmpReferences == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpReferences)
        }
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
    this.__insertAdapterOfSetQuestionCrossRef = object : EntityInsertAdapter<SetQuestionCrossRef>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `Set_Questions_CrossRef` (`mapping_id`,`set_id`,`question_id`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SetQuestionCrossRef) {
        statement.bindLong(1, entity.mappingId)
        statement.bindText(2, entity.setId)
        statement.bindText(3, entity.questionId)
      }
    }
    this.__updateAdapterOfQuestion = object : EntityDeleteOrUpdateAdapter<Question>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `Questions` SET `question_id` = ?,`master_category` = ?,`category` = ?,`tags` = ?,`question_type` = ?,`stem` = ?,`is_pinned` = ? WHERE `question_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Question) {
        statement.bindText(1, entity.questionId)
        val _tmpCollection: String? = entity.collection
        if (_tmpCollection == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpCollection)
        }
        val _tmpCategory: String? = entity.category
        if (_tmpCategory == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpCategory)
        }
        val _tmpTags: String? = entity.tags
        if (_tmpTags == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpTags)
        }
        val _tmpQuestionType: String? = entity.questionType
        if (_tmpQuestionType == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpQuestionType)
        }
        statement.bindText(6, entity.stem)
        val _tmp: Int = if (entity.isPinned) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindText(8, entity.questionId)
      }
    }
  }

  public override suspend fun insertQuestions(questions: List<Question>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestion.insert(_connection, questions)
  }

  public override suspend fun insertOptions(options: List<QuestionOption>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestionOption.insert(_connection, options)
  }

  public override suspend fun insertAnswers(answers: List<Answer>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAnswer.insert(_connection, answers)
  }

  public override suspend fun insertSet(`set`: QuestionSet): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestionSet.insert(_connection, set)
  }

  public override suspend fun insertQuestion(question: Question): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestion.insert(_connection, question)
  }

  public override suspend fun insertOption(option: QuestionOption): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestionOption.insert(_connection, option)
  }

  public override suspend fun insertAnswer(answer: Answer): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAnswer.insert(_connection, answer)
  }

  public override suspend fun insertSetQuestionCrossRef(crossRef: SetQuestionCrossRef): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSetQuestionCrossRef.insert(_connection, crossRef)
  }

  public override suspend fun updateQuestion(question: Question): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfQuestion.handle(_connection, question)
  }

  public override fun getQuestionsByStudyCollection(collection: String): Flow<List<Question>> {
    val _sql: String = "SELECT * FROM Questions WHERE master_category = ?"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collection)
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

  public override fun getQuestionsByCategory(category: String): Flow<List<Question>> {
    val _sql: String = "SELECT * FROM Questions WHERE category = ?"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
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

  public override fun getQuestionsByTag(tag: String): Flow<List<Question>> {
    val _sql: String = "SELECT * FROM Questions WHERE tags LIKE ?"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, tag)
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

  public override fun getQuestionsByCategoryAndTag(category: String, tag: String): Flow<List<Question>> {
    val _sql: String = "SELECT * FROM Questions WHERE category = ? AND tags LIKE ?"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        _argIndex = 2
        _stmt.bindText(_argIndex, tag)
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

  public override fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> {
    val _sql: String = "SELECT * FROM Question_Options WHERE question_id = ?"
    return createFlow(__db, false, arrayOf("Question_Options")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfOptionLetter: Int = getColumnIndexOrThrow(_stmt, "option_letter")
        val _columnIndexOfOptionText: Int = getColumnIndexOrThrow(_stmt, "option_text")
        val _columnIndexOfOptionExplanation: Int = getColumnIndexOrThrow(_stmt, "option_explanation")
        val _result: MutableList<QuestionOption> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestionOption
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpOptionLetter: String
          _tmpOptionLetter = _stmt.getText(_columnIndexOfOptionLetter)
          val _tmpOptionText: String?
          if (_stmt.isNull(_columnIndexOfOptionText)) {
            _tmpOptionText = null
          } else {
            _tmpOptionText = _stmt.getText(_columnIndexOfOptionText)
          }
          val _tmpOptionExplanation: String?
          if (_stmt.isNull(_columnIndexOfOptionExplanation)) {
            _tmpOptionExplanation = null
          } else {
            _tmpOptionExplanation = _stmt.getText(_columnIndexOfOptionExplanation)
          }
          _item = QuestionOption(_tmpQuestionId,_tmpOptionLetter,_tmpOptionText,_tmpOptionExplanation)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAnswerForQuestion(questionId: String): Flow<Answer?> {
    val _sql: String = "SELECT * FROM Answers WHERE question_id = ?"
    return createFlow(__db, false, arrayOf("Answers")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfCorrectAnswerString: Int = getColumnIndexOrThrow(_stmt, "correct_answer_string")
        val _columnIndexOfGeneralExplanation: Int = getColumnIndexOrThrow(_stmt, "general_explanation")
        val _columnIndexOfReferences: Int = getColumnIndexOrThrow(_stmt, "references")
        val _result: Answer?
        if (_stmt.step()) {
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpCorrectAnswerString: String?
          if (_stmt.isNull(_columnIndexOfCorrectAnswerString)) {
            _tmpCorrectAnswerString = null
          } else {
            _tmpCorrectAnswerString = _stmt.getText(_columnIndexOfCorrectAnswerString)
          }
          val _tmpGeneralExplanation: String?
          if (_stmt.isNull(_columnIndexOfGeneralExplanation)) {
            _tmpGeneralExplanation = null
          } else {
            _tmpGeneralExplanation = _stmt.getText(_columnIndexOfGeneralExplanation)
          }
          val _tmpReferences: String?
          if (_stmt.isNull(_columnIndexOfReferences)) {
            _tmpReferences = null
          } else {
            _tmpReferences = _stmt.getText(_columnIndexOfReferences)
          }
          _result = Answer(_tmpQuestionId,_tmpCorrectAnswerString,_tmpGeneralExplanation,_tmpReferences)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getOptionsForQuestionOnce(questionId: String): List<QuestionOption> {
    val _sql: String = "SELECT * FROM Question_Options WHERE question_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfOptionLetter: Int = getColumnIndexOrThrow(_stmt, "option_letter")
        val _columnIndexOfOptionText: Int = getColumnIndexOrThrow(_stmt, "option_text")
        val _columnIndexOfOptionExplanation: Int = getColumnIndexOrThrow(_stmt, "option_explanation")
        val _result: MutableList<QuestionOption> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestionOption
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpOptionLetter: String
          _tmpOptionLetter = _stmt.getText(_columnIndexOfOptionLetter)
          val _tmpOptionText: String?
          if (_stmt.isNull(_columnIndexOfOptionText)) {
            _tmpOptionText = null
          } else {
            _tmpOptionText = _stmt.getText(_columnIndexOfOptionText)
          }
          val _tmpOptionExplanation: String?
          if (_stmt.isNull(_columnIndexOfOptionExplanation)) {
            _tmpOptionExplanation = null
          } else {
            _tmpOptionExplanation = _stmt.getText(_columnIndexOfOptionExplanation)
          }
          _item = QuestionOption(_tmpQuestionId,_tmpOptionLetter,_tmpOptionText,_tmpOptionExplanation)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAnswerForQuestionOnce(questionId: String): Answer? {
    val _sql: String = "SELECT * FROM Answers WHERE question_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfCorrectAnswerString: Int = getColumnIndexOrThrow(_stmt, "correct_answer_string")
        val _columnIndexOfGeneralExplanation: Int = getColumnIndexOrThrow(_stmt, "general_explanation")
        val _columnIndexOfReferences: Int = getColumnIndexOrThrow(_stmt, "references")
        val _result: Answer?
        if (_stmt.step()) {
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          val _tmpCorrectAnswerString: String?
          if (_stmt.isNull(_columnIndexOfCorrectAnswerString)) {
            _tmpCorrectAnswerString = null
          } else {
            _tmpCorrectAnswerString = _stmt.getText(_columnIndexOfCorrectAnswerString)
          }
          val _tmpGeneralExplanation: String?
          if (_stmt.isNull(_columnIndexOfGeneralExplanation)) {
            _tmpGeneralExplanation = null
          } else {
            _tmpGeneralExplanation = _stmt.getText(_columnIndexOfGeneralExplanation)
          }
          val _tmpReferences: String?
          if (_stmt.isNull(_columnIndexOfReferences)) {
            _tmpReferences = null
          } else {
            _tmpReferences = _stmt.getText(_columnIndexOfReferences)
          }
          _result = Answer(_tmpQuestionId,_tmpCorrectAnswerString,_tmpGeneralExplanation,_tmpReferences)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getQuestionById(questionId: String): Question? {
    val _sql: String = "SELECT * FROM Questions WHERE question_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _columnIndexOfCollection: Int = getColumnIndexOrThrow(_stmt, "master_category")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfQuestionType: Int = getColumnIndexOrThrow(_stmt, "question_type")
        val _columnIndexOfStem: Int = getColumnIndexOrThrow(_stmt, "stem")
        val _columnIndexOfIsPinned: Int = getColumnIndexOrThrow(_stmt, "is_pinned")
        val _result: Question?
        if (_stmt.step()) {
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
          _result = Question(_tmpQuestionId,_tmpCollection,_tmpCategory,_tmpTags,_tmpQuestionType,_tmpStem,_tmpIsPinned)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getQuestionCountFlow(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM Questions"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getUserCreatedQuestionCount(): Flow<Int> {
    val _sql: String = """
        |
        |        SELECT COUNT(q.question_id) 
        |        FROM Questions q 
        |        INNER JOIN StudyCollections c ON q.master_category = c.name 
        |        WHERE c.is_user_created = 1
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("Questions", "StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getSharedQuestionCount(): Flow<Int> {
    val _sql: String = """
        |
        |        SELECT COUNT(q.question_id) 
        |        FROM Questions q 
        |        INNER JOIN StudyCollections c ON q.master_category = c.name 
        |        WHERE c.is_shared = 1
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("Questions", "StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getQuestionCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM Questions"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
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

  public override suspend fun getSetWithContent(setId: String): SetWithQuestions? {
    val _sql: String = "SELECT * FROM Question_Sets WHERE set_id = ?"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, setId)
        val _columnIndexOfSetId: Int = getColumnIndexOrThrow(_stmt, "set_id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfParentCollectionId: Int = getColumnIndexOrThrow(_stmt, "parent_collection_id")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCreatedTimestamp: Int = getColumnIndexOrThrow(_stmt, "created_timestamp")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _collectionQuestions: ArrayMap<String, MutableList<Question>> = ArrayMap<String, MutableList<Question>>()
        while (_stmt.step()) {
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfSetId)
          if (!_collectionQuestions.containsKey(_tmpKey)) {
            _collectionQuestions.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipQuestionsAscomAlgorithmxQBaseDataCollectionsQuestion(_connection, _collectionQuestions)
        val _result: SetWithQuestions?
        if (_stmt.step()) {
          val _tmpSet: QuestionSet
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
          _tmpSet = QuestionSet(_tmpSetId,_tmpTitle,_tmpParentCollectionId,_tmpDescription,_tmpCreatedTimestamp,_tmpIsUserCreated)
          val _tmpQuestionsCollection: MutableList<Question>
          val _tmpKey_1: String
          _tmpKey_1 = _stmt.getText(_columnIndexOfSetId)
          _tmpQuestionsCollection = _collectionQuestions.getValue(_tmpKey_1)
          _result = SetWithQuestions(_tmpSet,_tmpQuestionsCollection)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getQuestionCountByStudyCollection(collectionName: String): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM Questions WHERE master_category = ?"
    return createFlow(__db, false, arrayOf("Questions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionName)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSetIdForQuestion(questionId: String): String? {
    val _sql: String = "SELECT set_id FROM Set_Questions_CrossRef WHERE question_id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteOptionsForQuestion(questionId: String) {
    val _sql: String = "DELETE FROM Question_Options WHERE question_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSetById(setId: String) {
    val _sql: String = "DELETE FROM Question_Sets WHERE set_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, setId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSetsByIds(setIds: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM Question_Sets WHERE set_id IN (")
    val _inputSize: Int = setIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in setIds) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCrossRefsForSets(setIds: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM Set_Questions_CrossRef WHERE set_id IN (")
    val _inputSize: Int = setIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in setIds) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun removeQuestionFromSet(setId: String, questionId: String) {
    val _sql: String = "DELETE FROM Set_Questions_CrossRef WHERE set_id = ? AND question_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, setId)
        _argIndex = 2
        _stmt.bindText(_argIndex, questionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteQuestionById(questionId: String) {
    val _sql: String = "DELETE FROM Questions WHERE question_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, questionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllQuestions() {
    val _sql: String = "DELETE FROM Questions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllOptions() {
    val _sql: String = "DELETE FROM Question_Options"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllAnswers() {
    val _sql: String = "DELETE FROM Answers"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllCrossRefs() {
    val _sql: String = "DELETE FROM Set_Questions_CrossRef"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipQuestionsAscomAlgorithmxQBaseDataCollectionsQuestion(_connection: SQLiteConnection, _map: ArrayMap<String, MutableList<Question>>) {
    val __mapKeySet: Set<String> = _map.keys
    if (__mapKeySet.isEmpty()) {
      return
    }
    if (_map.size > 999) {
      recursiveFetchArrayMap(_map, true) { _tmpMap ->
        __fetchRelationshipQuestionsAscomAlgorithmxQBaseDataCollectionsQuestion(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `Questions`.`question_id` AS `question_id`,`Questions`.`master_category` AS `master_category`,`Questions`.`category` AS `category`,`Questions`.`tags` AS `tags`,`Questions`.`question_type` AS `question_type`,`Questions`.`stem` AS `stem`,`Questions`.`is_pinned` AS `is_pinned`,_junction.`set_id` FROM `Set_Questions_CrossRef` AS _junction INNER JOIN `Questions` ON (_junction.`question_id` = `Questions`.`question_id`) WHERE _junction.`set_id` IN (")
    val _inputSize: Int = __mapKeySet.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (_item: String in __mapKeySet) {
      _stmt.bindText(_argIndex, _item)
      _argIndex++
    }
    try {
      // _junction.set_id
      val _itemKeyIndex: Int = 7
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfQuestionId: Int = 0
      val _columnIndexOfCollection: Int = 1
      val _columnIndexOfCategory: Int = 2
      val _columnIndexOfTags: Int = 3
      val _columnIndexOfQuestionType: Int = 4
      val _columnIndexOfStem: Int = 5
      val _columnIndexOfIsPinned: Int = 6
      while (_stmt.step()) {
        val _tmpKey: String
        _tmpKey = _stmt.getText(_itemKeyIndex)
        val _tmpRelation: MutableList<Question>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: Question
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
          _item_1 = Question(_tmpQuestionId,_tmpCollection,_tmpCategory,_tmpTags,_tmpQuestionType,_tmpStem,_tmpIsPinned)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
