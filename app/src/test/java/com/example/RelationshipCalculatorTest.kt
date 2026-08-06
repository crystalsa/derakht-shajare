package com.example

import com.example.data.Person
import com.example.data.Relationship
import com.example.utils.RelationshipCalculator
import org.junit.Assert.*
import org.junit.Test

class RelationshipCalculatorTest {

    private val father = Person(id = 1, firstName = "علی", lastName = "رضایی", gender = "Male")
    private val mother = Person(id = 2, firstName = "زهرا", lastName = "محمدی", gender = "Female")
    private val son = Person(id = 3, firstName = "رضا", lastName = "رضایی", gender = "Male")
    private val daughter = Person(id = 4, firstName = "مریم", lastName = "رضایی", gender = "Female")
    private val grandfather = Person(id = 5, firstName = "حسین", lastName = "رضایی", gender = "Male")
    private val uncle = Person(id = 6, firstName = "حسن", lastName = "رضایی", gender = "Male")
    private val cousin = Person(id = 7, firstName = "محمد", lastName = "رضایی", gender = "Male")

    @Test
    fun testGetDirectedRelation_parentAndChild() {
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = son.id, type = "Parent-Child")
        )

        // Directed relation from father to son -> SON
        val relFromFatherToSon = RelationshipCalculator.getDirectedRelation(father, son, rels)
        assertEquals("SON", relFromFatherToSon)

        // Directed relation from son to father -> FATHER
        val relFromSonToFather = RelationshipCalculator.getDirectedRelation(son, father, rels)
        assertEquals("FATHER", relFromSonToFather)
    }

    @Test
    fun testGetDirectedRelation_spouse() {
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = mother.id, type = "Spouse")
        )

        val relFromFatherToMother = RelationshipCalculator.getDirectedRelation(father, mother, rels)
        assertEquals("SPOUSE", relFromFatherToMother)

        val relFromMotherToFather = RelationshipCalculator.getDirectedRelation(mother, father, rels)
        assertEquals("SPOUSE", relFromMotherToFather)
    }

    @Test
    fun testGetDirectedRelation_adoptiveParentChild() {
        val adoptiveChild = Person(id = 10, firstName = "سارا", lastName = "احمدی", gender = "Female")
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = adoptiveChild.id, type = "Adoptive-Parent-Child")
        )

        val relToChild = RelationshipCalculator.getDirectedRelation(father, adoptiveChild, rels)
        assertEquals("ADOPTIVE_DAUGHTER", relToChild)

        val relToFather = RelationshipCalculator.getDirectedRelation(adoptiveChild, father, rels)
        assertEquals("ADOPTIVE_FATHER", relToFather)
    }

    @Test
    fun testFindShortestPath_directRelation() {
        val allPersons = listOf(father, son)
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = son.id, type = "Parent-Child")
        )

        val path = RelationshipCalculator.findShortestPath(father, son, allPersons, rels)
        assertNotNull(path)
        assertEquals(2, path!!.size)
        assertEquals(father.id, path[0].first.id)
        assertEquals("Start", path[0].second)
        assertEquals(son.id, path[1].first.id)
        assertEquals("SON", path[1].second)
    }

    @Test
    fun testFindShortestPath_grandparentToGrandchild() {
        val allPersons = listOf(grandfather, father, son)
        val rels = listOf(
            Relationship(id = 1, personId1 = grandfather.id, personId2 = father.id, type = "Parent-Child"),
            Relationship(id = 2, personId1 = father.id, personId2 = son.id, type = "Parent-Child")
        )

        val path = RelationshipCalculator.findShortestPath(grandfather, son, allPersons, rels)
        assertNotNull(path)
        assertEquals(3, path!!.size)
        assertEquals(grandfather.id, path[0].first.id)
        assertEquals(father.id, path[1].first.id)
        assertEquals("SON", path[1].second)
        assertEquals(son.id, path[2].first.id)
        assertEquals("SON", path[2].second)
    }

    @Test
    fun testFindShortestPath_disconnectedMembers() {
        val stranger = Person(id = 99, firstName = "کامران", lastName = "کریمی", gender = "Male")
        val allPersons = listOf(father, stranger)
        val rels = emptyList<Relationship>()

        val path = RelationshipCalculator.findShortestPath(father, stranger, allPersons, rels)
        assertNull(path)
    }

    @Test
    fun testGetRelationshipLabel_fatherAndSon() {
        val allPersons = listOf(father, son)
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = son.id, type = "Parent-Child")
        )

        val label = RelationshipCalculator.getRelationshipLabel(father, son, allPersons, rels)
        assertTrue("Label should indicate father: $label", label.contains("پدر") || label.contains("پدرِ"))
    }

    @Test
    fun testGetRelationshipLabel_siblings() {
        val allPersons = listOf(father, son, daughter)
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = son.id, type = "Parent-Child"),
            Relationship(id = 2, personId1 = father.id, personId2 = daughter.id, type = "Parent-Child")
        )

        val sonToDaughter = RelationshipCalculator.getRelationshipLabel(son, daughter, allPersons, rels)
        assertTrue("Son should be brother to daughter: $sonToDaughter", sonToDaughter.contains("برادر"))

        val daughterToSon = RelationshipCalculator.getRelationshipLabel(daughter, son, allPersons, rels)
        assertTrue("Daughter should be sister to son: $daughterToSon", daughterToSon.contains("خواهر"))
    }

    @Test
    fun testGetBloodRelationshipNameBetweenSpouses_cousins() {
        // Grandfather -> Father & Uncle
        // Father -> Son
        // Uncle -> Cousin
        // Son & Cousin marry!
        val spouseA = son
        val spouseB = Person(id = 8, firstName = "مریم", lastName = "رضایی", gender = "Female") // female cousin

        val allPersons = listOf(grandfather, father, uncle, spouseA, spouseB)
        val rels = listOf(
            Relationship(id = 1, personId1 = grandfather.id, personId2 = father.id, type = "Parent-Child"),
            Relationship(id = 2, personId1 = grandfather.id, personId2 = uncle.id, type = "Parent-Child"),
            Relationship(id = 3, personId1 = father.id, personId2 = spouseA.id, type = "Parent-Child"),
            Relationship(id = 4, personId1 = uncle.id, personId2 = spouseB.id, type = "Parent-Child"),
            Relationship(id = 5, personId1 = spouseA.id, personId2 = spouseB.id, type = "Spouse")
        )

        val bloodRel = RelationshipCalculator.getBloodRelationshipNameBetweenSpouses(spouseA, spouseB, allPersons, rels)
        assertNotNull("Blood relationship between cousins should be detected", bloodRel)
        assertTrue("Blood relation label should contain cousin term: $bloodRel", bloodRel!!.contains("دخترعمو") || bloodRel.contains("پسرعمو"))
    }
}
