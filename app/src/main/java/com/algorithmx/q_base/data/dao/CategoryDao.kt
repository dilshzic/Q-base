package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.CollectionQuestionCrossRef
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Master_Categories")
    fun getAllCategories(): Flow<List<MasterCategory>>

    @Query("SELECT COUNT(*) FROM Master_Categories")
    suspend fun getMasterCategoryCount(): Int

    @Query("SELECT * FROM Question_Collections WHERE master_category_id = :masterCategoryId")
    fun getCollectionsByMasterCategoryId(masterCategoryId: String): Flow<List<QuestionCollection>>

    @Query("SELECT * FROM Question_Collections WHERE master_category_id = (SELECT master_category_id FROM Master_Categories WHERE name = :categoryName LIMIT 1)")
    fun getCollectionsByCategory(categoryName: String): Flow<List<QuestionCollection>>

    @Query("""
        SELECT DISTINCT q.subject FROM Questions q
        INNER JOIN Master_Categories mc ON q.master_category = mc.name
        WHERE mc.master_category_id = :masterCategoryId
    """)
    fun getSubjectsByMasterCategoryId(masterCategoryId: String): Flow<List<String>>

    @Query("""
        SELECT q.* FROM Questions q
        INNER JOIN Collection_Questions_CrossRef x ON q.question_id = x.question_id
        WHERE x.collection_id = :collectionId
    """)
    fun getQuestionsForCollection(collectionId: String): Flow<List<Question>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterCategories(categories: List<MasterCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<QuestionCollection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<CollectionQuestionCrossRef>)
}
