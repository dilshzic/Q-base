package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.Collection
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionSet
import com.algorithmx.q_base.data.entity.SetQuestionCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM Collections")
    fun getAllCollections(): Flow<List<com.algorithmx.q_base.data.entity.Collection>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM Questions WHERE collection = name) as questionCount 
        FROM Collections
    """)
    fun getAllCollectionsWithCount(): Flow<List<com.algorithmx.q_base.data.entity.CollectionWithCount>>

    @Query("SELECT COUNT(*) FROM Collections")
    suspend fun getCollectionCount(): Int

    @Query("SELECT * FROM Collections WHERE collection_id = :collectionId")
    fun getCollectionById(collectionId: String): Flow<com.algorithmx.q_base.data.entity.Collection?>

    @Query("SELECT * FROM Collections WHERE collection_id = :collectionId")
    suspend fun getCollectionByIdOnce(collectionId: String): com.algorithmx.q_base.data.entity.Collection?

    @Query("SELECT * FROM Question_Sets WHERE parent_collection_id = :collectionId")
    fun getSetsByCollectionId(collectionId: String): Flow<List<QuestionSet>>

    @Query("SELECT * FROM Question_Sets WHERE parent_collection_id = (SELECT collection_id FROM Collections WHERE name = :collectionName LIMIT 1)")
    fun getSetsByCollectionName(collectionName: String): Flow<List<QuestionSet>>

    @Query("""
        SELECT DISTINCT q.category FROM Questions q
        INNER JOIN Collections c ON q.collection = c.name
        WHERE c.collection_id = :collectionId
    """)
    fun getCategoriesByCollectionId(collectionId: String): Flow<List<String>>

    @Query("""
        SELECT q.* FROM Questions q
        INNER JOIN Set_Questions_CrossRef x ON q.question_id = x.question_id
        WHERE x.set_id = :setId
    """)
    fun getQuestionsForSet(setId: String): Flow<List<Question>>

    @Query("SELECT * FROM Question_Sets WHERE parent_collection_id = :collectionId")
    suspend fun getSetsByCollectionIdOnce(collectionId: String): List<QuestionSet>

    @Query("""
        SELECT q.* FROM Questions q
        INNER JOIN Set_Questions_CrossRef x ON q.question_id = x.question_id
        WHERE x.set_id = :setId
    """)
    suspend fun getQuestionsForSetOnce(setId: String): List<Question>

    @Query("SELECT * FROM Set_Questions_CrossRef WHERE set_id IN (:setIds)")
    suspend fun getCrossRefsForSetsBatch(setIds: List<String>): List<SetQuestionCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<com.algorithmx.q_base.data.entity.Collection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<QuestionSet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<SetQuestionCrossRef>)

    @Query("UPDATE Collections SET updated_at = :timestamp WHERE collection_id = :collectionId")
    suspend fun updateCollectionTimestamp(collectionId: String, timestamp: Long)

    @Query("DELETE FROM Collections WHERE collection_id = :collectionId")
    suspend fun deleteCollectionById(collectionId: String)

    @Query("DELETE FROM Collections")
    suspend fun deleteAllCollections()
}
