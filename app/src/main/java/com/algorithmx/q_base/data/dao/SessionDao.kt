package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.SessionAttempt
import com.algorithmx.q_base.data.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Update
    suspend fun updateSession(session: StudySession)

    @Query("SELECT * FROM Study_Sessions ORDER BY created_timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM Study_Sessions WHERE is_completed = 0 ORDER BY created_timestamp DESC")
    fun getOngoingSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM Study_Sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): StudySession?

    @Query("SELECT * FROM Question_Collections ORDER BY created_timestamp DESC")
    fun getAllCollections(): Flow<List<QuestionCollection>>

    @Query("SELECT * FROM Question_Collections ORDER BY created_timestamp DESC LIMIT 5")
    fun getRecentCollections(): Flow<List<QuestionCollection>>

    @Query("SELECT * FROM Questions WHERE is_pinned = 1")
    fun getPinnedQuestions(): Flow<List<Question>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempts(attempts: List<SessionAttempt>)

    @Query("SELECT * FROM Session_Attempts WHERE session_id = :sessionId")
    fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>>

    @Update
    suspend fun updateAttempt(attempt: SessionAttempt)

    @Query("SELECT * FROM Session_Attempts WHERE session_id = :sessionId AND question_id = :questionId")
    suspend fun getAttempt(sessionId: String, questionId: String): SessionAttempt?
}
