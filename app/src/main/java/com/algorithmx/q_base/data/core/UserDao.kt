package com.algorithmx.q_base.data.core

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getCurrentUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
