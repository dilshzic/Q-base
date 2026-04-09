package com.algorithmx.q_base.data.collections

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM StudyCollections")
    fun getAllStudyCollections(): Flow<List<StudyCollection>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM Questions WHERE collection = name) as questionCount 
        FROM StudyCollections
    """)
    fun getAllStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>>

    @Query("SELECT COUNT(*) FROM StudyCollections")
    suspend fun getStudyCollectionCount(): Int

    @Query("SELECT * FROM StudyCollections WHERE collection_id = :collectionId")
    fun getStudyCollectionById(collectionId: String): Flow<StudyCollection?>

    @Query("SELECT * FROM StudyCollections WHERE collection_id = :collectionId")
    suspend fun getStudyCollectionByIdOnce(collectionId: String): StudyCollection?

    @Query("SELECT * FROM Question_Sets WHERE parent_collection_id = :collectionId")
    fun getSetsByStudyCollectionId(collectionId: String): Flow<List<QuestionSet>>

    @Query("SELECT * FROM Question_Sets WHERE parent_collection_id = (SELECT collection_id FROM StudyCollections WHERE name = :collectionName LIMIT 1)")
    fun getSetsByStudyCollectionName(collectionName: String): Flow<List<QuestionSet>>

    @Query("""
        SELECT DISTINCT q.category FROM Questions q
        INNER JOIN StudyCollections c ON q.collection = c.name
        WHERE c.collection_id = :collectionId
    """)
    fun getCategoriesByStudyCollectionId(collectionId: String): Flow<List<String>>

    @Query("""
        SELECT q.* FROM Questions q
        INNER JOIN Set_Questions_CrossRef x ON q.question_id = x.question_id
        WHERE x.set_id = :setId
    """)
    fun getQuestionsForSet(setId: String): Flow<List<Question>>

    @Query("SELECT * FROM Question_Sets WHERE parent_collection_id = :collectionId")
    suspend fun getSetsByStudyCollectionIdOnce(collectionId: String): List<QuestionSet>

    @Query("""
        SELECT q.* FROM Questions q
        INNER JOIN Set_Questions_CrossRef x ON q.question_id = x.question_id
        WHERE x.set_id = :setId
    """)
    suspend fun getQuestionsForSetOnce(setId: String): List<Question>

    @Query("SELECT * FROM Set_Questions_CrossRef WHERE set_id IN (:setIds)")
    suspend fun getCrossRefsForSetsBatch(setIds: List<String>): List<SetQuestionCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyCollections(collections: List<StudyCollection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<QuestionSet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<SetQuestionCrossRef>)

    @Query("UPDATE StudyCollections SET updated_at = :timestamp WHERE collection_id = :collectionId")
    suspend fun updateStudyCollectionTimestamp(collectionId: String, timestamp: Long)

    @Query("DELETE FROM StudyCollections WHERE collection_id = :collectionId")
    suspend fun deleteStudyCollectionById(collectionId: String)

    @Query("DELETE FROM StudyCollections")
    suspend fun deleteAllStudyCollections()
}
