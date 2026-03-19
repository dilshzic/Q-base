package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.ProblemReport
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ProblemReport)

    @Query("SELECT * FROM Problem_Reports")
    fun getAllReports(): Flow<List<ProblemReport>>

    @Query("SELECT * FROM Problem_Reports WHERE question_id = :questionId")
    fun getReportsForQuestion(questionId: String): Flow<List<ProblemReport>>
}
