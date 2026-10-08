package io.github.vaishnavavrata

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class ConfigTest {

    @Test
    fun testCountryFromCode() {
        assertEquals(Country.BD, Country.fromCode("BD"))
        assertEquals(Country.BD, Country.fromCode("bd"))
        assertEquals(Country.IN, Country.fromCode("IN"))
        assertEquals(Country.IN, Country.fromCode("in"))
        assertEquals(Country.OTHER, Country.fromCode("OTHER"))
        assertEquals(Country.OTHER, Country.fromCode("US"))
        assertEquals(Country.OTHER, Country.fromCode(null))
        assertEquals(Country.OTHER, Country.fromCode(""))
    }

    @Test
    fun testCountryGetTimezone() {
        val defaultTz = ZoneId.of("Europe/London")
        assertEquals(ZoneId.of("Asia/Dhaka"), Country.BD.getTimezone(defaultTz))
        assertEquals(ZoneId.of("Asia/Kolkata"), Country.IN.getTimezone(defaultTz))
        assertEquals(defaultTz, Country.OTHER.getTimezone(defaultTz))
    }

    @Test
    fun testInferCountryFromInputs() {
        // Step 1: network country wins
        assertEquals(
            Country.BD,
            Config.inferCountryFromInputs("BD", "IN", "IN", "Asia/Kolkata")
        )
        assertEquals(
            Country.IN,
            Config.inferCountryFromInputs("IN", "BD", "BD", "Asia/Dhaka")
        )
        assertEquals(
            Country.OTHER,
            Config.inferCountryFromInputs("US", "BD", "BD", "Asia/Dhaka")
        )

        // Step 2: SIM country fallback
        assertEquals(
            Country.BD,
            Config.inferCountryFromInputs(null, "BD", "IN", "Asia/Kolkata")
        )

        // Step 3: Locale country fallback
        assertEquals(
            Country.IN,
            Config.inferCountryFromInputs(null, null, "IN", "Asia/Dhaka")
        )

        // Step 4: Timezone fallback
        assertEquals(
            Country.BD,
            Config.inferCountryFromInputs(null, null, null, "Asia/Dhaka")
        )
        assertEquals(
            Country.IN,
            Config.inferCountryFromInputs(null, null, null, "Asia/Kolkata")
        )
        assertEquals(
            Country.IN,
            Config.inferCountryFromInputs(null, null, null, "Asia/Calcutta")
        )

        // Step 5: Fallback to OTHER
        assertEquals(
            Country.OTHER,
            Config.inferCountryFromInputs(null, null, null, "Europe/Paris")
        )
    }

    @Test
    fun testInferLanguageFromInputs() {
        // Explicitly supported languages
        assertEquals("bn", Config.inferLanguageFromInputs("bn", Country.OTHER))
        assertEquals("hi", Config.inferLanguageFromInputs("hi", Country.BD))
        assertEquals("en", Config.inferLanguageFromInputs("en", Country.IN))

        // Country default fallback for unsupported device language
        assertEquals("bn", Config.inferLanguageFromInputs("fr", Country.BD))
        assertEquals("hi", Config.inferLanguageFromInputs("de", Country.IN))
        assertEquals("en", Config.inferLanguageFromInputs("es", Country.OTHER))
        assertEquals("en", Config.inferLanguageFromInputs(null, Country.OTHER))
    }
}
