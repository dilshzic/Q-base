package com.algorithmx.q_base.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.algorithmx.q_base.data.dao.*
import com.algorithmx.q_base.data.entity.*

@Database(
    entities = [
        com.algorithmx.q_base.data.entity.Collection::class,
        Question::class,
        QuestionOption::class,
        Answer::class,
        QuestionSet::class,
        SetQuestionCrossRef::class,
        StudySession::class,
        SessionAttempt::class,
        ProblemReport::class,
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        AiResponseEntity::class,
        BrainUsageEntity::class
    ],
    version = 19,
    exportSchema = false
)
@TypeConverters(com.algorithmx.q_base.data.util.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun collectionDao(): CollectionDao
    abstract fun sessionDao(): SessionDao
    abstract fun problemReportDao(): ProblemReportDao
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun aiResponseDao(): AiResponseDao
    abstract fun brainUsageDao(): BrainUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Collections` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `unreadCount` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `isPhotoVisible` INTEGER NOT NULL DEFAULT 1")
            }
        }

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `userId` TEXT NOT NULL PRIMARY KEY,
                        `displayName` TEXT NOT NULL,
                        `friendCode` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chats` (
                        `chatId` TEXT NOT NULL PRIMARY KEY,
                        `chatName` TEXT,
                        `isGroup` INTEGER NOT NULL,
                        `participantIds` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `messages` (
                        `messageId` TEXT NOT NULL PRIMARY KEY,
                        `chatId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`chatId`) REFERENCES `chats`(`chatId`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId` ON `messages`(`chatId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `profilePictureUrl` TEXT")
                db.execSQL("ALTER TABLE `Study_Sessions` ADD COLUMN `timing_type` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE `Study_Sessions` ADD COLUMN `is_random` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename Master_Categories to Collections
                db.execSQL("ALTER TABLE `Master_Categories` RENAME TO `Collections` ")
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Collections_new` (
                        `collection_id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `is_user_created` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `Collections_new` (collection_id, name, description, is_user_created) " +
                           "SELECT master_category_id, name, description, is_user_created FROM `Collections` ")
                db.execSQL("DROP TABLE `Collections` ")
                db.execSQL("ALTER TABLE `Collections_new` RENAME TO `Collections` ")

                // Rename Question_Collections to Question_Sets
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Question_Sets` (
                        `set_id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `parent_collection_id` TEXT NOT NULL,
                        `description` TEXT,
                        `created_timestamp` INTEGER NOT NULL DEFAULT 0,
                        `is_user_created` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`parent_collection_id`) REFERENCES `Collections`(`collection_id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `Question_Sets` (set_id, title, parent_collection_id, description, created_timestamp, is_user_created) " +
                           "SELECT collection_id, title, master_category_id, description, created_timestamp, is_user_created FROM `Question_Collections` ")
                db.execSQL("DROP TABLE `Question_Collections` ")

                // Update Collection_Questions_CrossRef
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Set_Questions_CrossRef` (
                        `mapping_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `set_id` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        FOREIGN KEY(`set_id`) REFERENCES `Question_Sets`(`set_id`) ON DELETE CASCADE,
                        FOREIGN KEY(`question_id`) REFERENCES `Questions`(`question_id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `Set_Questions_CrossRef` (mapping_id, set_id, question_id) " +
                           "SELECT mapping_id, collection_id, question_id FROM `Collection_Questions_CrossRef` ")
                db.execSQL("DROP TABLE `Collection_Questions_CrossRef` ")
                
                // Update Questions table if it has master_category
                db.execSQL("ALTER TABLE `Questions` RENAME COLUMN `master_category` TO `collection` ")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `isBlocked` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `isReported` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `isMuted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Question_Options` ADD COLUMN `option_explanation` TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `email` TEXT")
                db.execSQL("ALTER TABLE `users` ADD COLUMN `intro` TEXT")
                db.execSQL("ALTER TABLE `Collections` ADD COLUMN `is_shared` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chats` ADD COLUMN `adminId` TEXT")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Ai_Responses` (
                        `response_id` TEXT NOT NULL PRIMARY KEY,
                        `topic` TEXT NOT NULL,
                        `raw_json` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `is_promoted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `brain_usage_history` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        `provider` TEXT NOT NULL,
                        `modelUsed` TEXT NOT NULL,
                        `tokensEstimated` INTEGER NOT NULL,
                        `isSuccess` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `publicKey` TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `isBanned` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "Qbase.db"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                    .fallbackToDestructiveMigration()
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
