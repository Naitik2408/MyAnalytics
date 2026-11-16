package com.lifeanalytics.app

import com.lifeanalytics.app.app.PrimaryDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationTest {
    @Test
    fun primaryNavigationDestinations_matchProductSpecification() {
        val routes = PrimaryDestination.entries.map { it.route }
        assertEquals(listOf("home", "finance", "activity", "insights"), routes)
    }
}
