package com.jovoc.ekadashi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SunriseCalculatorTest {

    @Test
    fun testDhakaSunriseOnNov2() {
        val date = LocalDate.of(2026, 11, 2)
        val zone = ZoneId.of("Asia/Dhaka")
        val sunrise = SunriseCalculator.calculateSunrise(date, 23.8103, 90.4125, zone)

        assertEquals(2026, sunrise.year)
        assertEquals(11, sunrise.monthValue)
        assertEquals(2, sunrise.dayOfMonth)
        assertEquals(6, sunrise.hour)
        // In Dhaka on Nov 2, sunrise is approximately 6:04 AM - 6:07 AM
        assertTrue("Sunrise minute ${sunrise.minute} should be between 3 and 8", sunrise.minute in 3..8)
    }

    @Test
    fun testKolkataSunriseOnNov2() {
        val date = LocalDate.of(2026, 11, 2)
        val zone = ZoneId.of("Asia/Kolkata")
        val sunrise = SunriseCalculator.calculateSunrise(date, 22.5726, 88.3639, zone)

        assertEquals(2026, sunrise.year)
        assertEquals(11, sunrise.monthValue)
        assertEquals(2, sunrise.dayOfMonth)
        // In Kolkata (IST) on Nov 2, sunrise is ~5:40 - 5:45 AM IST
        assertEquals(5, sunrise.hour)
        assertTrue("Sunrise minute ${sunrise.minute} should be between 38 and 46", sunrise.minute in 38..46)
    }
}
