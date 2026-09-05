package com.example.utils

import com.example.data.Person
import com.example.data.Relationship
import org.junit.Assert.*
import org.junit.Test

class FamilyReportHelperTest {

    @Test
    fun testEmptyPersonsSummary() {
        val summary = FamilyReportHelper.calculateSummary(emptyList(), emptyList())
        assertEquals(0, summary.totalCount)
        assertEquals(0, summary.maleCount)
        assertEquals(0, summary.femaleCount)
        assertEquals(0, summary.totalGenerations)
        assertNull(summary.averageAge)
    }

    @Test
    fun testMultiGenerationFamilySummary() {
        val grandfather = Person(id = 1, firstName = "محمد", lastName = "صادقی", gender = "Male", generation = 0, birthDate = "1320-01-01", birthPlace = "تهران", occupation = "معلم")
        val grandmother = Person(id = 2, firstName = "فاطمه", lastName = "احمدی", gender = "Female", generation = 0, birthDate = "1325-05-10", isDeceased = true, deathDate = "1400-02-15")
        
        val son = Person(id = 3, firstName = "حسین", lastName = "صادقی", gender = "Male", generation = 1, birthDate = "1350-03-20", birthPlace = "تهران", occupation = "مهندس")
        val daughterInLaw = Person(id = 4, firstName = "نرگس", lastName = "کریمی", gender = "Female", generation = 1, birthDate = "1355-08-12")
        
        val grandson = Person(id = 5, firstName = "علی", lastName = "صادقی", gender = "Male", generation = 2, birthDate = "1380-06-15", birthPlace = "شیراز")

        val persons = listOf(grandfather, grandmother, son, daughterInLaw, grandson)
        val rels = listOf(
            Relationship(id = 1, personId1 = grandfather.id, personId2 = grandmother.id, type = "Spouse"),
            Relationship(id = 2, personId1 = grandfather.id, personId2 = son.id, type = "Parent-Child"),
            Relationship(id = 3, personId1 = son.id, personId2 = daughterInLaw.id, type = "Spouse"),
            Relationship(id = 4, personId1 = son.id, personId2 = grandson.id, type = "Parent-Child")
        )

        val summary = FamilyReportHelper.calculateSummary(persons, rels)

        assertEquals(5, summary.totalCount)
        assertEquals(3, summary.maleCount)
        assertEquals(2, summary.femaleCount)
        assertEquals(3, summary.totalGenerations) // generation 0, 1, 2 -> 3 generations
        assertEquals(4, summary.livingCount)
        assertEquals(1, summary.deceasedCount)
        assertEquals(2, summary.bridesCount) // grandmother and daughterInLaw are brides
        assertEquals(1, summary.childrenCount) // son
        assertEquals(1, summary.grandchildrenCount) // grandson
        assertNotNull(summary.topBirthPlace)
        assertTrue(summary.topBirthPlace!!.contains("تهران"))
        assertNotNull(summary.averageAge)
    }
}
