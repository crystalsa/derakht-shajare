package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [Person::class, Relationship::class, FamilyGroup::class, FamilyFolder::class], version = 4, exportSchema = false)
abstract class FamilyDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyDatabase? = null

        fun closeAndClearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `family_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT)")
                db.execSQL("ALTER TABLE `persons` ADD COLUMN `groupId` INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `family_groups` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `family_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parentId` INTEGER, `displayOrder` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("ALTER TABLE `family_groups` ADD COLUMN `folderId` INTEGER")

                val cursor = db.query("SELECT id, name, displayOrder FROM family_groups")
                cursor.use {
                    while (it.moveToNext()) {
                        val groupId = it.getLong(it.getColumnIndexOrThrow("id"))
                        val groupName = it.getString(it.getColumnIndexOrThrow("name"))
                        val order = it.getInt(it.getColumnIndexOrThrow("displayOrder"))
                        val values = android.content.ContentValues().apply {
                            put("name", groupName)
                            putNull("parentId")
                            put("displayOrder", order)
                        }
                        val newFolderId = db.insert("family_folders", android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT, values)
                        db.execSQL("UPDATE family_groups SET folderId = ? WHERE id = ?", arrayOf(newFolderId, groupId))
                    }
                }
            }
        }

        fun getDatabase(context: Context): FamilyDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = buildDatabase(context)
                    // Trigger an open to catch any corruption early
                    instance.openHelper.writableDatabase
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("FamilyDatabase", "Failed to open database. Attempting recovery.", e)
                    val dbFile = context.getDatabasePath("family_tree_database")
                    if (dbFile.exists()) {
                        val backupFile = File(dbFile.path + ".bak")
                        try {
                            dbFile.copyTo(backupFile, overwrite = true)
                            Log.d("FamilyDatabase", "Corrupt database backed up to ${backupFile.absolutePath}")
                        } catch (ex: Exception) {
                            Log.e("FamilyDatabase", "Failed to backup corrupt DB", ex)
                        }
                        context.deleteDatabase("family_tree_database")
                    }
                    val freshInstance = buildDatabase(context)
                    INSTANCE = freshInstance
                    freshInstance
                }
            }
        }

        private fun buildDatabase(context: Context): FamilyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FamilyDatabase::class.java,
                "family_tree_database"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
        }
    }
}
