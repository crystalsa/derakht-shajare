package com.example.utils

import com.example.data.Person
import com.example.data.Relationship
import org.junit.Assert.*
import org.junit.Test

class SubtreeExtractorTest {

    @Test
    fun testSubtree_simpleThreeGenerationDirectLine() {
        val gp = Person(id = 1, firstName = "پدربزرگ", lastName = "احمدی", gender = "Male", generation = 1)
        val parent = Person(id = 2, firstName = "پدر", lastName = "احمدی", gender = "Male", generation = 2)
        val child = Person(id = 3, firstName = "فرزند", lastName = "احمدی", gender = "Male", generation = 3)

        val allPersons = listOf(gp, parent, child)
        val allRels = listOf(
            Relationship(id = 10, personId1 = gp.id, personId2 = parent.id, type = "Parent-Child"),
            Relationship(id = 11, personId1 = parent.id, personId2 = child.id, type = "Parent-Child")
        )

        val (subtreePersons, subtreeRels) = SubtreeExtractor.getSubtreePersonsAndRelationships(gp.id, allPersons, allRels)

        assertEquals(3, subtreePersons.size)
        assertTrue(subtreePersons.any { it.id == gp.id })
        assertTrue(subtreePersons.any { it.id == parent.id })
        assertTrue(subtreePersons.any { it.id == child.id })

        assertEquals(2, subtreeRels.size)
    }

    @Test
    fun testSubtree_twoSpousesAndChildrenFromEach() {
        val root = Person(id = 1, firstName = "علی", lastName = "مهرابی", gender = "Male", generation = 2)
        val spouse1 = Person(id = 2, firstName = "مریم", lastName = "اکبری", gender = "Female", generation = 2)
        val spouse2 = Person(id = 3, firstName = "سارا", lastName = "کاظمی", gender = "Female", generation = 2)

        val child1 = Person(id = 4, firstName = "رضا", lastName = "مهرابی", gender = "Male", generation = 3)
        val child2 = Person(id = 5, firstName = "نیلوفر", lastName = "مهرابی", gender = "Female", generation = 3)

        val allPersons = listOf(root, spouse1, spouse2, child1, child2)
        val allRels = listOf(
            Relationship(id = 101, personId1 = root.id, personId2 = spouse1.id, type = "Spouse"),
            Relationship(id = 102, personId1 = root.id, personId2 = spouse2.id, type = "SecondSpouse_Divorced"),
            Relationship(id = 103, personId1 = root.id, personId2 = child1.id, type = "Parent-Child"),
            Relationship(id = 104, personId1 = spouse1.id, personId2 = child1.id, type = "Parent-Child"),
            Relationship(id = 105, personId1 = root.id, personId2 = child2.id, type = "Parent-Child"),
            Relationship(id = 106, personId1 = spouse2.id, personId2 = child2.id, type = "Parent-Child")
        )

        val (subtreePersons, subtreeRels) = SubtreeExtractor.getSubtreePersonsAndRelationships(root.id, allPersons, allRels)

        assertEquals(5, subtreePersons.size)
        assertEquals(6, subtreeRels.size)
    }

    @Test
    fun testSubtree_isolatedNodeNoRelationships() {
        val isolated = Person(id = 99, firstName = "تنها", lastName = "تنهایی", gender = "Male", generation = 0)
        val (subtreePersons, subtreeRels) = SubtreeExtractor.getSubtreePersonsAndRelationships(isolated.id, listOf(isolated), emptyList())

        assertEquals(1, subtreePersons.size)
        assertEquals(isolated.id, subtreePersons.first().id)
        assertTrue(subtreeRels.isEmpty())
    }

    @Test
    fun testSubtree_excludesUnrelatedSiblingBranch() {
        val gp = Person(id = 1, firstName = "جد مشترک", lastName = "فامیل", gender = "Male", generation = 0)
        val siblingA = Person(id = 2, firstName = "برادر الف", lastName = "فامیل", gender = "Male", generation = 1)
        val siblingB = Person(id = 3, firstName = "برادر ب", lastName = "فامیل", gender = "Male", generation = 1)
        val childA = Person(id = 4, firstName = "فرزند الف", lastName = "فامیل", gender = "Male", generation = 2)
        val childB = Person(id = 5, firstName = "فرزند ب", lastName = "فامیل", gender = "Male", generation = 2)

        val allPersons = listOf(gp, siblingA, siblingB, childA, childB)
        val allRels = listOf(
            Relationship(id = 10, personId1 = gp.id, personId2 = siblingA.id, type = "Parent-Child"),
            Relationship(id = 11, personId1 = gp.id, personId2 = siblingB.id, type = "Parent-Child"),
            Relationship(id = 12, personId1 = siblingA.id, personId2 = childA.id, type = "Parent-Child"),
            Relationship(id = 13, personId1 = siblingB.id, personId2 = childB.id, type = "Parent-Child")
        )

        val (subtreeA, relsA) = SubtreeExtractor.getSubtreePersonsAndRelationships(childA.id, allPersons, allRels)
        assertEquals(1, subtreeA.size)
        assertEquals(childA.id, subtreeA.first().id)
        assertTrue(relsA.isEmpty())
    }
}
