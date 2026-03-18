package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.Answer
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM Questions WHERE master_category = :masterCategory")
    fun getQuestionsByMasterCategory(masterCategory: String): Flow<List<Question>>

    @Query("SELECT * FROM Questions WHERE category = :category")
    fun getQuestionsByCategory(category: String): Flow<List<Question>>

    @Query("SELECT * FROM Questions WHERE category = :category AND subject = :subject")
    fun getQuestionsByCategoryAndSubject(category: String, subject: String): Flow<List<Question>>

    @Query("SELECT * FROM Question_Options WHERE question_id = :questionId")
    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>>

    @Query("SELECT * FROM Answers WHERE question_id = :questionId")
    fun getAnswerForQuestion(questionId: String): Flow<Answer?>

    @Query("SELECT * FROM Questions WHERE question_id = :questionId")
    suspend fun getQuestionById(questionId: String): Question?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<QuestionOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<Answer>)

    @Query("SELECT COUNT(*) FROM Questions")
    suspend fun getQuestionCount(): Int
}
