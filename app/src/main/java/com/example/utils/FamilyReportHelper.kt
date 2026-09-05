package com.example.utils

import com.example.data.Person
import com.example.data.Relationship
import com.example.ui.common.toFarsiNumbers
import com.example.ui.screens.getCurrentJalaliYear

data class FamilyReportSummary(
    val totalCount: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val totalGenerations: Int = 0,
    val livingCount: Int = 0,
    val deceasedCount: Int = 0,
    val bridesCount: Int = 0,
    val groomsCount: Int = 0,
    val childrenCount: Int = 0,
    val grandchildrenCount: Int = 0,
    val greatGrandchildrenCount: Int = 0,
    val greatGreatGrandchildrenCount: Int = 0,
    val averageAge: Int? = null,
    val topBirthPlace: String? = null,
    val topOccupation: String? = null
)

object FamilyReportHelper {
    fun calculateSummary(
        persons: List<Person>,
        relationships: List<Relationship>
    ): FamilyReportSummary {
        val totalCount = persons.size
        val maleCount = persons.count { it.gender == "Male" }
        val femaleCount = persons.count { it.gender == "Female" }

        val parentChildRels = relationships.filter { it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child" }
        val childrenIds = parentChildRels.map { it.personId2 }.toSet()
        val spouseRels = relationships.filter { it.type == "Spouse" }

        val maxGen = persons.maxOfOrNull { it.generation } ?: 0
        val totalGenerations = if (totalCount > 0) maxGen + 1 else 0

        val mainRoot = persons.filter { it.generation == 0 }.find { it.gender == "Male" }
            ?: persons.minByOrNull { it.generation }

        val brides = persons.filter { p ->
            p.id != mainRoot?.id &&
            p.gender == "Female" &&
            !childrenIds.contains(p.id) &&
            spouseRels.any { it.personId1 == p.id || it.personId2 == p.id }
        }

        val grooms = persons.filter { p ->
            p.id != mainRoot?.id &&
            p.gender == "Male" &&
            !childrenIds.contains(p.id) &&
            spouseRels.any { it.personId1 == p.id || it.personId2 == p.id }
        }

        val children = persons.filter { childrenIds.contains(it.id) && it.generation == 1 }
        val grandchildren = persons.filter { childrenIds.contains(it.id) && it.generation == 2 }
        val greatGrandchildren = persons.filter { childrenIds.contains(it.id) && it.generation == 3 }
        val greatGreatGrandchildren = persons.filter { childrenIds.contains(it.id) && it.generation == 4 }

        val livingCount = persons.count { !it.isDeceased }
        val deceasedCount = persons.count { it.isDeceased }

        val birthPlaces = persons.mapNotNull { it.birthPlace?.trim() }.filter { it.isNotEmpty() }
        val topBirthPlace = birthPlaces.groupBy { it }
            .entries.maxByOrNull { it.value.size }
            ?.let { "${it.key} (${it.value.size.toString().toFarsiNumbers()} نفر)" }

        val occupations = persons.mapNotNull { it.occupation?.trim() }.filter { it.isNotEmpty() }
        val topOccupation = occupations.groupBy { it }
            .entries.maxByOrNull { it.value.size }
            ?.let { "${it.key} (${it.value.size.toString().toFarsiNumbers()} نفر)" }

        val currentYear = getCurrentJalaliYear()
        val ageList = persons.mapNotNull { p ->
            val birthYearStr = p.birthDate?.split("-")?.firstOrNull()?.filter { it.isDigit() }
            val birthYear = birthYearStr?.toIntOrNull()
            if (birthYear != null) {
                if (p.isDeceased) {
                    val deathYearStr = p.deathDate?.split("-")?.firstOrNull()?.filter { it.isDigit() }
                    val deathYear = deathYearStr?.toIntOrNull()
                    if (deathYear != null) {
                        (deathYear - birthYear).coerceAtLeast(0)
                    } else null
                } else {
                    (currentYear - birthYear).coerceAtLeast(0)
                }
            } else null
        }
        val averageAge = if (ageList.isNotEmpty()) ageList.average().toInt() else null

        return FamilyReportSummary(
            totalCount = totalCount,
            maleCount = maleCount,
            femaleCount = femaleCount,
            totalGenerations = totalGenerations,
            livingCount = livingCount,
            deceasedCount = deceasedCount,
            bridesCount = brides.size,
            groomsCount = grooms.size,
            childrenCount = children.size,
            grandchildrenCount = grandchildren.size,
            greatGrandchildrenCount = greatGrandchildren.size,
            greatGreatGrandchildrenCount = greatGreatGrandchildren.size,
            averageAge = averageAge,
            topBirthPlace = topBirthPlace,
            topOccupation = topOccupation
        )
    }
}
