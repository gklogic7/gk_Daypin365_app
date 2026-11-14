package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity

@Database(
    entities = [
        ReminderEntity::class,
        StickyNoteEntity::class,
        TaskEntity::class,
        TaskProofEntity::class,
        QuickTemplateEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun stickyNoteDao(): StickyNoteDao
    abstract fun taskDao(): TaskDao
    abstract fun quickTemplateDao(): QuickTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN musicUri TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN musicDisplayName TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN imageUri TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN imageDisplayName TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN videoUri TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN videoDisplayName TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN useCustomInterval INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customIntervalUnit TEXT NOT NULL DEFAULT 'DAYS'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN customIntervalValue INTEGER NOT NULL DEFAULT 1")

                db.execSQL("UPDATE reminders SET musicUri = mediaUri, musicDisplayName = mediaDisplayName WHERE mediaType = 'MUSIC'")
                db.execSQL("UPDATE reminders SET imageUri = mediaUri, imageDisplayName = mediaDisplayName WHERE mediaType = 'IMAGE'")
                db.execSQL("UPDATE reminders SET videoUri = mediaUri, videoDisplayName = mediaDisplayName WHERE mediaType = 'VIDEO'")
                db.execSQL("UPDATE reminders SET useCustomInterval = 1, customIntervalUnit = 'DAYS', customIntervalValue = customIntervalDays WHERE recurrenceType = 'CUSTOM_DAYS'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `triggerAtMillis` INTEGER NOT NULL,
                        `recurrenceType` TEXT NOT NULL,
                        `useCustomInterval` INTEGER NOT NULL,
                        `customIntervalUnit` TEXT NOT NULL,
                        `customIntervalValue` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `musicUri` TEXT,
                        `musicDisplayName` TEXT,
                        `imageUri` TEXT,
                        `imageDisplayName` TEXT,
                        `videoUri` TEXT,
                        `videoDisplayName` TEXT,
                        `stickyColor` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        `lastTriggeredMillis` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_proofs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `taskId` INTEGER NOT NULL,
                        `statusAtProof` TEXT NOT NULL,
                        `imageUri` TEXT NOT NULL,
                        `imageDisplayName` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `capturedAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_proofs_taskId` ON `task_proofs` (`taskId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN checklistJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN checklistJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN checklistJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetKind` TEXT NOT NULL,
                        `defaultTitle` TEXT NOT NULL,
                        `defaultBody` TEXT NOT NULL,
                        `stickyColor` TEXT NOT NULL,
                        `recurrenceType` TEXT NOT NULL,
                        `useCustomInterval` INTEGER NOT NULL,
                        `customIntervalUnit` TEXT NOT NULL,
                        `customIntervalValue` INTEGER NOT NULL,
                        `checklistJson` TEXT NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN musicUri TEXT")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN musicDisplayName TEXT")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN imageUri TEXT")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN imageDisplayName TEXT")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN videoUri TEXT")
                db.execSQL("ALTER TABLE sticky_notes ADD COLUMN videoDisplayName TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daypin365.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
