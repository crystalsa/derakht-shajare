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
                } catch (e: Exception) {
                    android.util.Log.e("FamilyDatabase", "Error creating family_groups table: ${e.localizedMessage}")
                }

                try {
                    // Add groupId column to persons table
                    db.execSQL("ALTER TABLE `persons` ADD COLUMN `groupId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.e("FamilyDatabase", "Error adding groupId column to persons: ${e.localizedMessage}")
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // Create family_groups table if it does not exist
                    db.execSQL("CREATE TABLE IF NOT EXISTS `family_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT)")
                } catch (e: Exception) {
                    android.util.Log.e("FamilyDatabase", "Error creating family_groups table: ${e.localizedMessage}")
                }

                try {
                    // Add groupId column to persons table
                    db.execSQL("ALTER TABLE `persons` ADD COLUMN `groupId` INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.e("FamilyDatabase", "Error adding groupId column to persons: ${e.localizedMessage}")
                }
            }
        }

        fun getDatabase(context: Context): FamilyDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance: FamilyDatabase? = null
                try {
                    val dbBuilder = Room.databaseBuilder(
                        context.applicationContext,
                        FamilyDatabase::class.java,
                        "family_tree_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    
                    val db = dbBuilder.build()
                    // Force database opening and validation of the schema on the background thread/immediately
                    db.openHelper.writableDatabase
                    instance = db
                } catch (e: Exception) {
                    android.util.Log.e("FamilyDatabase", "Room migration or schema validation failed, recreating database: ${e.localizedMessage}")
                    try {
                        context.deleteDatabase("family_tree_database")
                        val db = Room.databaseBuilder(
                            context.applicationContext,
                            FamilyDatabase::class.java,
                            "family_tree_database"
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                        db.openHelper.writableDatabase
                        instance = db
                    } catch (ex: Exception) {
                        android.util.Log.e("FamilyDatabase", "Failed to recreate database: ${ex.localizedMessage}")
                    }
                }
                
                val finalInstance = instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FamilyDatabase::class.java,
                    "family_tree_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = finalInstance
                finalInstance
            }
        }
    }
}
