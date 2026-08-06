package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.FamilyDao
import com.example.data.FamilyDatabase
import com.example.data.FamilyGroup
import com.example.data.Person
import com.example.data.Relationship
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyDaoTest {

    private lateinit var db: FamilyDatabase
    private lateinit var dao: FamilyDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FamilyDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.familyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndGetPerson() = runBlocking {
        val person = Person(
            firstName = "علی",
            lastName = "حسینی",
            gender = "Male",
            occupation = "مهندس"
        )
        val id = dao.insertPerson(person)
        assertTrue(id > 0)

        val retrieved = dao.getPersonById(id)
        assertNotNull(retrieved)
        assertEquals("علی", retrieved!!.firstName)
        assertEquals("حسینی", retrieved.lastName)
        assertEquals("مهندس", retrieved.occupation)
    }

    @Test
    fun testUpdateAndDeletePerson() = runBlocking {
        val person = Person(firstName = "زهرا", lastName = "احمدی", gender = "Female")
        val id = dao.insertPerson(person)

        val inserted = dao.getPersonById(id)!!
        val updated = inserted.copy(lastName = "احمدی‌پور", occupation = "پزشک")
        dao.updatePerson(updated)

        val retrieved = dao.getPersonById(id)!!
        assertEquals("احمدی‌پور", retrieved.lastName)
        assertEquals("پزشک", retrieved.occupation)

        dao.deletePerson(retrieved)
        val afterDelete = dao.getPersonById(id)
        assertNull(afterDelete)
    }

    @Test
    fun testGetAllPersons_orderedByLastNameFirstName() = runBlocking {
        dao.insertPerson(Person(firstName = "رضا", lastName = "باقری", gender = "Male"))
        dao.insertPerson(Person(firstName = "احمد", lastName = "آزادی", gender = "Male"))
        dao.insertPerson(Person(firstName = "امید", lastName = "آزادی", gender = "Male"))

        val persons = dao.getAllPersons().first()
        assertEquals(3, persons.size)
        assertEquals("احمد", persons[0].firstName)
        assertEquals("امید", persons[1].firstName)
        assertEquals("رضا", persons[2].firstName)
    }

    @Test
    fun testRelationshipOperations() = runBlocking {
        val p1Id = dao.insertPerson(Person(firstName = "پدر", lastName = "تست", gender = "Male"))
        val p2Id = dao.insertPerson(Person(firstName = "پسر", lastName = "تست", gender = "Male"))

        val rel = Relationship(personId1 = p1Id, personId2 = p2Id, type = "Parent-Child")
        val relId = dao.insertRelationship(rel)
        assertTrue(relId > 0)

        val rels = dao.getAllRelationships().first()
        assertEquals(1, rels.size)
        assertEquals("Parent-Child", rels[0].type)

        dao.deleteRelationshipsForPerson(p1Id)
        val relsAfterDelete = dao.getAllRelationships().first()
        assertTrue(relsAfterDelete.isEmpty())
    }

    @Test
    fun testFamilyGroupOperations() = runBlocking {
        val group1 = FamilyGroup(name = "گروه ب", displayOrder = 2)
        val group2 = FamilyGroup(name = "گروه آ", displayOrder = 1)

        val g1Id = dao.insertGroup(group1)
        val g2Id = dao.insertGroup(group2)

        val groups = dao.getAllGroups().first()
        assertEquals(2, groups.size)
        assertEquals("گروه آ", groups[0].name) // Ordered by displayOrder
        assertEquals("گروه ب", groups[1].name)

        // Assign group to person
        val personId = dao.insertPerson(Person(firstName = "عضو", lastName = "گروه", gender = "Male", groupId = g2Id))
        val personWithGroup = dao.getPersonById(personId)!!
        assertEquals(g2Id, personWithGroup.groupId)

        // Dissociate group from person
        dao.removeGroupAssociationFromPersons(g2Id)
        val personAfterRemove = dao.getPersonById(personId)!!
        assertNull(personAfterRemove.groupId)
    }

    @Test
    fun testDeleteGroupCascades() = runBlocking {
        val groupId = dao.insertGroup(FamilyGroup(name = "گروه حذف", displayOrder = 1))

        val p1Id = dao.insertPerson(Person(firstName = "عضو۱", lastName = "تست", gender = "Male", groupId = groupId))
        val p2Id = dao.insertPerson(Person(firstName = "عضو۲", lastName = "تست", gender = "Female", groupId = groupId))

        dao.insertRelationship(Relationship(personId1 = p1Id, personId2 = p2Id, type = "Spouse"))

        // Delete relationships for group, then delete persons for group
        dao.deleteRelationshipsForGroup(groupId)
        dao.deletePersonsForGroup(groupId)

        val rels = dao.getAllRelationships().first()
        assertTrue(rels.isEmpty())

        val p1 = dao.getPersonById(p1Id)
        assertNull(p1)
    }
}
