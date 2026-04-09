package com.algorithmx.q_base.data.sessions

import androidx.room.*
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionSet
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: QuestionSet)

    @Update
    suspend fun updateSession(session: StudySession)

    @Query("SELECT * FROM Study_Sessions ORDER BY created_timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM Study_Sessions WHERE is_completed = 0 ORDER BY created_timestamp DESC")
    fun getOngoingSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM Study_Sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): StudySession?

    @Query("SELECT * FROM Question_Sets ORDER BY created_timestamp DESC")
    fun getAllSets(): Flow<List<QuestionSet>>

    @Query("SELECT * FROM Question_Sets ORDER BY created_timestamp DESC LIMIT 5")
    fun getRecentSets(): Flow<List<QuestionSet>>

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

    @Query("SELECT * FROM Study_Sessions WHERE collection_id = :collectionId AND is_completed = 0 ORDER BY created_timestamp DESC LIMIT 1")
    fun getLastSessionForStudyCollection(collectionId: String): Flow<StudySession?>

    @Query("DELETE FROM Study_Sessions WHERE session_id IN (:sessionIds)")
    suspend fun deleteSessionsByIds(sessionIds: List<String>)

    @Query("DELETE FROM Session_Attempts WHERE session_id IN (:sessionIds)")
    suspend fun deleteAttemptsForSessions(sessionIds: List<String>)

    @Query("DELETE FROM Study_Sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM Session_Attempts")
    suspend fun deleteAllAttempts()

    @Query("DELETE FROM Question_Sets")
    suspend fun deleteAllSets()
}
