package com.algorithmx.q_base.`data`.chat

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
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
public class ChatDao_Impl(
  __db: RoomDatabase,
) : ChatDao {
  private val __db: RoomDatabase

  private val __deleteAdapterOfChatEntity: EntityDeleteOrUpdateAdapter<ChatEntity>

  private val __upsertAdapterOfChatEntity: EntityUpsertAdapter<ChatEntity>
  init {
    this.__db = __db
    this.__deleteAdapterOfChatEntity = object : EntityDeleteOrUpdateAdapter<ChatEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `chats` WHERE `chatId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChatEntity) {
        statement.bindText(1, entity.chatId)
      }
    }
    this.__upsertAdapterOfChatEntity = EntityUpsertAdapter<ChatEntity>(object : EntityInsertAdapter<ChatEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `chats` (`chatId`,`chatName`,`isGroup`,`participantIds`,`adminIds`,`isBlocked`,`isReported`,`isMuted`,`unreadCount`,`lastUsedKeyFingerprint`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChatEntity) {
        statement.bindText(1, entity.chatId)
        val _tmpChatName: String? = entity.chatName
        if (_tmpChatName == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpChatName)
        }
        val _tmp: Int = if (entity.isGroup) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        statement.bindText(4, entity.participantIds)
        val _tmp_1: String = ChatTypeConverters.fromAdminIds(entity.adminIds)
        statement.bindText(5, _tmp_1)
        val _tmp_2: Int = if (entity.isBlocked) 1 else 0
        statement.bindLong(6, _tmp_2.toLong())
        val _tmp_3: Int = if (entity.isReported) 1 else 0
        statement.bindLong(7, _tmp_3.toLong())
        val _tmp_4: Int = if (entity.isMuted) 1 else 0
        statement.bindLong(8, _tmp_4.toLong())
        statement.bindLong(9, entity.unreadCount.toLong())
        val _tmpLastUsedKeyFingerprint: String? = entity.lastUsedKeyFingerprint
        if (_tmpLastUsedKeyFingerprint == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpLastUsedKeyFingerprint)
        }
      }
    }, object : EntityDeleteOrUpdateAdapter<ChatEntity>() {
      protected override fun createQuery(): String = "UPDATE `chats` SET `chatId` = ?,`chatName` = ?,`isGroup` = ?,`participantIds` = ?,`adminIds` = ?,`isBlocked` = ?,`isReported` = ?,`isMuted` = ?,`unreadCount` = ?,`lastUsedKeyFingerprint` = ? WHERE `chatId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChatEntity) {
        statement.bindText(1, entity.chatId)
        val _tmpChatName: String? = entity.chatName
        if (_tmpChatName == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpChatName)
        }
        val _tmp: Int = if (entity.isGroup) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        statement.bindText(4, entity.participantIds)
        val _tmp_1: String = ChatTypeConverters.fromAdminIds(entity.adminIds)
        statement.bindText(5, _tmp_1)
        val _tmp_2: Int = if (entity.isBlocked) 1 else 0
        statement.bindLong(6, _tmp_2.toLong())
        val _tmp_3: Int = if (entity.isReported) 1 else 0
        statement.bindLong(7, _tmp_3.toLong())
        val _tmp_4: Int = if (entity.isMuted) 1 else 0
        statement.bindLong(8, _tmp_4.toLong())
        statement.bindLong(9, entity.unreadCount.toLong())
        val _tmpLastUsedKeyFingerprint: String? = entity.lastUsedKeyFingerprint
        if (_tmpLastUsedKeyFingerprint == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpLastUsedKeyFingerprint)
        }
        statement.bindText(11, entity.chatId)
      }
    })
  }

  public override suspend fun deleteChat(chat: ChatEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfChatEntity.handle(_connection, chat)
  }

  public override suspend fun insertChat(chat: ChatEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfChatEntity.upsert(_connection, chat)
  }

  public override suspend fun getChatById(chatId: String): ChatEntity? {
    val _sql: String = "SELECT * FROM chats WHERE chatId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfChatName: Int = getColumnIndexOrThrow(_stmt, "chatName")
        val _columnIndexOfIsGroup: Int = getColumnIndexOrThrow(_stmt, "isGroup")
        val _columnIndexOfParticipantIds: Int = getColumnIndexOrThrow(_stmt, "participantIds")
        val _columnIndexOfAdminIds: Int = getColumnIndexOrThrow(_stmt, "adminIds")
        val _columnIndexOfIsBlocked: Int = getColumnIndexOrThrow(_stmt, "isBlocked")
        val _columnIndexOfIsReported: Int = getColumnIndexOrThrow(_stmt, "isReported")
        val _columnIndexOfIsMuted: Int = getColumnIndexOrThrow(_stmt, "isMuted")
        val _columnIndexOfUnreadCount: Int = getColumnIndexOrThrow(_stmt, "unreadCount")
        val _columnIndexOfLastUsedKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "lastUsedKeyFingerprint")
        val _result: ChatEntity?
        if (_stmt.step()) {
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpChatName: String?
          if (_stmt.isNull(_columnIndexOfChatName)) {
            _tmpChatName = null
          } else {
            _tmpChatName = _stmt.getText(_columnIndexOfChatName)
          }
          val _tmpIsGroup: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsGroup).toInt()
          _tmpIsGroup = _tmp != 0
          val _tmpParticipantIds: String
          _tmpParticipantIds = _stmt.getText(_columnIndexOfParticipantIds)
          val _tmpAdminIds: List<String>
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfAdminIds)
          _tmpAdminIds = ChatTypeConverters.toAdminIds(_tmp_1)
          val _tmpIsBlocked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsBlocked).toInt()
          _tmpIsBlocked = _tmp_2 != 0
          val _tmpIsReported: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsReported).toInt()
          _tmpIsReported = _tmp_3 != 0
          val _tmpIsMuted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsMuted).toInt()
          _tmpIsMuted = _tmp_4 != 0
          val _tmpUnreadCount: Int
          _tmpUnreadCount = _stmt.getLong(_columnIndexOfUnreadCount).toInt()
          val _tmpLastUsedKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfLastUsedKeyFingerprint)) {
            _tmpLastUsedKeyFingerprint = null
          } else {
            _tmpLastUsedKeyFingerprint = _stmt.getText(_columnIndexOfLastUsedKeyFingerprint)
          }
          _result = ChatEntity(_tmpChatId,_tmpChatName,_tmpIsGroup,_tmpParticipantIds,_tmpAdminIds,_tmpIsBlocked,_tmpIsReported,_tmpIsMuted,_tmpUnreadCount,_tmpLastUsedKeyFingerprint)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> {
    val _sql: String = "SELECT * FROM chats WHERE chatId = ?"
    return createFlow(__db, false, arrayOf("chats")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfChatName: Int = getColumnIndexOrThrow(_stmt, "chatName")
        val _columnIndexOfIsGroup: Int = getColumnIndexOrThrow(_stmt, "isGroup")
        val _columnIndexOfParticipantIds: Int = getColumnIndexOrThrow(_stmt, "participantIds")
        val _columnIndexOfAdminIds: Int = getColumnIndexOrThrow(_stmt, "adminIds")
        val _columnIndexOfIsBlocked: Int = getColumnIndexOrThrow(_stmt, "isBlocked")
        val _columnIndexOfIsReported: Int = getColumnIndexOrThrow(_stmt, "isReported")
        val _columnIndexOfIsMuted: Int = getColumnIndexOrThrow(_stmt, "isMuted")
        val _columnIndexOfUnreadCount: Int = getColumnIndexOrThrow(_stmt, "unreadCount")
        val _columnIndexOfLastUsedKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "lastUsedKeyFingerprint")
        val _result: ChatEntity?
        if (_stmt.step()) {
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpChatName: String?
          if (_stmt.isNull(_columnIndexOfChatName)) {
            _tmpChatName = null
          } else {
            _tmpChatName = _stmt.getText(_columnIndexOfChatName)
          }
          val _tmpIsGroup: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsGroup).toInt()
          _tmpIsGroup = _tmp != 0
          val _tmpParticipantIds: String
          _tmpParticipantIds = _stmt.getText(_columnIndexOfParticipantIds)
          val _tmpAdminIds: List<String>
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfAdminIds)
          _tmpAdminIds = ChatTypeConverters.toAdminIds(_tmp_1)
          val _tmpIsBlocked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsBlocked).toInt()
          _tmpIsBlocked = _tmp_2 != 0
          val _tmpIsReported: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsReported).toInt()
          _tmpIsReported = _tmp_3 != 0
          val _tmpIsMuted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsMuted).toInt()
          _tmpIsMuted = _tmp_4 != 0
          val _tmpUnreadCount: Int
          _tmpUnreadCount = _stmt.getLong(_columnIndexOfUnreadCount).toInt()
          val _tmpLastUsedKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfLastUsedKeyFingerprint)) {
            _tmpLastUsedKeyFingerprint = null
          } else {
            _tmpLastUsedKeyFingerprint = _stmt.getText(_columnIndexOfLastUsedKeyFingerprint)
          }
          _result = ChatEntity(_tmpChatId,_tmpChatName,_tmpIsGroup,_tmpParticipantIds,_tmpAdminIds,_tmpIsBlocked,_tmpIsReported,_tmpIsMuted,_tmpUnreadCount,_tmpLastUsedKeyFingerprint)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getP2PChat(ids1: String, ids2: String): ChatEntity? {
    val _sql: String = "SELECT * FROM chats WHERE isGroup = 0 AND ((participantIds = ?) OR (participantIds = ?))"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, ids1)
        _argIndex = 2
        _stmt.bindText(_argIndex, ids2)
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfChatName: Int = getColumnIndexOrThrow(_stmt, "chatName")
        val _columnIndexOfIsGroup: Int = getColumnIndexOrThrow(_stmt, "isGroup")
        val _columnIndexOfParticipantIds: Int = getColumnIndexOrThrow(_stmt, "participantIds")
        val _columnIndexOfAdminIds: Int = getColumnIndexOrThrow(_stmt, "adminIds")
        val _columnIndexOfIsBlocked: Int = getColumnIndexOrThrow(_stmt, "isBlocked")
        val _columnIndexOfIsReported: Int = getColumnIndexOrThrow(_stmt, "isReported")
        val _columnIndexOfIsMuted: Int = getColumnIndexOrThrow(_stmt, "isMuted")
        val _columnIndexOfUnreadCount: Int = getColumnIndexOrThrow(_stmt, "unreadCount")
        val _columnIndexOfLastUsedKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "lastUsedKeyFingerprint")
        val _result: ChatEntity?
        if (_stmt.step()) {
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpChatName: String?
          if (_stmt.isNull(_columnIndexOfChatName)) {
            _tmpChatName = null
          } else {
            _tmpChatName = _stmt.getText(_columnIndexOfChatName)
          }
          val _tmpIsGroup: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsGroup).toInt()
          _tmpIsGroup = _tmp != 0
          val _tmpParticipantIds: String
          _tmpParticipantIds = _stmt.getText(_columnIndexOfParticipantIds)
          val _tmpAdminIds: List<String>
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfAdminIds)
          _tmpAdminIds = ChatTypeConverters.toAdminIds(_tmp_1)
          val _tmpIsBlocked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsBlocked).toInt()
          _tmpIsBlocked = _tmp_2 != 0
          val _tmpIsReported: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsReported).toInt()
          _tmpIsReported = _tmp_3 != 0
          val _tmpIsMuted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsMuted).toInt()
          _tmpIsMuted = _tmp_4 != 0
          val _tmpUnreadCount: Int
          _tmpUnreadCount = _stmt.getLong(_columnIndexOfUnreadCount).toInt()
          val _tmpLastUsedKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfLastUsedKeyFingerprint)) {
            _tmpLastUsedKeyFingerprint = null
          } else {
            _tmpLastUsedKeyFingerprint = _stmt.getText(_columnIndexOfLastUsedKeyFingerprint)
          }
          _result = ChatEntity(_tmpChatId,_tmpChatName,_tmpIsGroup,_tmpParticipantIds,_tmpAdminIds,_tmpIsBlocked,_tmpIsReported,_tmpIsMuted,_tmpUnreadCount,_tmpLastUsedKeyFingerprint)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllChats(): Flow<List<ChatEntity>> {
    val _sql: String = "SELECT * FROM chats"
    return createFlow(__db, false, arrayOf("chats")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfChatName: Int = getColumnIndexOrThrow(_stmt, "chatName")
        val _columnIndexOfIsGroup: Int = getColumnIndexOrThrow(_stmt, "isGroup")
        val _columnIndexOfParticipantIds: Int = getColumnIndexOrThrow(_stmt, "participantIds")
        val _columnIndexOfAdminIds: Int = getColumnIndexOrThrow(_stmt, "adminIds")
        val _columnIndexOfIsBlocked: Int = getColumnIndexOrThrow(_stmt, "isBlocked")
        val _columnIndexOfIsReported: Int = getColumnIndexOrThrow(_stmt, "isReported")
        val _columnIndexOfIsMuted: Int = getColumnIndexOrThrow(_stmt, "isMuted")
        val _columnIndexOfUnreadCount: Int = getColumnIndexOrThrow(_stmt, "unreadCount")
        val _columnIndexOfLastUsedKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "lastUsedKeyFingerprint")
        val _result: MutableList<ChatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatEntity
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpChatName: String?
          if (_stmt.isNull(_columnIndexOfChatName)) {
            _tmpChatName = null
          } else {
            _tmpChatName = _stmt.getText(_columnIndexOfChatName)
          }
          val _tmpIsGroup: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsGroup).toInt()
          _tmpIsGroup = _tmp != 0
          val _tmpParticipantIds: String
          _tmpParticipantIds = _stmt.getText(_columnIndexOfParticipantIds)
          val _tmpAdminIds: List<String>
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfAdminIds)
          _tmpAdminIds = ChatTypeConverters.toAdminIds(_tmp_1)
          val _tmpIsBlocked: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsBlocked).toInt()
          _tmpIsBlocked = _tmp_2 != 0
          val _tmpIsReported: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsReported).toInt()
          _tmpIsReported = _tmp_3 != 0
          val _tmpIsMuted: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfIsMuted).toInt()
          _tmpIsMuted = _tmp_4 != 0
          val _tmpUnreadCount: Int
          _tmpUnreadCount = _stmt.getLong(_columnIndexOfUnreadCount).toInt()
          val _tmpLastUsedKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfLastUsedKeyFingerprint)) {
            _tmpLastUsedKeyFingerprint = null
          } else {
            _tmpLastUsedKeyFingerprint = _stmt.getText(_columnIndexOfLastUsedKeyFingerprint)
          }
          _item = ChatEntity(_tmpChatId,_tmpChatName,_tmpIsGroup,_tmpParticipantIds,_tmpAdminIds,_tmpIsBlocked,_tmpIsReported,_tmpIsMuted,_tmpUnreadCount,_tmpLastUsedKeyFingerprint)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getChatSummaries(): Flow<List<ChatSummary>> {
    val _sql: String = """
        |
        |        SELECT 
        |            c.chatId, c.chatName, c.isGroup, c.participantIds, c.unreadCount, c.isBlocked,
        |            m.payload as lastMessagePayload, m.timestamp as lastMessageTimestamp, m.type as lastMessageType
        |        FROM chats c
        |        LEFT JOIN (
        |            SELECT chatId, payload, timestamp, type
        |            FROM messages m1
        |            WHERE timestamp = (SELECT MAX(timestamp) FROM messages m2 WHERE m2.chatId = m1.chatId)
        |        ) m ON c.chatId = m.chatId
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("chats", "messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfChatId: Int = 0
        val _columnIndexOfChatName: Int = 1
        val _columnIndexOfIsGroup: Int = 2
        val _columnIndexOfParticipantIds: Int = 3
        val _columnIndexOfUnreadCount: Int = 4
        val _columnIndexOfIsBlocked: Int = 5
        val _columnIndexOfLastMessagePayload: Int = 6
        val _columnIndexOfLastMessageTimestamp: Int = 7
        val _columnIndexOfLastMessageType: Int = 8
        val _result: MutableList<ChatSummary> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatSummary
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpChatName: String?
          if (_stmt.isNull(_columnIndexOfChatName)) {
            _tmpChatName = null
          } else {
            _tmpChatName = _stmt.getText(_columnIndexOfChatName)
          }
          val _tmpIsGroup: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsGroup).toInt()
          _tmpIsGroup = _tmp != 0
          val _tmpParticipantIds: String
          _tmpParticipantIds = _stmt.getText(_columnIndexOfParticipantIds)
          val _tmpUnreadCount: Int
          _tmpUnreadCount = _stmt.getLong(_columnIndexOfUnreadCount).toInt()
          val _tmpIsBlocked: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsBlocked).toInt()
          _tmpIsBlocked = _tmp_1 != 0
          val _tmpLastMessagePayload: String?
          if (_stmt.isNull(_columnIndexOfLastMessagePayload)) {
            _tmpLastMessagePayload = null
          } else {
            _tmpLastMessagePayload = _stmt.getText(_columnIndexOfLastMessagePayload)
          }
          val _tmpLastMessageTimestamp: Long?
          if (_stmt.isNull(_columnIndexOfLastMessageTimestamp)) {
            _tmpLastMessageTimestamp = null
          } else {
            _tmpLastMessageTimestamp = _stmt.getLong(_columnIndexOfLastMessageTimestamp)
          }
          val _tmpLastMessageType: String?
          if (_stmt.isNull(_columnIndexOfLastMessageType)) {
            _tmpLastMessageType = null
          } else {
            _tmpLastMessageType = _stmt.getText(_columnIndexOfLastMessageType)
          }
          _item = ChatSummary(_tmpChatId,_tmpChatName,_tmpIsGroup,_tmpParticipantIds,_tmpUnreadCount,_tmpIsBlocked,_tmpLastMessagePayload,_tmpLastMessageTimestamp,_tmpLastMessageType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalUnreadCount(): Flow<Int?> {
    val _sql: String = "SELECT SUM(unreadCount) FROM chats"
    return createFlow(__db, false, arrayOf("chats")) { _connection ->
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

  public override suspend fun updateBlockedStatus(chatId: String, isBlocked: Boolean) {
    val _sql: String = "UPDATE chats SET isBlocked = ? WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isBlocked) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateReportedStatus(chatId: String, isReported: Boolean) {
    val _sql: String = "UPDATE chats SET isReported = ? WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isReported) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateMutedStatus(chatId: String, isMuted: Boolean) {
    val _sql: String = "UPDATE chats SET isMuted = ? WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isMuted) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateParticipants(chatId: String, participantIds: String) {
    val _sql: String = "UPDATE chats SET participantIds = ? WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, participantIds)
        _argIndex = 2
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteChatById(chatId: String) {
    val _sql: String = "DELETE FROM chats WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun incrementUnreadCount(chatId: String) {
    val _sql: String = "UPDATE chats SET unreadCount = unreadCount + 1 WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearUnreadCount(chatId: String) {
    val _sql: String = "UPDATE chats SET unreadCount = 0 WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllChats() {
    val _sql: String = "DELETE FROM chats"
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
