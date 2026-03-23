package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM Questions WHERE collection = :collection")
    fun getQuestionsByCollection(collection: String): Flow<List<Question>>

    @Query("SELECT * FROM Questions WHERE category = :category")
    fun getQuestionsByCategory(category: String): Flow<List<Question>>

    @Query("SELECT * FROM Questions WHERE tags LIKE :tag")
    fun getQuestionsByTag(tag: String): Flow<List<Question>>

    @Query("SELECT * FROM Questions WHERE category = :category AND tags LIKE :tag")
    fun getQuestionsByCategoryAndTag(category: String, tag: String): Flow<List<Question>>

    @Query("SELECT * FROM Question_Options WHERE question_id = :questionId")
    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>>

    @Query("SELECT * FROM Answers WHERE question_id = :questionId")
    fun getAnswerForQuestion(questionId: String): Flow<Answer?>

    @Query("SELECT * FROM Question_Options WHERE question_id = :questionId")
    suspend fun getOptionsForQuestionOnce(questionId: String): List<QuestionOption>

    @Query("SELECT * FROM Answers WHERE question_id = :questionId")
    suspend fun getAnswerForQuestionOnce(questionId: String): Answer?

    @Query("SELECT * FROM Questions WHERE question_id = :questionId")
    suspend fun getQuestionById(questionId: String): Question?

    @Update
    suspend fun updateQuestion(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<QuestionOption>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<Answer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: QuestionSet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: QuestionOption)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: Answer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetQuestionCrossRef(crossRef: SetQuestionCrossRef)

    @Query("SELECT COUNT(*) FROM Questions")
    fun getQuestionCountFlow(): Flow<Int>

    @Query("""
        SELECT COUNT(q.question_id) 
        FROM Questions q 
        INNER JOIN Collections c ON q.collection = c.name 
        WHERE c.is_user_created = 1
    """)
    fun getUserCreatedQuestionCount(): Flow<Int>

    @Query("""
        SELECT COUNT(q.question_id) 
        FROM Questions q 
        INNER JOIN Collections c ON q.collection = c.name 
        WHERE c.is_shared = 1
    """)
    fun getSharedQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Questions")
    suspend fun getQuestionCount(): Int

    @Transaction
    @Query("SELECT * FROM Question_Sets WHERE set_id = :setId")
    suspend fun getSetWithContent(setId: String): SetWithQuestions?

    @Query("SELECT COUNT(*) FROM Questions WHERE collection = :collectionName")
    fun getQuestionCountByCollection(collectionName: String): Flow<Int>

    @Query("DELETE FROM Question_Options WHERE question_id = :questionId")
    suspend fun deleteOptionsForQuestion(questionId: String)

    @Query("DELETE FROM Question_Sets WHERE set_id = :setId")
    suspend fun deleteSetById(setId: String)

    @Query("DELETE FROM Question_Sets WHERE set_id IN (:setIds)")
    suspend fun deleteSetsByIds(setIds: List<String>)

    @Query("DELETE FROM Set_Questions_CrossRef WHERE set_id IN (:setIds)")
    suspend fun deleteCrossRefsForSets(setIds: List<String>)

    @Query("DELETE FROM Set_Questions_CrossRef WHERE set_id = :setId AND question_id = :questionId")
    suspend fun removeQuestionFromSet(setId: String, questionId: String)

    @Query("DELETE FROM Questions WHERE question_id = :questionId")
    suspend fun deleteQuestionById(questionId: String)

    @Query("DELETE FROM Questions")
    suspend fun deleteAllQuestions()

    @Query("DELETE FROM Question_Options")
    suspend fun deleteAllOptions()

    @Query("DELETE FROM Answers")
    suspend fun deleteAllAnswers()

    @Query("DELETE FROM Set_Questions_CrossRef")
    suspend fun deleteAllCrossRefs()
}
