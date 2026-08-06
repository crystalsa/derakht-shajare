package com.example

import com.example.ui.screens.calculateAge
import com.example.ui.screens.formatLifeDates
import com.example.ui.screens.formatLifeYearsOnlyLTR
import com.example.ui.screens.getCurrentJalaliYear
import com.example.ui.screens.validateBirthAndDeathDates
import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {

    @Test
    fun testGetCurrentJalaliYear_validRange() {
        val jalaliYear = getCurrentJalaliYear()
        assertTrue("Jalali year should be around 1400+", jalaliYear in 1400..1500)
    }

    @Test
    fun testValidateBirthAndDeathDates_validCases() {
        assertTrue(validateBirthAndDeathDates("1370/01/01", "1400/05/10"))
        assertTrue(validateBirthAndDeathDates("1370/05/10", "1370/05/10"))
        assertTrue(validateBirthAndDeathDates("1370/01/01", null))
        assertTrue(validateBirthAndDeathDates(null, "1400/05/10"))
        assertTrue(validateBirthAndDeathDates(null, null))
    }

    @Test
    fun testValidateBirthAndDeathDates_invalidCases() {
        assertFalse("Death year before birth year should be invalid", validateBirthAndDeathDates("1400/01/01", "1370/01/01"))
        assertFalse("Death month before birth month in same year should be invalid", validateBirthAndDeathDates("1400/05/10", "1400/02/10"))
        assertFalse("Death day before birth day in same month/year should be invalid", validateBirthAndDeathDates("1400/05/10", "1400/05/05"))
    }

    @Test
    fun testCalculateAge_livingPerson() {
        val currentYear = 1403
        val age = calculateAge("1370/05/15", null, currentYear)
        assertEquals(33, age)
    }

    @Test
    fun testCalculateAge_deceasedPerson() {
        val currentYear = 1403
        val age = calculateAge("1320/01/01", "1400/01/01", currentYear)
        assertEquals(80, age)
    }

    @Test
    fun testCalculateAge_nullBirthDate() {
        val age = calculateAge(null, "1400/01/01", 1403)
        assertNull(age)
    }

    @Test
    fun testFormatLifeDates_living() {
        val formatted = formatLifeDates("1370/01/01", null, isDeceased = false, currentYear = 1403)
        assertTrue(formatted.contains("1370/01/01"))
        assertTrue(formatted.contains("33 ساله"))
    }

    @Test
    fun testFormatLifeDates_deceased() {
        val formatted = formatLifeDates("1320/01/01", "1400/01/01", isDeceased = true, currentYear = 1403)
        assertTrue(formatted.contains("1320/01/01"))
        assertTrue(formatted.contains("1400/01/01"))
        assertTrue(formatted.contains("80 ساله"))
    }

    @Test
    fun testFormatLifeYearsOnlyLTR() {
        val livingYears = formatLifeYearsOnlyLTR("1370/05/12", null, isDeceased = false)
        assertTrue(livingYears.contains("1370"))
        assertTrue(livingYears.contains("متولد"))

        val deceasedYears = formatLifeYearsOnlyLTR("1320/01/01", "1400/05/10", isDeceased = true)
        assertTrue(deceasedYears.contains("1320"))
        assertTrue(deceasedYears.contains("1400"))
    }
}
