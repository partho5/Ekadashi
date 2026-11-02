package com.jovoc.ekadashi

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure Kotlin astronomical solar sunrise calculator based on NOAA Solar Calculation algorithms.
 * Requires zero external dependencies.
 */
object SunriseCalculator {

    /**
     * Calculates the sunrise time for a given [date], [lat] (latitude in degrees),
     * [lon] (longitude in degrees), and [zone].
     *
     * Standard zenith of 90.833° is used (accounting for atmospheric refraction and solar disc diameter).
     */
    fun calculateSunrise(
        date: LocalDate,
        lat: Double = 23.8103, // Default Dhaka
        lon: Double = 90.4125,
        zone: ZoneId = ZoneId.of("Asia/Dhaka")
    ): ZonedDateTime {
        val dayOfYear = date.dayOfYear
        // Fractional year in radians
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1 + (6.0 - 12.0) / 24.0)

        // Equation of time in minutes
        val eqtime = 229.18 * (
            0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
        )

        // Solar declination in radians
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma)
            - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma)
            - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val latRad = Math.toRadians(lat)
        val zenithRad = Math.toRadians(90.833)

        // Hour angle
        val cosHa = (cos(zenithRad) / (cos(latRad) * cos(decl))) - (tan(latRad) * tan(decl))

        val haRad = when {
            cosHa > 1.0 -> 0.0 // Polar night
            cosHa < -1.0 -> Math.PI // Midnight sun
            else -> acos(cosHa)
        }
        val haDeg = Math.toDegrees(haRad)

        // Sunrise time in UTC minutes from midnight
        val timeUtcMinutes = 720.0 - 4.0 * (lon + haDeg) - eqtime

        val secondsFromMidnight = (timeUtcMinutes * 60.0).toLong()
        val utcMidnight = date.atStartOfDay(ZoneOffset.UTC)
        val sunriseUtc = utcMidnight.plusSeconds(secondsFromMidnight)

        return sunriseUtc.withZoneSameInstant(zone)
    }
}
