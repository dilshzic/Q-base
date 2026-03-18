package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.SessionAttempt
import com.algorithmx.q_base.data.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Query("SELECT * FROM Study_Sessions ORDER BY session_id DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM Study_Sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): StudySession?

    @Query("SELECT * FROM Question_Collections")
    fun getAllCollections(): Flow<List<QuestionCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempts(attempts: List<SessionAttempt>)

    @Query("SELECT * FROM Session_Attempts WHERE session_id = :sessionId")
    fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>>

    @Update
    suspend fun updateAttempt(attempt: SessionAttempt)

    @Query("SELECT * FROM Session_Attempts WHERE session_id = :sessionId AND question_id = :questionId")
    suspend fun getAttempt(sessionId: String, questionId: String): SessionAttempt?
}
