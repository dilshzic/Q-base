package com.algorithmx.q_base.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.ProblemReportDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.entity.*

@Database(
    entities = [
        Question::class,
        QuestionOption::class,
        Answer::class,
        MasterCategory::class,
        QuestionCollection::class,
        CollectionQuestionCrossRef::class,
        StudySession::class,
        SessionAttempt::class,
        ProblemReport::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun problemReportDao(): ProblemReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `Master_Categories`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Master_Categories` (
                        `master_category_id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `is_user_created` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("DROP TABLE IF EXISTS `Question_Collections`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Question_Collections` (
                        `collection_id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `master_category_id` TEXT NOT NULL,
                        `description` TEXT,
                        `created_timestamp` INTEGER NOT NULL DEFAULT 0,
                        `is_user_created` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`master_category_id`) REFERENCES `Master_Categories`(`master_category_id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Collection_Questions_CrossRef` (
                        `mapping_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `collection_id` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        FOREIGN KEY(`collection_id`) REFERENCES `Question_Collections`(`collection_id`) ON DELETE CASCADE,
                        FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_crossref_collection` ON `Collection_Questions_CrossRef`(`collection_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_crossref_question` ON `Collection_Questions_CrossRef`(`question_id`)")

                db.execSQL("ALTER TABLE `Session_Attempts` ADD COLUMN `time_spent_seconds` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `Study_Sessions` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `Study_Sessions` ADD COLUMN `created_timestamp` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Problem_Reports` (
                        `report_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "MedicalQuiz.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
