package com.example.ui.screens

import java.util.Calendar
import com.example.ui.common.toFarsiNumbers

fun getCurrentJalaliYear(): Int {
    val cal = Calendar.getInstance()
    val gy = cal.get(Calendar.YEAR)
    val gm = cal.get(Calendar.MONTH) + 1
    val gd = cal.get(Calendar.DAY_OF_MONTH)
    
    val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
    
    var gy2 = gy - 1600
    var gm2 = gm - 1
    var gd2 = gd - 1
    
    var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
    for (i in 0 until gm2) {
        gDayNo += gDaysInMonth[i]
    }
    if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
        gDayNo++
    }
    gDayNo += gd2
    
    var jDayNo = gDayNo - 79
    val jNp = jDayNo / 12053
    jDayNo %= 12053
    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461
    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }
    return jy
}


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
    
    val result = if (isDeceased) {
        val dStr = if (!deathDate.isNullOrBlank()) deathDate else "؟"
        "\u202D$bStr - $dStr\u202C$ageStr"
    } else {
        if (!birthDate.isNullOrBlank()) {
             "\u202D$bStr\u202C$ageStr"
        } else ""
    }
    return result.toFarsiNumbers()
}

fun formatLifeYearsOnlyLTR(birthDate: String?, deathDate: String?, isDeceased: Boolean): String {
    val extractYear = { dateStr: String? ->
        if (dateStr.isNullOrBlank()) "؟"
        else dateStr.split("/", "-").firstOrNull()?.filter { it.isDigit() } ?: "؟"
    }
    
    val bYear = extractYear(birthDate)
    
    val result = if (isDeceased) {
        val dYear = extractYear(deathDate)
        "\u202D$bYear - $dYear\u202C"
    } else {
        if (!birthDate.isNullOrBlank()) {
             "متولد \u202D$bYear\u202C"
        } else ""
    }
    return result.toFarsiNumbers()
}
