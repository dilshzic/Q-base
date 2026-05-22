package com.algorithmx.q_base.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.algorithmx.q_base.core.ai.data.*
import com.algorithmx.q_base.feature.content_import.data.*
import com.algorithmx.q_base.core.data.*
import com.algorithmx.q_base.feature.sessions.data.*

@Database(
    entities = [
        com.algorithmx.q_base.data.collections.StudyCollection::class,
        com.algorithmx.q_base.data.collections.Question::class,
        com.algorithmx.q_base.data.collections.QuestionOption::class,
        com.algorithmx.q_base.data.collections.Answer::class,
        com.algorithmx.q_base.data.collections.QuestionSet::class,
        com.algorithmx.q_base.data.collections.SetQuestionCrossRef::class,
        com.algorithmx.q_base.data.sessions.StudySession::class,
        com.algorithmx.q_base.data.sessions.SessionAttempt::class,
        com.algorithmx.q_base.data.collections.ProblemReport::class,
        com.algorithmx.q_base.data.core.UserEntity::class,
        com.algorithmx.q_base.data.ai.AiResponseEntity::class,
        com.algorithmx.q_base.data.ai.BrainUsageEntity::class,
        com.algorithmx.q_base.data.collections.CollectionVersionLedgerEntity::class,
        com.algorithmx.q_base.data.sync.OfflineActionEntity::class
    ],
    version = 28,
    exportSchema = false
)
@TypeConverters(com.algorithmx.q_base.data.util.TypeConverters::class)
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