package com.algorithmx.q_base.`data`.chat

import androidx.room.EntityDeleteOrUpdateAdapter
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
public class MessageDao_Impl(
  __db: RoomDatabase,
) : MessageDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMessageEntity: EntityInsertAdapter<MessageEntity>

  private val __deleteAdapterOfMessageEntity: EntityDeleteOrUpdateAdapter<MessageEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfMessageEntity = object : EntityInsertAdapter<MessageEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `messages` (`messageId`,`chatId`,`senderId`,`payload`,`type`,`timestamp`,`decryptionStatus`,`status`,`keyFingerprint`,`wrappedKey`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MessageEntity) {
        statement.bindText(1, entity.messageId)
        statement.bindText(2, entity.chatId)
        statement.bindText(3, entity.senderId)
        statement.bindText(4, entity.payload)
        statement.bindText(5, entity.type)
        statement.bindLong(6, entity.timestamp)
        statement.bindText(7, entity.decryptionStatus)
        statement.bindText(8, entity.status)
        val _tmpKeyFingerprint: String? = entity.keyFingerprint
        if (_tmpKeyFingerprint == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpKeyFingerprint)
        }
        val _tmpWrappedKey: String? = entity.wrappedKey
        if (_tmpWrappedKey == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpWrappedKey)
        }
      }
    }
    this.__deleteAdapterOfMessageEntity = object : EntityDeleteOrUpdateAdapter<MessageEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `messages` WHERE `messageId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MessageEntity) {
        statement.bindText(1, entity.messageId)
      }
    }
  }

  public override suspend fun insertMessage(message: MessageEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMessageEntity.insert(_connection, message)
  }

  public override suspend fun deleteMessage(message: MessageEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfMessageEntity.handle(_connection, message)
  }

  public override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
    val _sql: String = "SELECT * FROM messages WHERE chatId = ? ORDER BY timestamp ASC"
    return createFlow(__db, false, arrayOf("messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfSenderId: Int = getColumnIndexOrThrow(_stmt, "senderId")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfDecryptionStatus: Int = getColumnIndexOrThrow(_stmt, "decryptionStatus")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "keyFingerprint")
        val _columnIndexOfWrappedKey: Int = getColumnIndexOrThrow(_stmt, "wrappedKey")
        val _result: MutableList<MessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MessageEntity
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpSenderId: String
          _tmpSenderId = _stmt.getText(_columnIndexOfSenderId)
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpDecryptionStatus: String
          _tmpDecryptionStatus = _stmt.getText(_columnIndexOfDecryptionStatus)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfKeyFingerprint)) {
            _tmpKeyFingerprint = null
          } else {
            _tmpKeyFingerprint = _stmt.getText(_columnIndexOfKeyFingerprint)
          }
          val _tmpWrappedKey: String?
          if (_stmt.isNull(_columnIndexOfWrappedKey)) {
            _tmpWrappedKey = null
          } else {
            _tmpWrappedKey = _stmt.getText(_columnIndexOfWrappedKey)
          }
          _item = MessageEntity(_tmpMessageId,_tmpChatId,_tmpSenderId,_tmpPayload,_tmpType,_tmpTimestamp,_tmpDecryptionStatus,_tmpStatus,_tmpKeyFingerprint,_tmpWrappedKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getMessageById(messageId: String): MessageEntity? {
    val _sql: String = "SELECT * FROM messages WHERE messageId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, messageId)
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfSenderId: Int = getColumnIndexOrThrow(_stmt, "senderId")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfDecryptionStatus: Int = getColumnIndexOrThrow(_stmt, "decryptionStatus")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "keyFingerprint")
        val _columnIndexOfWrappedKey: Int = getColumnIndexOrThrow(_stmt, "wrappedKey")
        val _result: MessageEntity?
        if (_stmt.step()) {
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpSenderId: String
          _tmpSenderId = _stmt.getText(_columnIndexOfSenderId)
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpDecryptionStatus: String
          _tmpDecryptionStatus = _stmt.getText(_columnIndexOfDecryptionStatus)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfKeyFingerprint)) {
            _tmpKeyFingerprint = null
          } else {
            _tmpKeyFingerprint = _stmt.getText(_columnIndexOfKeyFingerprint)
          }
          val _tmpWrappedKey: String?
          if (_stmt.isNull(_columnIndexOfWrappedKey)) {
            _tmpWrappedKey = null
          } else {
            _tmpWrappedKey = _stmt.getText(_columnIndexOfWrappedKey)
          }
          _result = MessageEntity(_tmpMessageId,_tmpChatId,_tmpSenderId,_tmpPayload,_tmpType,_tmpTimestamp,_tmpDecryptionStatus,_tmpStatus,_tmpKeyFingerprint,_tmpWrappedKey)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllMessages(): Flow<List<MessageEntity>> {
    val _sql: String = "SELECT * FROM messages"
    return createFlow(__db, false, arrayOf("messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfSenderId: Int = getColumnIndexOrThrow(_stmt, "senderId")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfDecryptionStatus: Int = getColumnIndexOrThrow(_stmt, "decryptionStatus")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "keyFingerprint")
        val _columnIndexOfWrappedKey: Int = getColumnIndexOrThrow(_stmt, "wrappedKey")
        val _result: MutableList<MessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MessageEntity
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpSenderId: String
          _tmpSenderId = _stmt.getText(_columnIndexOfSenderId)
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpDecryptionStatus: String
          _tmpDecryptionStatus = _stmt.getText(_columnIndexOfDecryptionStatus)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfKeyFingerprint)) {
            _tmpKeyFingerprint = null
          } else {
            _tmpKeyFingerprint = _stmt.getText(_columnIndexOfKeyFingerprint)
          }
          val _tmpWrappedKey: String?
          if (_stmt.isNull(_columnIndexOfWrappedKey)) {
            _tmpWrappedKey = null
          } else {
            _tmpWrappedKey = _stmt.getText(_columnIndexOfWrappedKey)
          }
          _item = MessageEntity(_tmpMessageId,_tmpChatId,_tmpSenderId,_tmpPayload,_tmpType,_tmpTimestamp,_tmpDecryptionStatus,_tmpStatus,_tmpKeyFingerprint,_tmpWrappedKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPendingMessages(): List<MessageEntity> {
    val _sql: String = "SELECT * FROM messages WHERE status = 'PENDING' ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfSenderId: Int = getColumnIndexOrThrow(_stmt, "senderId")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfDecryptionStatus: Int = getColumnIndexOrThrow(_stmt, "decryptionStatus")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfKeyFingerprint: Int = getColumnIndexOrThrow(_stmt, "keyFingerprint")
        val _columnIndexOfWrappedKey: Int = getColumnIndexOrThrow(_stmt, "wrappedKey")
        val _result: MutableList<MessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MessageEntity
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpSenderId: String
          _tmpSenderId = _stmt.getText(_columnIndexOfSenderId)
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpDecryptionStatus: String
          _tmpDecryptionStatus = _stmt.getText(_columnIndexOfDecryptionStatus)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpKeyFingerprint: String?
          if (_stmt.isNull(_columnIndexOfKeyFingerprint)) {
            _tmpKeyFingerprint = null
          } else {
            _tmpKeyFingerprint = _stmt.getText(_columnIndexOfKeyFingerprint)
          }
          val _tmpWrappedKey: String?
          if (_stmt.isNull(_columnIndexOfWrappedKey)) {
            _tmpWrappedKey = null
          } else {
            _tmpWrappedKey = _stmt.getText(_columnIndexOfWrappedKey)
          }
          _item = MessageEntity(_tmpMessageId,_tmpChatId,_tmpSenderId,_tmpPayload,_tmpType,_tmpTimestamp,_tmpDecryptionStatus,_tmpStatus,_tmpKeyFingerprint,_tmpWrappedKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteMessagesByChatId(chatId: String) {
    val _sql: String = "DELETE FROM messages WHERE chatId = ?"
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

  public override suspend fun deleteAllMessages() {
    val _sql: String = "DELETE FROM messages"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateMessageStatus(messageId: String, status: String) {
    val _sql: String = "UPDATE messages SET status = ? WHERE messageId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        _stmt.bindText(_argIndex, messageId)
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
