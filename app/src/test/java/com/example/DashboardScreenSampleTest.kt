package com.example

import org.junit.Test
import com.example.data.*
import com.example.ui.tree.computeTreeLayoutPositions
import androidx.compose.ui.graphics.Color

class DashboardScreenSampleTest {
    @Test
    fun testSeedDataLayout() {
        val persons = mutableListOf<Person>()
        val relationships = mutableListOf<Relationship>()
        
        var idCounter = 1L
        fun insertPerson(p: Person): Long {
            val pWithId = p.copy(id = idCounter++)
            persons.add(pWithId)
            return pWithId.id
        }
        
        fun insertRelationship(r: Relationship) {
            relationships.add(r.copy(id = idCounter++))
        }

        // Just copy the seedSampleData logic here to see if it crashes.
        val defaultGroupId = 1L
        
        val g1f = insertPerson(Person(firstName = "محمد", lastName = "علوی", gender = "Male"))
        val g1m = insertPerson(Person(firstName = "زهرا", lastName = "سادات", gender = "Female"))
        insertRelationship(Relationship(personId1 = g1f, personId2 = g1m, type = "Spouse"))

        val g2f = insertPerson(Person(firstName = "علی", lastName = "علوی", gender = "Male"))
        val g2m = insertPerson(Person(firstName = "مریم", lastName = "حسینی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g2f, personId2 = g2m, type = "Spouse"))
        insertRelationship(Relationship(personId1 = g1f, personId2 = g2f, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g1m, personId2 = g2f, type = "Parent-Child"))

        val g3f = insertPerson(Person(firstName = "حسین", lastName = "علوی", gender = "Male"))
        val g3m = insertPerson(Person(firstName = "فاطمه", lastName = "رضایی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g3f, personId2 = g3m, type = "Spouse"))
        insertRelationship(Relationship(personId1 = g2f, personId2 = g3f, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g2m, personId2 = g3f, type = "Parent-Child"))

        val g4f = insertPerson(Person(firstName = "رضا", lastName = "علوی", gender = "Male"))
        val g4m = insertPerson(Person(firstName = "لیلا", lastName = "محسنی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g4f, personId2 = g4m, type = "Spouse"))
        insertRelationship(Relationship(personId1 = g3f, personId2 = g4f, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g3m, personId2 = g4f, type = "Parent-Child"))

        val g4_uncle = insertPerson(Person(firstName = "محمود", lastName = "علوی", gender = "Male"))
        val g4_uncle_wife = insertPerson(Person(firstName = "نسترن", lastName = "کرمی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g4_uncle, personId2 = g4_uncle_wife, type = "Spouse"))
        insertRelationship(Relationship(personId1 = g3f, personId2 = g4_uncle, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g3m, personId2 = g4_uncle, type = "Parent-Child"))

        val g4_aunt = insertPerson(Person(firstName = "مینا", lastName = "علوی", gender = "Female"))
        val g4_aunt_husband = insertPerson(Person(firstName = "جواد", lastName = "صابری", gender = "Male"))
        insertRelationship(Relationship(personId1 = g4_aunt, personId2 = g4_aunt_husband, type = "Spouse"))
        insertRelationship(Relationship(personId1 = g3f, personId2 = g4_aunt, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g3m, personId2 = g4_aunt, type = "Parent-Child"))

        val g5f = insertPerson(Person(firstName = "حمید", lastName = "علوی", gender = "Male"))
        val g5m = insertPerson(Person(firstName = "الهام", lastName = "سهرابی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g5f, personId2 = g5m, type = "Spouse"))
        insertRelationship(Relationship(personId1 = g4f, personId2 = g5f, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g4m, personId2 = g5f, type = "Parent-Child"))

        val g5d = insertPerson(Person(firstName = "سارا", lastName = "علوی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g4f, personId2 = g5d, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g4m, personId2 = g5d, type = "Parent-Child"))

        val g5_cousin_f = insertPerson(Person(firstName = "مریم", lastName = "علوی", gender = "Female"))
        insertRelationship(Relationship(personId1 = g4_uncle, personId2 = g5_cousin_f, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g4_uncle_wife, personId2 = g5_cousin_f, type = "Parent-Child"))

        val g5_cousin_m = insertPerson(Person(firstName = "امیر", lastName = "صابری", gender = "Male"))
        insertRelationship(Relationship(personId1 = g4_aunt, personId2 = g5_cousin_m, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g4_aunt_husband, personId2 = g5_cousin_m, type = "Parent-Child"))

        val g5_brother = insertPerson(Person(firstName = "امید", lastName = "علوی", gender = "Male"))
        insertRelationship(Relationship(personId1 = g4f, personId2 = g5_brother, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g4m, personId2 = g5_brother, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g5_brother, personId2 = g5_cousin_f, type = "Spouse"))

        val g6_child1 = insertPerson(Person(firstName = "پرهام", lastName = "علوی", gender = "Male"))
        insertRelationship(Relationship(personId1 = g5_brother, personId2 = g6_child1, type = "Parent-Child"))
        insertRelationship(Relationship(personId1 = g5_cousin_f, personId2 = g6_child1, type = "Parent-Child"))

        insertRelationship(Relationship(personId1 = g5d, personId2 = g5_cousin_m, type = "Spouse"))

        try {
            val pos = computeTreeLayoutPositions(persons, relationships, "Vertical", null, emptySet())
            println("SUCCESS! pos.size = " + pos.size)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
