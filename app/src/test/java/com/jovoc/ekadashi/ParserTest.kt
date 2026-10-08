package com.jovoc.ekadashi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ParserTest {

    @Test
    fun testValidVratasParsing() {
        val json = """
            {
              "vratas": [
                {
                  "type": "ekadashi",
                  "date": "2026-10-13",
                  "parana": {
                    "date": "2026-10-14",
                    "start": "06:12",
                    "end": "09:24"
                  }
                }
              ]
            }
        """.trimIndent()

        val list = parseVratasJson(json)
        assertEquals(1, list.size)

        val vrata = list[0]
        assertEquals("ekadashi", vrata.type)
        assertEquals(LocalDate.of(2026, 10, 13), vrata.date)

        val parana = vrata.parana
        assertNotNull(parana)
        assertEquals(LocalDate.of(2026, 10, 14), parana!!.date)
        assertEquals(LocalTime.of(6, 12), parana.start)
        assertEquals(LocalTime.of(9, 24), parana.end)

        // Timezone conversion test: IST 06:12 -> Asia/Dhaka (UTC+6) should be 06:42
        val dhakaZone = ZoneId.of("Asia/Dhaka")
        val dhakaStart = parana.startIn(dhakaZone)
        assertNotNull(dhakaStart)
        assertEquals(LocalTime.of(6, 42), dhakaStart!!.toLocalTime())
    }

    @Test
    fun testNullParanaParsing() {
        val json = """
            {
              "vratas": [
                {
                  "type": "janmashtami",
                  "date": "2026-08-28",
                  "parana": null
                }
              ]
            }
        """.trimIndent()

        val list = parseVratasJson(json)
        assertEquals(1, list.size)
        assertEquals("janmashtami", list[0].type)
        assertNull(list[0].parana)
    }

    @Test
    fun testMissingParanaEndParsing() {
        val json = """
            {
              "vratas": [
                {
                  "type": "shiva_ratri",
                  "date": "2026-03-08",
                  "parana": {
                    "date": "2026-03-09",
                    "start": "06:00"
                  }
                }
              ]
            }
        """.trimIndent()

        val list = parseVratasJson(json)
        assertEquals(1, list.size)
        val parana = list[0].parana
        assertNotNull(parana)
        assertEquals(LocalTime.of(6, 0), parana!!.start)
        assertNull(parana.end)
    }

    @Test
    fun testMalformedEntriesSkipped() {
        val json = """
            {
              "vratas": [
                { "type": "", "date": "2026-10-13" },
                { "type": "valid", "date": "invalid-date" },
                "not-an-object",
                { "type": "valid_one", "date": "2026-10-15" }
              ]
            }
        """.trimIndent()

        val list = parseVratasJson(json)
        assertEquals(1, list.size)
        assertEquals("valid_one", list[0].type)
    }

    @Test
    fun testUnknownTypeKept() {
        val json = """
            {
              "vratas": [
                { "type": "custom_new_vrata", "date": "2026-12-25" }
              ]
            }
        """.trimIndent()

        val list = parseVratasJson(json)
        assertEquals(1, list.size)
        assertEquals("custom_new_vrata", list[0].type)
    }

    @Test
    fun testUnknownFieldsIgnored() {
        val json = """
            {
              "version": 2,
              "vratas": [
                {
                  "type": "ekadashi",
                  "date": "2026-10-13",
                  "extra_notes": "Special fast",
                  "parana": {
                    "date": "2026-10-14",
                    "start": "06:12",
                    "end": "09:24",
                    "foo": "bar"
                  }
                }
              ]
            }
        """.trimIndent()

        val list = parseVratasJson(json)
        assertEquals(1, list.size)
        assertEquals("ekadashi", list[0].type)
    }

    @Test
    fun testMalformedJsonReturnsEmptyList() {
        val list = parseVratasJson("invalid json content {{{")
        assertTrue(list.isEmpty())
    }

    @Test
    fun testCategoryNoteAndEndOnlyParana() {
        val json = """
        {
          "vratas": [
            { "type": "a", "category": "ekadashi", "date": "2027-03-19",
              "parana": { "date": "2027-03-20", "start": null, "end": "09:50" }, "note": "n" },
            { "type": "b", "date": "2027-03-22", "parana": null, "note": null }
          ]
        }
        """.trimIndent()
        val list = parseVratasJson(json)
        assertEquals(2, list.size)
        assertEquals(true, list[0].isEkadashi)
        assertEquals("n", list[0].note)
        assertEquals(null, list[0].parana?.start)
        assertEquals(java.time.LocalTime.of(9, 50), list[0].parana?.end)
        assertEquals(false, list[1].isEkadashi)
        assertEquals(null, list[1].note)
    }
}
