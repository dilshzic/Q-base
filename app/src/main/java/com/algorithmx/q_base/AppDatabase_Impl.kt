package com.algorithmx.q_base.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.algorithmx.q_base.`data`.ai.AiResponseDao
import com.algorithmx.q_base.`data`.ai.AiResponseDao_Impl
import com.algorithmx.q_base.`data`.ai.BrainUsageDao
import com.algorithmx.q_base.`data`.ai.BrainUsageDao_Impl
import com.algorithmx.q_base.`data`.collections.CollectionDao
import com.algorithmx.q_base.`data`.collections.CollectionDao_Impl
import com.algorithmx.q_base.`data`.collections.CollectionVersionLedgerDao
import com.algorithmx.q_base.`data`.collections.CollectionVersionLedgerDao_Impl
import com.algorithmx.q_base.`data`.collections.ProblemReportDao
import com.algorithmx.q_base.`data`.collections.ProblemReportDao_Impl
import com.algorithmx.q_base.`data`.collections.QuestionDao
import com.algorithmx.q_base.`data`.collections.QuestionDao_Impl
import com.algorithmx.q_base.`data`.core.UserDao
import com.algorithmx.q_base.`data`.core.UserDao_Impl
import com.algorithmx.q_base.`data`.sessions.SessionDao
import com.algorithmx.q_base.`data`.sessions.SessionDao_Impl
import com.algorithmx.q_base.`data`.sync.ActionQueueDao
import com.algorithmx.q_base.`data`.sync.ActionQueueDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _questionDao: Lazy<QuestionDao> = lazy {
    QuestionDao_Impl(this)
  }

  private val _collectionDao: Lazy<CollectionDao> = lazy {
    CollectionDao_Impl(this)
  }

  private val _sessionDao: Lazy<SessionDao> = lazy {
    SessionDao_Impl(this)
  }

  private val _problemReportDao: Lazy<ProblemReportDao> = lazy {
    ProblemReportDao_Impl(this)
  }

  private val _userDao: Lazy<UserDao> = lazy {
    UserDao_Impl(this)
  }

  private val _aiResponseDao: Lazy<AiResponseDao> = lazy {
    AiResponseDao_Impl(this)
  }

  private val _brainUsageDao: Lazy<BrainUsageDao> = lazy {
    BrainUsageDao_Impl(this)
  }

  private val _collectionVersionLedgerDao: Lazy<CollectionVersionLedgerDao> = lazy {
    CollectionVersionLedgerDao_Impl(this)
  }

  private val _actionQueueDao: Lazy<ActionQueueDao> = lazy {
    ActionQueueDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(28, "c934006d6c171ab975ec50dcb75da23b", "e687c147555ad84c95ed26974acd2d66") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `StudyCollections` (`collection_id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `is_user_created` INTEGER NOT NULL, `is_shared` INTEGER NOT NULL, `shared_with_group_id` TEXT, `is_admin_only` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`collection_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Questions` (`question_id` TEXT NOT NULL, `master_category` TEXT, `category` TEXT, `tags` TEXT, `question_type` TEXT, `stem` TEXT NOT NULL, `is_pinned` INTEGER NOT NULL, PRIMARY KEY(`question_id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Questions_master_category` ON `Questions` (`master_category`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Questions_category` ON `Questions` (`category`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Questions_tags` ON `Questions` (`tags`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Question_Options` (`question_id` TEXT NOT NULL, `option_letter` TEXT NOT NULL, `option_text` TEXT, `option_explanation` TEXT, PRIMARY KEY(`question_id`, `option_letter`), FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Question_Options_question_id` ON `Question_Options` (`question_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Answers` (`question_id` TEXT NOT NULL, `correct_answer_string` TEXT, `general_explanation` TEXT, `references` TEXT, PRIMARY KEY(`question_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Question_Sets` (`set_id` TEXT NOT NULL, `title` TEXT NOT NULL, `parent_collection_id` TEXT NOT NULL, `description` TEXT, `created_timestamp` INTEGER NOT NULL, `is_user_created` INTEGER NOT NULL, PRIMARY KEY(`set_id`), FOREIGN KEY(`parent_collection_id`) REFERENCES `StudyCollections`(`collection_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Question_Sets_parent_collection_id` ON `Question_Sets` (`parent_collection_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Set_Questions_CrossRef` (`mapping_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `set_id` TEXT NOT NULL, `question_id` TEXT NOT NULL, FOREIGN KEY(`set_id`) REFERENCES `Question_Sets`(`set_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Set_Questions_CrossRef_set_id` ON `Set_Questions_CrossRef` (`set_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Set_Questions_CrossRef_question_id` ON `Set_Questions_CrossRef` (`question_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Study_Sessions` (`session_id` TEXT NOT NULL, `title` TEXT NOT NULL, `time_limit_seconds` INTEGER, `score_achieved` REAL NOT NULL, `created_timestamp` INTEGER NOT NULL, `is_completed` INTEGER NOT NULL, `timing_type` TEXT NOT NULL, `is_random` INTEGER NOT NULL, `collection_id` TEXT, `last_question_index` INTEGER NOT NULL, `is_admin_only` INTEGER NOT NULL, PRIMARY KEY(`session_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Session_Attempts` (`attempt_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `session_id` TEXT NOT NULL, `question_id` TEXT NOT NULL, `attempt_status` TEXT NOT NULL, `user_selected_answers` TEXT NOT NULL, `time_spent_seconds` INTEGER NOT NULL, `marks_obtained` REAL NOT NULL, FOREIGN KEY(`session_id`) REFERENCES `Study_Sessions`(`session_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Session_Attempts_session_id` ON `Session_Attempts` (`session_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Session_Attempts_question_id` ON `Session_Attempts` (`question_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Problem_Reports` (`report_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `question_id` TEXT NOT NULL, `explanation` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Problem_Reports_question_id` ON `Problem_Reports` (`question_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `users` (`userId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `email` TEXT, `intro` TEXT, `profilePictureUrl` TEXT, `friendCode` TEXT NOT NULL, `publicKey` TEXT, `isBanned` INTEGER NOT NULL, `isPhotoVisible` INTEGER NOT NULL, PRIMARY KEY(`userId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Ai_Responses` (`response_id` TEXT NOT NULL, `topic` TEXT NOT NULL, `raw_json` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `is_promoted` INTEGER NOT NULL, PRIMARY KEY(`response_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `brain_usage_history` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `timestampMs` INTEGER NOT NULL, `provider` TEXT NOT NULL, `modelUsed` TEXT NOT NULL, `tokensEstimated` INTEGER NOT NULL, `isSuccess` INTEGER NOT NULL, `errorMessage` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `collection_version_ledger` (`collectionId` TEXT NOT NULL, `currentRevisionId` INTEGER NOT NULL, `lastAppliedTimestamp` INTEGER NOT NULL, PRIMARY KEY(`collectionId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `Offline_Actions` (`actionId` TEXT NOT NULL, `actionType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL, PRIMARY KEY(`actionId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c934006d6c171ab975ec50dcb75da23b')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `StudyCollections`")
        connection.execSQL("DROP TABLE IF EXISTS `Questions`")
        connection.execSQL("DROP TABLE IF EXISTS `Question_Options`")
        connection.execSQL("DROP TABLE IF EXISTS `Answers`")
        connection.execSQL("DROP TABLE IF EXISTS `Question_Sets`")
        connection.execSQL("DROP TABLE IF EXISTS `Set_Questions_CrossRef`")
        connection.execSQL("DROP TABLE IF EXISTS `Study_Sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `Session_Attempts`")
        connection.execSQL("DROP TABLE IF EXISTS `Problem_Reports`")
        connection.execSQL("DROP TABLE IF EXISTS `users`")
        connection.execSQL("DROP TABLE IF EXISTS `Ai_Responses`")
        connection.execSQL("DROP TABLE IF EXISTS `brain_usage_history`")
        connection.execSQL("DROP TABLE IF EXISTS `collection_version_ledger`")
        connection.execSQL("DROP TABLE IF EXISTS `Offline_Actions`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsStudyCollections: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStudyCollections.put("collection_id", TableInfo.Column("collection_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("description", TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("is_user_created", TableInfo.Column("is_user_created", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("is_shared", TableInfo.Column("is_shared", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("shared_with_group_id", TableInfo.Column("shared_with_group_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("is_admin_only", TableInfo.Column("is_admin_only", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudyCollections.put("updated_at", TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStudyCollections: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStudyCollections: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStudyCollections: TableInfo = TableInfo("StudyCollections", _columnsStudyCollections, _foreignKeysStudyCollections, _indicesStudyCollections)
        val _existingStudyCollections: TableInfo = read(connection, "StudyCollections")
        if (!_infoStudyCollections.equals(_existingStudyCollections)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |StudyCollections(com.algorithmx.q_base.data.collections.StudyCollection).
              | Expected:
              |""".trimMargin() + _infoStudyCollections + """
              |
              | Found:
              |""".trimMargin() + _existingStudyCollections)
        }
        val _columnsQuestions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsQuestions.put("question_id", TableInfo.Column("question_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestions.put("master_category", TableInfo.Column("master_category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestions.put("category", TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestions.put("tags", TableInfo.Column("tags", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestions.put("question_type", TableInfo.Column("question_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestions.put("stem", TableInfo.Column("stem", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestions.put("is_pinned", TableInfo.Column("is_pinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysQuestions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesQuestions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesQuestions.add(TableInfo.Index("index_Questions_master_category", false, listOf("master_category"), listOf("ASC")))
        _indicesQuestions.add(TableInfo.Index("index_Questions_category", false, listOf("category"), listOf("ASC")))
        _indicesQuestions.add(TableInfo.Index("index_Questions_tags", false, listOf("tags"), listOf("ASC")))
        val _infoQuestions: TableInfo = TableInfo("Questions", _columnsQuestions, _foreignKeysQuestions, _indicesQuestions)
        val _existingQuestions: TableInfo = read(connection, "Questions")
        if (!_infoQuestions.equals(_existingQuestions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Questions(com.algorithmx.q_base.data.collections.Question).
              | Expected:
              |""".trimMargin() + _infoQuestions + """
              |
              | Found:
              |""".trimMargin() + _existingQuestions)
        }
        val _columnsQuestionOptions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsQuestionOptions.put("question_id", TableInfo.Column("question_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionOptions.put("option_letter", TableInfo.Column("option_letter", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionOptions.put("option_text", TableInfo.Column("option_text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionOptions.put("option_explanation", TableInfo.Column("option_explanation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysQuestionOptions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysQuestionOptions.add(TableInfo.ForeignKey("Questions", "CASCADE", "NO ACTION", listOf("question_id"), listOf("question_id")))
        val _indicesQuestionOptions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesQuestionOptions.add(TableInfo.Index("index_Question_Options_question_id", false, listOf("question_id"), listOf("ASC")))
        val _infoQuestionOptions: TableInfo = TableInfo("Question_Options", _columnsQuestionOptions, _foreignKeysQuestionOptions, _indicesQuestionOptions)
        val _existingQuestionOptions: TableInfo = read(connection, "Question_Options")
        if (!_infoQuestionOptions.equals(_existingQuestionOptions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Question_Options(com.algorithmx.q_base.data.collections.QuestionOption).
              | Expected:
              |""".trimMargin() + _infoQuestionOptions + """
              |
              | Found:
              |""".trimMargin() + _existingQuestionOptions)
        }
        val _columnsAnswers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAnswers.put("question_id", TableInfo.Column("question_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAnswers.put("correct_answer_string", TableInfo.Column("correct_answer_string", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAnswers.put("general_explanation", TableInfo.Column("general_explanation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAnswers.put("references", TableInfo.Column("references", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAnswers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAnswers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAnswers: TableInfo = TableInfo("Answers", _columnsAnswers, _foreignKeysAnswers, _indicesAnswers)
        val _existingAnswers: TableInfo = read(connection, "Answers")
        if (!_infoAnswers.equals(_existingAnswers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Answers(com.algorithmx.q_base.data.collections.Answer).
              | Expected:
              |""".trimMargin() + _infoAnswers + """
              |
              | Found:
              |""".trimMargin() + _existingAnswers)
        }
        val _columnsQuestionSets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsQuestionSets.put("set_id", TableInfo.Column("set_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionSets.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionSets.put("parent_collection_id", TableInfo.Column("parent_collection_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionSets.put("description", TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionSets.put("created_timestamp", TableInfo.Column("created_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestionSets.put("is_user_created", TableInfo.Column("is_user_created", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysQuestionSets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysQuestionSets.add(TableInfo.ForeignKey("StudyCollections", "CASCADE", "NO ACTION", listOf("parent_collection_id"), listOf("collection_id")))
        val _indicesQuestionSets: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesQuestionSets.add(TableInfo.Index("index_Question_Sets_parent_collection_id", false, listOf("parent_collection_id"), listOf("ASC")))
        val _infoQuestionSets: TableInfo = TableInfo("Question_Sets", _columnsQuestionSets, _foreignKeysQuestionSets, _indicesQuestionSets)
        val _existingQuestionSets: TableInfo = read(connection, "Question_Sets")
        if (!_infoQuestionSets.equals(_existingQuestionSets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Question_Sets(com.algorithmx.q_base.data.collections.QuestionSet).
              | Expected:
              |""".trimMargin() + _infoQuestionSets + """
              |
              | Found:
              |""".trimMargin() + _existingQuestionSets)
        }
        val _columnsSetQuestionsCrossRef: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSetQuestionsCrossRef.put("mapping_id", TableInfo.Column("mapping_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSetQuestionsCrossRef.put("set_id", TableInfo.Column("set_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSetQuestionsCrossRef.put("question_id", TableInfo.Column("question_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSetQuestionsCrossRef: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSetQuestionsCrossRef.add(TableInfo.ForeignKey("Question_Sets", "CASCADE", "NO ACTION", listOf("set_id"), listOf("set_id")))
        _foreignKeysSetQuestionsCrossRef.add(TableInfo.ForeignKey("Questions", "CASCADE", "NO ACTION", listOf("question_id"), listOf("question_id")))
        val _indicesSetQuestionsCrossRef: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesSetQuestionsCrossRef.add(TableInfo.Index("index_Set_Questions_CrossRef_set_id", false, listOf("set_id"), listOf("ASC")))
        _indicesSetQuestionsCrossRef.add(TableInfo.Index("index_Set_Questions_CrossRef_question_id", false, listOf("question_id"), listOf("ASC")))
        val _infoSetQuestionsCrossRef: TableInfo = TableInfo("Set_Questions_CrossRef", _columnsSetQuestionsCrossRef, _foreignKeysSetQuestionsCrossRef, _indicesSetQuestionsCrossRef)
        val _existingSetQuestionsCrossRef: TableInfo = read(connection, "Set_Questions_CrossRef")
        if (!_infoSetQuestionsCrossRef.equals(_existingSetQuestionsCrossRef)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Set_Questions_CrossRef(com.algorithmx.q_base.data.collections.SetQuestionCrossRef).
              | Expected:
              |""".trimMargin() + _infoSetQuestionsCrossRef + """
              |
              | Found:
              |""".trimMargin() + _existingSetQuestionsCrossRef)
        }
        val _columnsStudySessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStudySessions.put("session_id", TableInfo.Column("session_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("time_limit_seconds", TableInfo.Column("time_limit_seconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("score_achieved", TableInfo.Column("score_achieved", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("created_timestamp", TableInfo.Column("created_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("is_completed", TableInfo.Column("is_completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("timing_type", TableInfo.Column("timing_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("is_random", TableInfo.Column("is_random", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("collection_id", TableInfo.Column("collection_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("last_question_index", TableInfo.Column("last_question_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStudySessions.put("is_admin_only", TableInfo.Column("is_admin_only", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStudySessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStudySessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStudySessions: TableInfo = TableInfo("Study_Sessions", _columnsStudySessions, _foreignKeysStudySessions, _indicesStudySessions)
        val _existingStudySessions: TableInfo = read(connection, "Study_Sessions")
        if (!_infoStudySessions.equals(_existingStudySessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Study_Sessions(com.algorithmx.q_base.data.sessions.StudySession).
              | Expected:
              |""".trimMargin() + _infoStudySessions + """
              |
              | Found:
              |""".trimMargin() + _existingStudySessions)
        }
        val _columnsSessionAttempts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSessionAttempts.put("attempt_id", TableInfo.Column("attempt_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessionAttempts.put("session_id", TableInfo.Column("session_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessionAttempts.put("question_id", TableInfo.Column("question_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessionAttempts.put("attempt_status", TableInfo.Column("attempt_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessionAttempts.put("user_selected_answers", TableInfo.Column("user_selected_answers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessionAttempts.put("time_spent_seconds", TableInfo.Column("time_spent_seconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessionAttempts.put("marks_obtained", TableInfo.Column("marks_obtained", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSessionAttempts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSessionAttempts.add(TableInfo.ForeignKey("Study_Sessions", "CASCADE", "NO ACTION", listOf("session_id"), listOf("session_id")))
        _foreignKeysSessionAttempts.add(TableInfo.ForeignKey("Questions", "CASCADE", "NO ACTION", listOf("question_id"), listOf("question_id")))
        val _indicesSessionAttempts: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesSessionAttempts.add(TableInfo.Index("index_Session_Attempts_session_id", false, listOf("session_id"), listOf("ASC")))
        _indicesSessionAttempts.add(TableInfo.Index("index_Session_Attempts_question_id", false, listOf("question_id"), listOf("ASC")))
        val _infoSessionAttempts: TableInfo = TableInfo("Session_Attempts", _columnsSessionAttempts, _foreignKeysSessionAttempts, _indicesSessionAttempts)
        val _existingSessionAttempts: TableInfo = read(connection, "Session_Attempts")
        if (!_infoSessionAttempts.equals(_existingSessionAttempts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Session_Attempts(com.algorithmx.q_base.data.sessions.SessionAttempt).
              | Expected:
              |""".trimMargin() + _infoSessionAttempts + """
              |
              | Found:
              |""".trimMargin() + _existingSessionAttempts)
        }
        val _columnsProblemReports: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProblemReports.put("report_id", TableInfo.Column("report_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProblemReports.put("question_id", TableInfo.Column("question_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProblemReports.put("explanation", TableInfo.Column("explanation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProblemReports.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProblemReports: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysProblemReports.add(TableInfo.ForeignKey("Questions", "CASCADE", "NO ACTION", listOf("question_id"), listOf("question_id")))
        val _indicesProblemReports: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesProblemReports.add(TableInfo.Index("index_Problem_Reports_question_id", false, listOf("question_id"), listOf("ASC")))
        val _infoProblemReports: TableInfo = TableInfo("Problem_Reports", _columnsProblemReports, _foreignKeysProblemReports, _indicesProblemReports)
        val _existingProblemReports: TableInfo = read(connection, "Problem_Reports")
        if (!_infoProblemReports.equals(_existingProblemReports)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Problem_Reports(com.algorithmx.q_base.data.collections.ProblemReport).
              | Expected:
              |""".trimMargin() + _infoProblemReports + """
              |
              | Found:
              |""".trimMargin() + _existingProblemReports)
        }
        val _columnsUsers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUsers.put("userId", TableInfo.Column("userId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("displayName", TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("email", TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("intro", TableInfo.Column("intro", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("profilePictureUrl", TableInfo.Column("profilePictureUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("friendCode", TableInfo.Column("friendCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("publicKey", TableInfo.Column("publicKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("isBanned", TableInfo.Column("isBanned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("isPhotoVisible", TableInfo.Column("isPhotoVisible", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUsers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoUsers: TableInfo = TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers)
        val _existingUsers: TableInfo = read(connection, "users")
        if (!_infoUsers.equals(_existingUsers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |users(com.algorithmx.q_base.data.core.UserEntity).
              | Expected:
              |""".trimMargin() + _infoUsers + """
              |
              | Found:
              |""".trimMargin() + _existingUsers)
        }
        val _columnsAiResponses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAiResponses.put("response_id", TableInfo.Column("response_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiResponses.put("topic", TableInfo.Column("topic", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiResponses.put("raw_json", TableInfo.Column("raw_json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiResponses.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAiResponses.put("is_promoted", TableInfo.Column("is_promoted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAiResponses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAiResponses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAiResponses: TableInfo = TableInfo("Ai_Responses", _columnsAiResponses, _foreignKeysAiResponses, _indicesAiResponses)
        val _existingAiResponses: TableInfo = read(connection, "Ai_Responses")
        if (!_infoAiResponses.equals(_existingAiResponses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Ai_Responses(com.algorithmx.q_base.data.ai.AiResponseEntity).
              | Expected:
              |""".trimMargin() + _infoAiResponses + """
              |
              | Found:
              |""".trimMargin() + _existingAiResponses)
        }
        val _columnsBrainUsageHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBrainUsageHistory.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("taskId", TableInfo.Column("taskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("timestampMs", TableInfo.Column("timestampMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("provider", TableInfo.Column("provider", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("modelUsed", TableInfo.Column("modelUsed", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("tokensEstimated", TableInfo.Column("tokensEstimated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("isSuccess", TableInfo.Column("isSuccess", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBrainUsageHistory.put("errorMessage", TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBrainUsageHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBrainUsageHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBrainUsageHistory: TableInfo = TableInfo("brain_usage_history", _columnsBrainUsageHistory, _foreignKeysBrainUsageHistory, _indicesBrainUsageHistory)
        val _existingBrainUsageHistory: TableInfo = read(connection, "brain_usage_history")
        if (!_infoBrainUsageHistory.equals(_existingBrainUsageHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |brain_usage_history(com.algorithmx.q_base.data.ai.BrainUsageEntity).
              | Expected:
              |""".trimMargin() + _infoBrainUsageHistory + """
              |
              | Found:
              |""".trimMargin() + _existingBrainUsageHistory)
        }
        val _columnsCollectionVersionLedger: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCollectionVersionLedger.put("collectionId", TableInfo.Column("collectionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCollectionVersionLedger.put("currentRevisionId", TableInfo.Column("currentRevisionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCollectionVersionLedger.put("lastAppliedTimestamp", TableInfo.Column("lastAppliedTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCollectionVersionLedger: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCollectionVersionLedger: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCollectionVersionLedger: TableInfo = TableInfo("collection_version_ledger", _columnsCollectionVersionLedger, _foreignKeysCollectionVersionLedger, _indicesCollectionVersionLedger)
        val _existingCollectionVersionLedger: TableInfo = read(connection, "collection_version_ledger")
        if (!_infoCollectionVersionLedger.equals(_existingCollectionVersionLedger)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |collection_version_ledger(com.algorithmx.q_base.data.collections.CollectionVersionLedgerEntity).
              | Expected:
              |""".trimMargin() + _infoCollectionVersionLedger + """
              |
              | Found:
              |""".trimMargin() + _existingCollectionVersionLedger)
        }
        val _columnsOfflineActions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsOfflineActions.put("actionId", TableInfo.Column("actionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineActions.put("actionType", TableInfo.Column("actionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineActions.put("payloadJson", TableInfo.Column("payloadJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineActions.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineActions.put("retryCount", TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysOfflineActions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesOfflineActions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoOfflineActions: TableInfo = TableInfo("Offline_Actions", _columnsOfflineActions, _foreignKeysOfflineActions, _indicesOfflineActions)
        val _existingOfflineActions: TableInfo = read(connection, "Offline_Actions")
        if (!_infoOfflineActions.equals(_existingOfflineActions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |Offline_Actions(com.algorithmx.q_base.data.sync.OfflineActionEntity).
              | Expected:
              |""".trimMargin() + _infoOfflineActions + """
              |
              | Found:
              |""".trimMargin() + _existingOfflineActions)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "StudyCollections", "Questions", "Question_Options", "Answers", "Question_Sets", "Set_Questions_CrossRef", "Study_Sessions", "Session_Attempts", "Problem_Reports", "users", "Ai_Responses", "brain_usage_history", "collection_version_ledger", "Offline_Actions")
  }

  public override fun clearAllTables() {
    super.performClear(true, "StudyCollections", "Questions", "Question_Options", "Answers", "Question_Sets", "Set_Questions_CrossRef", "Study_Sessions", "Session_Attempts", "Problem_Reports", "users", "Ai_Responses", "brain_usage_history", "collection_version_ledger", "Offline_Actions")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(QuestionDao::class, QuestionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CollectionDao::class, CollectionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SessionDao::class, SessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ProblemReportDao::class, ProblemReportDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(UserDao::class, UserDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AiResponseDao::class, AiResponseDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BrainUsageDao::class, BrainUsageDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CollectionVersionLedgerDao::class, CollectionVersionLedgerDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ActionQueueDao::class, ActionQueueDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun questionDao(): QuestionDao = _questionDao.value

  public override fun collectionDao(): CollectionDao = _collectionDao.value

  public override fun sessionDao(): SessionDao = _sessionDao.value

  public override fun problemReportDao(): ProblemReportDao = _problemReportDao.value

  public override fun userDao(): UserDao = _userDao.value

  public override fun aiResponseDao(): AiResponseDao = _aiResponseDao.value

  public override fun brainUsageDao(): BrainUsageDao = _brainUsageDao.value

  public override fun collectionVersionLedgerDao(): CollectionVersionLedgerDao = _collectionVersionLedgerDao.value

  public override fun actionQueueDao(): ActionQueueDao = _actionQueueDao.value
}
