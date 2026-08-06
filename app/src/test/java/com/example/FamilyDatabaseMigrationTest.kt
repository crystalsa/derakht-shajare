package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.FamilyDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyDatabaseMigrationTest {

    private val TEST_DB = "migration_test_db"

    @Before
    fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    private fun createV1Database(): SupportSQLiteDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `persons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `gender` TEXT NOT NULL, `birthDate` TEXT, `birthPlace` TEXT, `deathDate` TEXT, `deathPlace` TEXT, `isDeceased` INTEGER NOT NULL, `occupation` TEXT, `biography` TEXT, `photoUri` TEXT, `generation` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `relationships` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personId1` INTEGER NOT NULL, `personId2` INTEGER NOT NULL, `type` TEXT NOT NULL)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        return helper.writableDatabase
    }

    private fun createV2Database(): SupportSQLiteDatabase {
        val db = createV1Database()
        FamilyDatabase.MIGRATION_1_2.migrate(db)
        return db
    }

    @Test
    fun testMigrate1To2() {
        val db = createV1Database()
        db.execSQL("INSERT INTO persons (id, firstName, lastName, gender, isDeceased, generation) VALUES (1, 'احمد', 'رضایی', 'Male', 0, 0)")

        // Run migration 1 -> 2
        FamilyDatabase.MIGRATION_1_2.migrate(db)

        // Verify family_groups table was created
        val cursorGroups = db.query("SELECT * FROM family_groups")
        assertNotNull(cursorGroups)
        cursorGroups.close()

        // Verify persons table now has groupId column
        val cursorPersons = db.query("SELECT id, firstName, groupId FROM persons WHERE id = 1")
        assertTrue(cursorPersons.moveToFirst())
        assertEquals("احمد", cursorPersons.getString(cursorPersons.getColumnIndexOrThrow("firstName")))
        assertTrue(cursorPersons.isNull(cursorPersons.getColumnIndexOrThrow("groupId")))
        cursorPersons.close()
    }

    @Test
    fun testMigrate2To3() {
        val db = createV2Database()
        db.execSQL("INSERT INTO family_groups (id, name, description) VALUES (1, 'خاندان رضایی', 'توضیحات')")

        // Run migration 2 -> 3
        FamilyDatabase.MIGRATION_2_3.migrate(db)

        // Verify displayOrder column exists and defaults to 0
        val cursor = db.query("SELECT id, name, displayOrder FROM family_groups WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("خاندان رضایی", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("displayOrder")))
        cursor.close()
    }

    @Test
    fun testMigrate1To3Sequential() {
        val db = createV1Database()
        db.execSQL("INSERT INTO persons (id, firstName, lastName, gender, isDeceased, generation) VALUES (10, 'مریم', 'کاظمی', 'Female', 0, 1)")

        // Run migration 1 -> 2 then 2 -> 3
        FamilyDatabase.MIGRATION_1_2.migrate(db)
        FamilyDatabase.MIGRATION_2_3.migrate(db)

        val cursor = db.query("SELECT id, firstName, groupId FROM persons WHERE id = 10")
        assertTrue(cursor.moveToFirst())
        assertEquals("مریم", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
        cursor.close()

        val cursorGroups = db.query("SELECT * FROM family_groups")
        assertNotNull(cursorGroups)
        cursorGroups.close()
    }
}
