package com.lifeanalytics.app

import com.lifeanalytics.app.app.PrimaryDestination
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationTest {
    @Test
    fun primaryNavigationDestinations_matchProductSpecification() {
        val routes = PrimaryDestination.entries.map { it.route }
        assertEquals(listOf("home", "health", "career", "finance"), routes)
    }

    @Test
    fun yearProgress_normalAndLeapYears_calculatesDynamically() {
        val jan1 = DateAttributionPolicy.yearProgress(LocalDate.of(2026, 1, 1))
        assertEquals(1, jan1.daysPassed)
        assertEquals(364, jan1.daysRemaining)
        assertEquals(365, jan1.totalDaysInYear)

        val jun1 = DateAttributionPolicy.yearProgress(LocalDate.of(2026, 6, 1))
        assertEquals(152, jun1.daysPassed)
        assertEquals(213, jun1.daysRemaining)

        val nov20 = DateAttributionPolicy.yearProgress(LocalDate.of(2026, 11, 20))
        assertEquals(324, nov20.daysPassed)
        assertEquals(41, nov20.daysRemaining)

        val dec31 = DateAttributionPolicy.yearProgress(LocalDate.of(2026, 12, 31))
        assertEquals(365, dec31.daysPassed)
        assertEquals(0, dec31.daysRemaining)

        // Leap year (2028 has 366 days)
        val leapFeb29 = DateAttributionPolicy.yearProgress(LocalDate.of(2028, 2, 29))
        assertEquals(60, leapFeb29.daysPassed)
        assertEquals(306, leapFeb29.daysRemaining)
        assertEquals(366, leapFeb29.totalDaysInYear)

        val leapDec31 = DateAttributionPolicy.yearProgress(LocalDate.of(2028, 12, 31))
        assertEquals(366, leapDec31.daysPassed)
        assertEquals(0, leapDec31.daysRemaining)
    }
}

