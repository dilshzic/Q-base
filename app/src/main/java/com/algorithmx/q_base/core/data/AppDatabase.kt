package com.algorithmx.q_base.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.algorithmx.q_base.core.ai.data.*
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.core.data.*
import com.algorithmx.q_base.feature.sessions.data.*

@Database(
    entities = [
        StudyCollection::class,
        Question::class,
        QuestionOption::class,
        Answer::class,
        QuestionSet::class,
        SetQuestionCrossRef::class,
        com.algorithmx.q_base.feature.sessions.data.StudySession::class,
        com.algorithmx.q_base.feature.sessions.data.SessionAttempt::class,
        ProblemReport::class,
        UserEntity::class,
        AiResponseEntity::class,
        BrainUsageEntity::class,
        CollectionVersionLedgerEntity::class,
        com.algorithmx.q_base.sync.orchestration.OfflineActionEntity::class
    ],
    version = 28,
    exportSchema = false
)
@TypeConverters(com.algorithmx.q_base.core.data.util.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun collectionDao(): CollectionDao
    abstract fun sessionDao(): SessionDao
    abstract fun problemReportDao(): ProblemReportDao
    abstract fun userDao(): UserDao
    abstract fun aiResponseDao(): AiResponseDao
    abstract fun brainUsageDao(): BrainUsageDao
    abstract fun collectionVersionLedgerDao(): CollectionVersionLedgerDao
    abstract fun actionQueueDao(): com.algorithmx.q_base.data.sync.ActionQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "Qbase.db"
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error building database", e)
                    throw e
                }
            }
        }
    }
}