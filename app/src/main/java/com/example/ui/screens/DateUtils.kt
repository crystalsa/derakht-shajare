package com.example.ui.screens

fun validateBirthAndDeathDates(birthDate: String?, deathDate: String?): Boolean {
    if (birthDate.isNullOrBlank() || deathDate.isNullOrBlank()) return true
    
    val bParts = birthDate.split("/", "-").mapNotNull { it.toIntOrNull() }
    val dParts = deathDate.split("/", "-").mapNotNull { it.toIntOrNull() }
    
    val bYear = bParts.getOrNull(0)
    val bMonth = bParts.getOrNull(1) ?: 1
    val bDay = bParts.getOrNull(2) ?: 1
    
    val dYear = dParts.getOrNull(0)
    val dMonth = dParts.getOrNull(1) ?: 1
    val dDay = dParts.getOrNull(2) ?: 1
    
    if (bYear != null && dYear != null) {
        if (dYear < bYear) return false
        if (dYear == bYear) {
            if (dMonth < bMonth) return false
            if (dMonth == bMonth) {
                if (dDay < bDay) return false
            }
        }
    }
    return true
}

fun calculateAge(birthDate: String?, deathDate: String?, currentYear: Int): Int? {
    if (birthDate.isNullOrBlank()) return null
    val bYear = birthDate.split("/", "-").mapNotNull { it.toIntOrNull() }.firstOrNull() ?: return null
    if (!deathDate.isNullOrBlank()) {
        val dYear = deathDate.split("/", "-").mapNotNull { it.toIntOrNull() }.firstOrNull()
        if (dYear != null) return dYear - bYear
    }
    return currentYear - bYear
}

fun formatLifeDates(birthDate: String?, deathDate: String?, isDeceased: Boolean, currentYear: Int): String {
    val bStr = if (!birthDate.isNullOrBlank()) birthDate else "؟"
    val age = calculateAge(birthDate, if (isDeceased) deathDate else null, currentYear)
    val ageStr = if (age != null) " ($age ساله)" else ""
    
    return if (isDeceased) {
        val dStr = if (!deathDate.isNullOrBlank()) deathDate else "؟"
        "\u202D$bStr - $dStr\u202C$ageStr"
    } else {
        if (!birthDate.isNullOrBlank()) {
             "\u202D$bStr\u202C$ageStr"
        } else ""
    }
}

fun formatLifeYearsOnlyLTR(birthDate: String?, deathDate: String?, isDeceased: Boolean): String {
    val extractYear = { dateStr: String? ->
        if (dateStr.isNullOrBlank()) "؟"
        else dateStr.split("/", "-").firstOrNull()?.filter { it.isDigit() } ?: "؟"
    }
    
    val bYear = extractYear(birthDate)
    
    return if (isDeceased) {
        val dYear = extractYear(deathDate)
        "\u202D$bYear - $dYear\u202C"
    } else {
        if (!birthDate.isNullOrBlank()) {
             "متولد \u202D$bYear\u202C"
        } else ""
    }
}
