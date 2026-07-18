package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class, Relationship::class, FamilyGroup::class], version = 3, exportSchema = false)
abstract class FamilyDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // Create family_groups table if it does not exist
                    db.execSQL("CREATE TABLE IF NOT EXISTS `family_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT)")
                } catch (e: android.database.sqlite.SQLiteException) {
                    android.util.Log.e("FamilyDatabase", "Error creating family_groups table in migration 1_2", e)
                }

                try {
                    // Add groupId column to persons table
                    db.execSQL("ALTER TABLE `persons` ADD COLUMN `groupId` INTEGER DEFAULT NULL")
                } catch (e: android.database.sqlite.SQLiteException) {
                    android.util.Log.e("FamilyDatabase", "Error adding groupId column to persons in migration 1_2", e)
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.i("FamilyDatabase", "Running MIGRATION_2_3 (no-op database schema change, logic updates only)")
            }
        }

        fun getDatabase(context: Context): FamilyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FamilyDatabase::class.java,
                    "family_tree_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
