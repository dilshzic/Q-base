package com.algorithmx.q_base.`data`.core

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
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
public class UserDao_Impl(
  __db: RoomDatabase,
) : UserDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfUserEntity: EntityInsertAdapter<UserEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfUserEntity = object : EntityInsertAdapter<UserEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `users` (`userId`,`displayName`,`email`,`intro`,`profilePictureUrl`,`friendCode`,`publicKey`,`isBanned`,`isPhotoVisible`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: UserEntity) {
        statement.bindText(1, entity.userId)
        statement.bindText(2, entity.displayName)
        val _tmpEmail: String? = entity.email
        if (_tmpEmail == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpEmail)
        }
        val _tmpIntro: String? = entity.intro
        if (_tmpIntro == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpIntro)
        }
        val _tmpProfilePictureUrl: String? = entity.profilePictureUrl
        if (_tmpProfilePictureUrl == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpProfilePictureUrl)
        }
        statement.bindText(6, entity.friendCode)
        val _tmpPublicKey: String? = entity.publicKey
        if (_tmpPublicKey == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpPublicKey)
        }
        val _tmp: Int = if (entity.isBanned) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.isPhotoVisible) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
      }
    }
  }

  public override suspend fun insertUser(user: UserEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfUserEntity.insert(_connection, user)
  }

  public override suspend fun getUserById(userId: String): UserEntity? {
    val _sql: String = "SELECT * FROM users WHERE userId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfEmail: Int = getColumnIndexOrThrow(_stmt, "email")
        val _columnIndexOfIntro: Int = getColumnIndexOrThrow(_stmt, "intro")
        val _columnIndexOfProfilePictureUrl: Int = getColumnIndexOrThrow(_stmt, "profilePictureUrl")
        val _columnIndexOfFriendCode: Int = getColumnIndexOrThrow(_stmt, "friendCode")
        val _columnIndexOfPublicKey: Int = getColumnIndexOrThrow(_stmt, "publicKey")
        val _columnIndexOfIsBanned: Int = getColumnIndexOrThrow(_stmt, "isBanned")
        val _columnIndexOfIsPhotoVisible: Int = getColumnIndexOrThrow(_stmt, "isPhotoVisible")
        val _result: UserEntity?
        if (_stmt.step()) {
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpEmail: String?
          if (_stmt.isNull(_columnIndexOfEmail)) {
            _tmpEmail = null
          } else {
            _tmpEmail = _stmt.getText(_columnIndexOfEmail)
          }
          val _tmpIntro: String?
          if (_stmt.isNull(_columnIndexOfIntro)) {
            _tmpIntro = null
          } else {
            _tmpIntro = _stmt.getText(_columnIndexOfIntro)
          }
          val _tmpProfilePictureUrl: String?
          if (_stmt.isNull(_columnIndexOfProfilePictureUrl)) {
            _tmpProfilePictureUrl = null
          } else {
            _tmpProfilePictureUrl = _stmt.getText(_columnIndexOfProfilePictureUrl)
          }
          val _tmpFriendCode: String
          _tmpFriendCode = _stmt.getText(_columnIndexOfFriendCode)
          val _tmpPublicKey: String?
          if (_stmt.isNull(_columnIndexOfPublicKey)) {
            _tmpPublicKey = null
          } else {
            _tmpPublicKey = _stmt.getText(_columnIndexOfPublicKey)
          }
          val _tmpIsBanned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBanned).toInt()
          _tmpIsBanned = _tmp != 0
          val _tmpIsPhotoVisible: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPhotoVisible).toInt()
          _tmpIsPhotoVisible = _tmp_1 != 0
          _result = UserEntity(_tmpUserId,_tmpDisplayName,_tmpEmail,_tmpIntro,_tmpProfilePictureUrl,_tmpFriendCode,_tmpPublicKey,_tmpIsBanned,_tmpIsPhotoVisible)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCurrentUser(userId: String): Flow<UserEntity?> {
    val _sql: String = "SELECT * FROM users WHERE userId = ?"
    return createFlow(__db, false, arrayOf("users")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfEmail: Int = getColumnIndexOrThrow(_stmt, "email")
        val _columnIndexOfIntro: Int = getColumnIndexOrThrow(_stmt, "intro")
        val _columnIndexOfProfilePictureUrl: Int = getColumnIndexOrThrow(_stmt, "profilePictureUrl")
        val _columnIndexOfFriendCode: Int = getColumnIndexOrThrow(_stmt, "friendCode")
        val _columnIndexOfPublicKey: Int = getColumnIndexOrThrow(_stmt, "publicKey")
        val _columnIndexOfIsBanned: Int = getColumnIndexOrThrow(_stmt, "isBanned")
        val _columnIndexOfIsPhotoVisible: Int = getColumnIndexOrThrow(_stmt, "isPhotoVisible")
        val _result: UserEntity?
        if (_stmt.step()) {
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpEmail: String?
          if (_stmt.isNull(_columnIndexOfEmail)) {
            _tmpEmail = null
          } else {
            _tmpEmail = _stmt.getText(_columnIndexOfEmail)
          }
          val _tmpIntro: String?
          if (_stmt.isNull(_columnIndexOfIntro)) {
            _tmpIntro = null
          } else {
            _tmpIntro = _stmt.getText(_columnIndexOfIntro)
          }
          val _tmpProfilePictureUrl: String?
          if (_stmt.isNull(_columnIndexOfProfilePictureUrl)) {
            _tmpProfilePictureUrl = null
          } else {
            _tmpProfilePictureUrl = _stmt.getText(_columnIndexOfProfilePictureUrl)
          }
          val _tmpFriendCode: String
          _tmpFriendCode = _stmt.getText(_columnIndexOfFriendCode)
          val _tmpPublicKey: String?
          if (_stmt.isNull(_columnIndexOfPublicKey)) {
            _tmpPublicKey = null
          } else {
            _tmpPublicKey = _stmt.getText(_columnIndexOfPublicKey)
          }
          val _tmpIsBanned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBanned).toInt()
          _tmpIsBanned = _tmp != 0
          val _tmpIsPhotoVisible: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPhotoVisible).toInt()
          _tmpIsPhotoVisible = _tmp_1 != 0
          _result = UserEntity(_tmpUserId,_tmpDisplayName,_tmpEmail,_tmpIntro,_tmpProfilePictureUrl,_tmpFriendCode,_tmpPublicKey,_tmpIsBanned,_tmpIsPhotoVisible)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllUsers(): Flow<List<UserEntity>> {
    val _sql: String = "SELECT * FROM users"
    return createFlow(__db, false, arrayOf("users")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfEmail: Int = getColumnIndexOrThrow(_stmt, "email")
        val _columnIndexOfIntro: Int = getColumnIndexOrThrow(_stmt, "intro")
        val _columnIndexOfProfilePictureUrl: Int = getColumnIndexOrThrow(_stmt, "profilePictureUrl")
        val _columnIndexOfFriendCode: Int = getColumnIndexOrThrow(_stmt, "friendCode")
        val _columnIndexOfPublicKey: Int = getColumnIndexOrThrow(_stmt, "publicKey")
        val _columnIndexOfIsBanned: Int = getColumnIndexOrThrow(_stmt, "isBanned")
        val _columnIndexOfIsPhotoVisible: Int = getColumnIndexOrThrow(_stmt, "isPhotoVisible")
        val _result: MutableList<UserEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: UserEntity
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpEmail: String?
          if (_stmt.isNull(_columnIndexOfEmail)) {
            _tmpEmail = null
          } else {
            _tmpEmail = _stmt.getText(_columnIndexOfEmail)
          }
          val _tmpIntro: String?
          if (_stmt.isNull(_columnIndexOfIntro)) {
            _tmpIntro = null
          } else {
            _tmpIntro = _stmt.getText(_columnIndexOfIntro)
          }
          val _tmpProfilePictureUrl: String?
          if (_stmt.isNull(_columnIndexOfProfilePictureUrl)) {
            _tmpProfilePictureUrl = null
          } else {
            _tmpProfilePictureUrl = _stmt.getText(_columnIndexOfProfilePictureUrl)
          }
          val _tmpFriendCode: String
          _tmpFriendCode = _stmt.getText(_columnIndexOfFriendCode)
          val _tmpPublicKey: String?
          if (_stmt.isNull(_columnIndexOfPublicKey)) {
            _tmpPublicKey = null
          } else {
            _tmpPublicKey = _stmt.getText(_columnIndexOfPublicKey)
          }
          val _tmpIsBanned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsBanned).toInt()
          _tmpIsBanned = _tmp != 0
          val _tmpIsPhotoVisible: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPhotoVisible).toInt()
          _tmpIsPhotoVisible = _tmp_1 != 0
          _item = UserEntity(_tmpUserId,_tmpDisplayName,_tmpEmail,_tmpIntro,_tmpProfilePictureUrl,_tmpFriendCode,_tmpPublicKey,_tmpIsBanned,_tmpIsPhotoVisible)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllUsers() {
    val _sql: String = "DELETE FROM users"
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
