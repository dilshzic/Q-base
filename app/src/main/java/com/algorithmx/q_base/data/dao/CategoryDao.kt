package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.QuestionCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Master_Categories")
    fun getAllCategories(): Flow<List<MasterCategory>>

    @Query("SELECT * FROM Question_Collections")
    fun getAllCollections(): Flow<List<QuestionCollection>>

    @Query("SELECT * FROM Question_Collections WHERE master_category = :masterCategory")
    fun getCollectionsByCategory(masterCategory: String): Flow<List<QuestionCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterCategories(categories: List<MasterCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<QuestionCollection>)
}
