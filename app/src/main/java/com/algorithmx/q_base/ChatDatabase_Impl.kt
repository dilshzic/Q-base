package com.algorithmx.q_base.`data`.chat

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
public class ChatDatabase_Impl : ChatDatabase() {
  private val _chatDao: Lazy<ChatDao> = lazy {
    ChatDao_Impl(this)
  }

  private val _messageDao: Lazy<MessageDao> = lazy {
    MessageDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "7a0e1b5cde2046174d36541ee8f65da9", "cf902ad5f565c3773eff9a5ed328f91a") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`chatId` TEXT NOT NULL, `chatName` TEXT, `isGroup` INTEGER NOT NULL, `participantIds` TEXT NOT NULL, `adminIds` TEXT NOT NULL, `isBlocked` INTEGER NOT NULL, `isReported` INTEGER NOT NULL, `isMuted` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL, `lastUsedKeyFingerprint` TEXT, PRIMARY KEY(`chatId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`messageId` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `payload` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `decryptionStatus` TEXT NOT NULL, `status` TEXT NOT NULL, `keyFingerprint` TEXT, `wrappedKey` TEXT, PRIMARY KEY(`messageId`), FOREIGN KEY(`chatId`) REFERENCES `chats`(`chatId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId` ON `messages` (`chatId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7a0e1b5cde2046174d36541ee8f65da9')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `chats`")
        connection.execSQL("DROP TABLE IF EXISTS `messages`")
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
        val _columnsChats: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChats.put("chatId", TableInfo.Column("chatId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("chatName", TableInfo.Column("chatName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isGroup", TableInfo.Column("isGroup", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("participantIds", TableInfo.Column("participantIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("adminIds", TableInfo.Column("adminIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isBlocked", TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isReported", TableInfo.Column("isReported", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("isMuted", TableInfo.Column("isMuted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("unreadCount", TableInfo.Column("unreadCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChats.put("lastUsedKeyFingerprint", TableInfo.Column("lastUsedKeyFingerprint", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChats: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesChats: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoChats: TableInfo = TableInfo("chats", _columnsChats, _foreignKeysChats, _indicesChats)
        val _existingChats: TableInfo = read(connection, "chats")
        if (!_infoChats.equals(_existingChats)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |chats(com.algorithmx.q_base.data.chat.ChatEntity).
              | Expected:
              |""".trimMargin() + _infoChats + """
              |
              | Found:
              |""".trimMargin() + _existingChats)
        }
        val _columnsMessages: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMessages.put("messageId", TableInfo.Column("messageId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("chatId", TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("senderId", TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("payload", TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("decryptionStatus", TableInfo.Column("decryptionStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("keyFingerprint", TableInfo.Column("keyFingerprint", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMessages.put("wrappedKey", TableInfo.Column("wrappedKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMessages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysMessages.add(TableInfo.ForeignKey("chats", "CASCADE", "NO ACTION", listOf("chatId"), listOf("chatId")))
        val _indicesMessages: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesMessages.add(TableInfo.Index("index_messages_chatId", false, listOf("chatId"), listOf("ASC")))
        val _infoMessages: TableInfo = TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages)
        val _existingMessages: TableInfo = read(connection, "messages")
        if (!_infoMessages.equals(_existingMessages)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |messages(com.algorithmx.q_base.data.chat.MessageEntity).
              | Expected:
              |""".trimMargin() + _infoMessages + """
              |
              | Found:
              |""".trimMargin() + _existingMessages)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "chats", "messages")
  }

  public override fun clearAllTables() {
    super.performClear(true, "chats", "messages")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ChatDao::class, ChatDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MessageDao::class, MessageDao_Impl.getRequiredConverters())
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

  public override fun chatDao(): ChatDao = _chatDao.value

  public override fun messageDao(): MessageDao = _messageDao.value
}
