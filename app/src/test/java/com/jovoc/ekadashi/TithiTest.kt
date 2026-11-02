package com.jovoc.ekadashi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TithiTest {

    private val sampleJson = """
    {
      "timezone": "Bangladesh Standard Time (UTC+06:00)",
      "latitude": 23.8103,
      "longitude": 90.4125,
      "tithis": [
        {
          "tithi": "সপ্তমী",
          "start": "2026-10-31 21:56:52",
          "end": "2026-11-01 19:36:49"
        },
        {
          "tithi": "অষ্টমী",
          "start": "2026-11-01 19:36:49",
          "end": "2026-11-02 17:24:40"
        },
        {
          "tithi": "নবমী",
          "start": "2026-11-02 17:24:40",
          "end": "2026-11-03 15:25:04"
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testParseTithis() {
        val zone = ZoneId.of("Asia/Dhaka")
        val list = TithiHelper.parseTithisJson(sampleJson, zone)
        assertEquals(3, list.size)
        assertEquals(TithiType.SAPTAMI, list[0].type)
        assertEquals(TithiType.ASHTAMI, list[1].type)
        assertEquals(TithiType.NAVAMI, list[2].type)
    }

    @Test
    fun testComputeDayScheduleOnNov2() {
        val zone = ZoneId.of("Asia/Dhaka")
        val list = TithiHelper.parseTithisJson(sampleJson, zone)
        val date = LocalDate.of(2026, 11, 2)

        // At 12:00 PM on Nov 2, Ashtami is active (ends at 17:24:40)
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 11, 2, 12, 0, 0), zone)

        val schedule = TithiHelper.computeDaySchedule(
            date = date,
            allTithis = list,
            targetZone = zone,
            now = now,
            lang = "en"
        )

        assertNotNull(schedule)
        schedule!!

        // On Nov 2 from sunrise (~6:05 AM) to sunrise next day (~6:06 AM):
        // Ashtami runs from Sunrise to 17:24:40.
        // Nabami runs from 17:24:40 to Sunrise next day.
        assertEquals(2, schedule.segments.size)
        assertEquals(TithiType.ASHTAMI, schedule.segments[0].tithi.type)
        assertEquals(TithiType.NAVAMI, schedule.segments[1].tithi.type)

        // Active segment should be Ashtami
        assertEquals(TithiType.ASHTAMI, schedule.activeSegment?.tithi?.type)
        assertEquals("Coconut", schedule.forbiddenFood)

        // Visual percent sum should be approximately 1.0 (100%)
        val totalVisual = schedule.segments.map { it.visualPercent }.sum()
        assertTrue("Sum of visual percent should be ~1.0 but was $totalVisual", kotlin.math.abs(totalVisual - 1.0f) < 0.01f)
    }

    @Test
    fun testForbiddenFoodInDifferentLanguages() {
        assertEquals("Coconut", TithiHelper.getForbiddenFood(TithiType.ASHTAMI, "en"))
        assertEquals("নারকেল", TithiHelper.getForbiddenFood(TithiType.ASHTAMI, "bn"))
        assertEquals("नारियल", TithiHelper.getForbiddenFood(TithiType.ASHTAMI, "hi"))

        assertEquals("Bottle Gourd (Lau)", TithiHelper.getForbiddenFood(TithiType.NAVAMI, "en"))
        assertEquals("লাউ", TithiHelper.getForbiddenFood(TithiType.NAVAMI, "bn"))
        assertEquals("लौकी", TithiHelper.getForbiddenFood(TithiType.NAVAMI, "hi"))
    }

    @Test
    fun testRealJsonFile() {
        val file = File("../data/tithis_1month.json")
        if (file.exists()) {
            val content = file.readText()
            val zone = ZoneId.of("Asia/Dhaka")
            val list = TithiHelper.parseTithisJson(content, zone)
            assertTrue("Expected records in tithis_1month.json", list.size > 20)
        }
    }
}
