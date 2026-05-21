package com.algorithmx.q_base.`data`.collections

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CollectionDao_Impl(
  __db: RoomDatabase,
) : CollectionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfStudyCollection: EntityInsertAdapter<StudyCollection>

  private val __insertAdapterOfQuestionSet: EntityInsertAdapter<QuestionSet>

  private val __insertAdapterOfSetQuestionCrossRef: EntityInsertAdapter<SetQuestionCrossRef>

  private val __updateAdapterOfStudyCollection: EntityDeleteOrUpdateAdapter<StudyCollection>
  init {
    this.__db = __db
    this.__insertAdapterOfStudyCollection = object : EntityInsertAdapter<StudyCollection>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `StudyCollections` (`collection_id`,`name`,`description`,`is_user_created`,`is_shared`,`shared_with_group_id`,`is_admin_only`,`updated_at`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StudyCollection) {
        statement.bindText(1, entity.collectionId)
        statement.bindText(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpDescription)
        }
        val _tmp: Int = if (entity.isUserCreated) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.isShared) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        val _tmpSharedWithGroupId: String? = entity.sharedWithGroupId
        if (_tmpSharedWithGroupId == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpSharedWithGroupId)
        }
        val _tmp_2: Int = if (entity.isAdminOnly) 1 else 0
        statement.bindLong(7, _tmp_2.toLong())
        statement.bindLong(8, entity.updatedAt)
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
    this.__updateAdapterOfStudyCollection = object : EntityDeleteOrUpdateAdapter<StudyCollection>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `StudyCollections` SET `collection_id` = ?,`name` = ?,`description` = ?,`is_user_created` = ?,`is_shared` = ?,`shared_with_group_id` = ?,`is_admin_only` = ?,`updated_at` = ? WHERE `collection_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: StudyCollection) {
        statement.bindText(1, entity.collectionId)
        statement.bindText(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpDescription)
        }
        val _tmp: Int = if (entity.isUserCreated) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.isShared) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        val _tmpSharedWithGroupId: String? = entity.sharedWithGroupId
        if (_tmpSharedWithGroupId == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpSharedWithGroupId)
        }
        val _tmp_2: Int = if (entity.isAdminOnly) 1 else 0
        statement.bindLong(7, _tmp_2.toLong())
        statement.bindLong(8, entity.updatedAt)
        statement.bindText(9, entity.collectionId)
      }
    }
  }

  public override suspend fun insertStudyCollections(collections: List<StudyCollection>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStudyCollection.insert(_connection, collections)
  }

  public override suspend fun insertSets(sets: List<QuestionSet>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestionSet.insert(_connection, sets)
  }

  public override suspend fun insertCrossRefs(refs: List<SetQuestionCrossRef>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSetQuestionCrossRef.insert(_connection, refs)
  }

  public override suspend fun updateStudyCollection(collection: StudyCollection): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfStudyCollection.handle(_connection, collection)
  }

  public override fun getAllStudyCollections(): Flow<List<StudyCollection>> {
    val _sql: String = "SELECT * FROM StudyCollections"
    return createFlow(__db, false, arrayOf("StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _columnIndexOfIsShared: Int = getColumnIndexOrThrow(_stmt, "is_shared")
        val _columnIndexOfSharedWithGroupId: Int = getColumnIndexOrThrow(_stmt, "shared_with_group_id")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: MutableList<StudyCollection> = mutableListOf()
        while (_stmt.step()) {
          val _item: StudyCollection
          val _tmpCollectionId: String
          _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          val _tmpIsShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsShared).toInt()
          _tmpIsShared = _tmp_1 != 0
          val _tmpSharedWithGroupId: String?
          if (_stmt.isNull(_columnIndexOfSharedWithGroupId)) {
            _tmpSharedWithGroupId = null
          } else {
            _tmpSharedWithGroupId = _stmt.getText(_columnIndexOfSharedWithGroupId)
          }
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = StudyCollection(_tmpCollectionId,_tmpName,_tmpDescription,_tmpIsUserCreated,_tmpIsShared,_tmpSharedWithGroupId,_tmpIsAdminOnly,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>> {
    val _sql: String = """
        |
        |        SELECT *, (SELECT COUNT(*) FROM Questions WHERE master_category = name) as questionCount 
        |        FROM StudyCollections
        |    
        """.trimMargin()
    return createFlow(__db, true, arrayOf("Questions", "StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _columnIndexOfIsShared: Int = getColumnIndexOrThrow(_stmt, "is_shared")
        val _columnIndexOfSharedWithGroupId: Int = getColumnIndexOrThrow(_stmt, "shared_with_group_id")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _columnIndexOfQuestionCount: Int = getColumnIndexOrThrow(_stmt, "questionCount")
        val _result: MutableList<StudyCollectionWithCount> = mutableListOf()
        while (_stmt.step()) {
          val _item: StudyCollectionWithCount
          val _tmpQuestionCount: Int
          _tmpQuestionCount = _stmt.getLong(_columnIndexOfQuestionCount).toInt()
          val _tmpCollection: StudyCollection
          val _tmpCollectionId: String
          _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          val _tmpIsShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsShared).toInt()
          _tmpIsShared = _tmp_1 != 0
          val _tmpSharedWithGroupId: String?
          if (_stmt.isNull(_columnIndexOfSharedWithGroupId)) {
            _tmpSharedWithGroupId = null
          } else {
            _tmpSharedWithGroupId = _stmt.getText(_columnIndexOfSharedWithGroupId)
          }
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _tmpCollection = StudyCollection(_tmpCollectionId,_tmpName,_tmpDescription,_tmpIsUserCreated,_tmpIsShared,_tmpSharedWithGroupId,_tmpIsAdminOnly,_tmpUpdatedAt)
          _item = StudyCollectionWithCount(_tmpCollection,_tmpQuestionCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStudyCollectionCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM StudyCollections"
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

  public override fun getStudyCollectionById(collectionId: String): Flow<StudyCollection?> {
    val _sql: String = "SELECT * FROM StudyCollections WHERE collection_id = ?"
    return createFlow(__db, false, arrayOf("StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _columnIndexOfIsShared: Int = getColumnIndexOrThrow(_stmt, "is_shared")
        val _columnIndexOfSharedWithGroupId: Int = getColumnIndexOrThrow(_stmt, "shared_with_group_id")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: StudyCollection?
        if (_stmt.step()) {
          val _tmpCollectionId: String
          _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          val _tmpIsShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsShared).toInt()
          _tmpIsShared = _tmp_1 != 0
          val _tmpSharedWithGroupId: String?
          if (_stmt.isNull(_columnIndexOfSharedWithGroupId)) {
            _tmpSharedWithGroupId = null
          } else {
            _tmpSharedWithGroupId = _stmt.getText(_columnIndexOfSharedWithGroupId)
          }
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = StudyCollection(_tmpCollectionId,_tmpName,_tmpDescription,_tmpIsUserCreated,_tmpIsShared,_tmpSharedWithGroupId,_tmpIsAdminOnly,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStudyCollectionByIdOnce(collectionId: String): StudyCollection? {
    val _sql: String = "SELECT * FROM StudyCollections WHERE collection_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _columnIndexOfIsShared: Int = getColumnIndexOrThrow(_stmt, "is_shared")
        val _columnIndexOfSharedWithGroupId: Int = getColumnIndexOrThrow(_stmt, "shared_with_group_id")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: StudyCollection?
        if (_stmt.step()) {
          val _tmpCollectionId: String
          _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          val _tmpIsShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsShared).toInt()
          _tmpIsShared = _tmp_1 != 0
          val _tmpSharedWithGroupId: String?
          if (_stmt.isNull(_columnIndexOfSharedWithGroupId)) {
            _tmpSharedWithGroupId = null
          } else {
            _tmpSharedWithGroupId = _stmt.getText(_columnIndexOfSharedWithGroupId)
          }
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = StudyCollection(_tmpCollectionId,_tmpName,_tmpDescription,_tmpIsUserCreated,_tmpIsShared,_tmpSharedWithGroupId,_tmpIsAdminOnly,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStudyCollectionByNameOnce(name: String): StudyCollection? {
    val _sql: String = "SELECT * FROM StudyCollections WHERE name = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, name)
        val _columnIndexOfCollectionId: Int = getColumnIndexOrThrow(_stmt, "collection_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfIsUserCreated: Int = getColumnIndexOrThrow(_stmt, "is_user_created")
        val _columnIndexOfIsShared: Int = getColumnIndexOrThrow(_stmt, "is_shared")
        val _columnIndexOfSharedWithGroupId: Int = getColumnIndexOrThrow(_stmt, "shared_with_group_id")
        val _columnIndexOfIsAdminOnly: Int = getColumnIndexOrThrow(_stmt, "is_admin_only")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: StudyCollection?
        if (_stmt.step()) {
          val _tmpCollectionId: String
          _tmpCollectionId = _stmt.getText(_columnIndexOfCollectionId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpIsUserCreated: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsUserCreated).toInt()
          _tmpIsUserCreated = _tmp != 0
          val _tmpIsShared: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsShared).toInt()
          _tmpIsShared = _tmp_1 != 0
          val _tmpSharedWithGroupId: String?
          if (_stmt.isNull(_columnIndexOfSharedWithGroupId)) {
            _tmpSharedWithGroupId = null
          } else {
            _tmpSharedWithGroupId = _stmt.getText(_columnIndexOfSharedWithGroupId)
          }
          val _tmpIsAdminOnly: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsAdminOnly).toInt()
          _tmpIsAdminOnly = _tmp_2 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = StudyCollection(_tmpCollectionId,_tmpName,_tmpDescription,_tmpIsUserCreated,_tmpIsShared,_tmpSharedWithGroupId,_tmpIsAdminOnly,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getSetsByStudyCollectionId(collectionId: String): Flow<List<QuestionSet>> {
    val _sql: String = "SELECT * FROM Question_Sets WHERE parent_collection_id = ?"
    return createFlow(__db, false, arrayOf("Question_Sets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
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

  public override fun getSetsByStudyCollectionName(collectionName: String): Flow<List<QuestionSet>> {
    val _sql: String = "SELECT * FROM Question_Sets WHERE parent_collection_id = (SELECT collection_id FROM StudyCollections WHERE name = ? LIMIT 1)"
    return createFlow(__db, false, arrayOf("Question_Sets", "StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionName)
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

  public override fun getCategoriesByStudyCollectionId(collectionId: String): Flow<List<String>> {
    val _sql: String = """
        |
        |        SELECT DISTINCT q.category FROM Questions q
        |        INNER JOIN StudyCollections c ON q.master_category = c.name
        |        WHERE c.collection_id = ?
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("Questions", "StudyCollections")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getQuestionsForSet(setId: String): Flow<List<Question>> {
    val _sql: String = """
        |
        |        SELECT q.* FROM Questions q
        |        INNER JOIN Set_Questions_CrossRef x ON q.question_id = x.question_id
        |        WHERE x.set_id = ?
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("Questions", "Set_Questions_CrossRef")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, setId)
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

  public override suspend fun getSetsByStudyCollectionIdOnce(collectionId: String): List<QuestionSet> {
    val _sql: String = "SELECT * FROM Question_Sets WHERE parent_collection_id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, collectionId)
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

  public override suspend fun getQuestionsForSetOnce(setId: String): List<Question> {
    val _sql: String = """
        |
        |        SELECT q.* FROM Questions q
        |        INNER JOIN Set_Questions_CrossRef x ON q.question_id = x.question_id
        |        WHERE x.set_id = ?
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, setId)
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

  public override suspend fun getCrossRefsForSetsBatch(setIds: List<String>): List<SetQuestionCrossRef> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM Set_Questions_CrossRef WHERE set_id IN (")
    val _inputSize: Int = setIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in setIds) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfMappingId: Int = getColumnIndexOrThrow(_stmt, "mapping_id")
        val _columnIndexOfSetId: Int = getColumnIndexOrThrow(_stmt, "set_id")
        val _columnIndexOfQuestionId: Int = getColumnIndexOrThrow(_stmt, "question_id")
        val _result: MutableList<SetQuestionCrossRef> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SetQuestionCrossRef
          val _tmpMappingId: Long
          _tmpMappingId = _stmt.getLong(_columnIndexOfMappingId)
          val _tmpSetId: String
          _tmpSetId = _stmt.getText(_columnIndexOfSetId)
          val _tmpQuestionId: String
          _tmpQuestionId = _stmt.getText(_columnIndexOfQuestionId)
          _item_1 = SetQuestionCrossRef(_tmpMappingId,_tmpSetId,_tmpQuestionId)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStudyCollectionTimestamp(collectionId: String, timestamp: Long) {
    val _sql: String = "UPDATE StudyCollections SET updated_at = ? WHERE collection_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 2
        _stmt.bindText(_argIndex, collectionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteStudyCollectionById(collectionId: String) {
    val _sql: String = "DELETE FROM StudyCollections WHERE collection_id = ?"
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

  public override suspend fun deleteAllStudyCollections() {
    val _sql: String = "DELETE FROM StudyCollections"
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
