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
    fun testFindShortestPath_prefersDirectShortPathOverLongerPath() {
        // Between father and son, there is a direct Parent-Child path (length 2 nodes)
        // and a longer loop through grandfather (father -> grandfather -> other child -> son etc. or spouse)
        val allPersons = listOf(father, mother, son)
        val rels = listOf(
            Relationship(id = 1, personId1 = father.id, personId2 = mother.id, type = "Spouse"),
            Relationship(id = 2, personId1 = mother.id, personId2 = son.id, type = "Parent-Child"),
            Relationship(id = 3, personId1 = father.id, personId2 = son.id, type = "Parent-Child")
        )

        val shortestPath = RelationshipCalculator.findShortestPath(father, son, allPersons, rels)
        assertNotNull(shortestPath)
        // The direct link father -> son is 2 nodes (father, son)
        // The indirect link father -> mother -> son is 3 nodes
        assertEquals(2, shortestPath!!.size)
        assertEquals(father.id, shortestPath[0].first.id)
        assertEquals(son.id, shortestPath[1].first.id)
        assertEquals("SON", shortestPath[1].second)
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

    @Test
    fun testGetRelationshipLabel_brideAndGrandmother() {
        // Grandmother (زهرا) -> Father (علی) -> Son/Grandson (رضا) -> Bride/Wife (سارا)
        val grandmother = Person(id = 20, firstName = "زهرا", lastName = "صدر", gender = "Female")
        val fatherPerson = Person(id = 21, firstName = "علی", lastName = "رضایی", gender = "Male")
        val grandson = Person(id = 22, firstName = "رضا", lastName = "رضایی", gender = "Male")
        val bride = Person(id = 23, firstName = "سارا", lastName = "کاظمی", gender = "Female")

        val allPersons = listOf(grandmother, fatherPerson, grandson, bride)
        val rels = listOf(
            Relationship(id = 1, personId1 = grandmother.id, personId2 = fatherPerson.id, type = "Parent-Child"),
            Relationship(id = 2, personId1 = fatherPerson.id, personId2 = grandson.id, type = "Parent-Child"),
            Relationship(id = 3, personId1 = grandson.id, personId2 = bride.id, type = "Spouse")
        )

        // Bride relative to Grandmother: سارا عروس نوه‌ی زهرا هست
        val brideToGrandmother = RelationshipCalculator.getRelationshipLabel(bride, grandmother, allPersons, rels)
        assertTrue(
            "Bride should be recognized as daughter-in-law of grandson (عروس نوه): $brideToGrandmother",
            brideToGrandmother.contains("عروس") && (brideToGrandmother.contains("نوه") || brideToGrandmother.contains("نوه‌ی"))
        )

        // Grandmother relative to Bride: زهرا مادربزرگ همسرِ سارا هست
        val grandmotherToBride = RelationshipCalculator.getRelationshipLabel(grandmother, bride, allPersons, rels)
        assertTrue(
            "Grandmother should be recognized as spouse's grandmother (مادربزرگ همسر): $grandmotherToBride",
            (grandmotherToBride.contains("مادربزرگ") || grandmotherToBride.contains("مادر بزرگ")) && grandmotherToBride.contains("همسر")
        )
    }

    @Test
    fun testGetRelationshipLabel_groomAndGrandfather() {
        // Grandfather (حسین) -> Father (علی) -> Daughter/Granddaughter (مریم) -> Groom/Husband (پیمان)
        val grandfatherPerson = Person(id = 30, firstName = "حسین", lastName = "رضایی", gender = "Male")
        val fatherPerson = Person(id = 31, firstName = "علی", lastName = "رضایی", gender = "Male")
        val granddaughter = Person(id = 32, firstName = "مریم", lastName = "رضایی", gender = "Female")
        val groom = Person(id = 33, firstName = "پیمان", lastName = "کریمی", gender = "Male")

        val allPersons = listOf(grandfatherPerson, fatherPerson, granddaughter, groom)
        val rels = listOf(
            Relationship(id = 1, personId1 = grandfatherPerson.id, personId2 = fatherPerson.id, type = "Parent-Child"),
            Relationship(id = 2, personId1 = fatherPerson.id, personId2 = granddaughter.id, type = "Parent-Child"),
            Relationship(id = 3, personId1 = granddaughter.id, personId2 = groom.id, type = "Spouse")
        )

        // Groom relative to Grandfather
        val groomToGrandfather = RelationshipCalculator.getRelationshipLabel(groom, grandfatherPerson, allPersons, rels)
        assertTrue(
            "Groom should be recognized as son-in-law of grandchild (داماد نوه): $groomToGrandfather",
            groomToGrandfather.contains("داماد") && (groomToGrandfather.contains("نوه") || groomToGrandfather.contains("نوه‌ی"))
        )

        // Grandfather relative to Groom
        val grandfatherToGroom = RelationshipCalculator.getRelationshipLabel(grandfatherPerson, groom, allPersons, rels)
        assertTrue(
            "Grandfather should be recognized as spouse's grandfather (پدربزرگ همسر): $grandfatherToGroom",
            (grandfatherToGroom.contains("پدربزرگ") || grandfatherToGroom.contains("پدر بزرگ")) && grandfatherToGroom.contains("همسر")
        )
    }

    @Test
    fun testGetRelationshipLabel_uncleOfSpouseAndWifeOfNephew() {
        // Grandfather -> Uncle & Father
        // Father -> Nephew -> Wife
        val grandfatherPerson = Person(id = 40, firstName = "حسین", lastName = "رضایی", gender = "Male")
        val unclePerson = Person(id = 41, firstName = "حسن", lastName = "رضایی", gender = "Male")
        val fatherPerson = Person(id = 42, firstName = "علی", lastName = "رضایی", gender = "Male")
        val nephew = Person(id = 43, firstName = "رضا", lastName = "رضایی", gender = "Male")
        val nephewWife = Person(id = 44, firstName = "مهسا", lastName = "تهرانی", gender = "Female")

        val allPersons = listOf(grandfatherPerson, unclePerson, fatherPerson, nephew, nephewWife)
        val rels = listOf(
            Relationship(id = 1, personId1 = grandfatherPerson.id, personId2 = unclePerson.id, type = "Parent-Child"),
            Relationship(id = 2, personId1 = grandfatherPerson.id, personId2 = fatherPerson.id, type = "Parent-Child"),
            Relationship(id = 3, personId1 = fatherPerson.id, personId2 = nephew.id, type = "Parent-Child"),
            Relationship(id = 4, personId1 = nephew.id, personId2 = nephewWife.id, type = "Spouse")
        )

        // Uncle relative to Nephew's Wife
        val uncleToWife = RelationshipCalculator.getRelationshipLabel(unclePerson, nephewWife, allPersons, rels)
        assertTrue("Uncle of spouse: $uncleToWife", uncleToWife.contains("عموی") && uncleToWife.contains("همسر"))

        // Nephew's Wife relative to Uncle
        val wifeToUncle = RelationshipCalculator.getRelationshipLabel(nephewWife, unclePerson, allPersons, rels)
        assertTrue("Wife of nephew: $wifeToUncle", wifeToUncle.contains("زن") && wifeToUncle.contains("برادرزاده"))
    }
}
