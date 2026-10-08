package io.github.vaishnavavrata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RemindersTest {

    private val dhakaZone: ZoneId = ZoneId.of("Asia/Dhaka")

    @Test
    fun testCalculateRemindersTriggerTimesAndKinds() {
        // Base setup: Now is Oct 1, 2026 00:00 Dhaka time
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 1), LocalTime.of(0, 0), dhakaZone)
        val vrataDate = LocalDate.of(2026, 10, 13)
        val parana = Parana(date = LocalDate.of(2026, 10, 14), start = LocalTime.of(6, 12), end = LocalTime.of(9, 24))
        val vrata = Vrata(type = "ekadashi", date = vrataDate, parana = parana, category = CATEGORY_EKADASHI)

        val reminders = calculateReminders(listOf(vrata), dhakaZone, now)
        assertEquals(3, reminders.size)

        val twoDay = reminders.first { it.kind == ReminderKind.TWO_DAY }
        val expectedTwoDayTime = ZonedDateTime.of(LocalDate.of(2026, 10, 11), LocalTime.of(8, 0), dhakaZone)
        assertEquals(expectedTwoDayTime.toInstant().toEpochMilli(), twoDay.triggerAtMillis)

        val oneDay = reminders.first { it.kind == ReminderKind.ONE_DAY }
        val expectedOneDayTime = ZonedDateTime.of(LocalDate.of(2026, 10, 12), LocalTime.of(20, 0), dhakaZone)
        assertEquals(expectedOneDayTime.toInstant().toEpochMilli(), oneDay.triggerAtMillis)

        val paranaReminder = reminders.first { it.kind == ReminderKind.PARANA }
        val expectedParanaTime = ZonedDateTime.of(LocalDate.of(2026, 10, 13), LocalTime.of(20, 0), dhakaZone)
        assertEquals(expectedParanaTime.toInstant().toEpochMilli(), paranaReminder.triggerAtMillis)
    }

    @Test
    fun test60DayWindowLimit() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 1), LocalTime.of(0, 0), dhakaZone)

        val withinWindow = Vrata(type = "ekadashi", date = LocalDate.of(2026, 10, 20), parana = null, category = CATEGORY_EKADASHI)
        val beyondWindow = Vrata(type = "janmashtami", date = LocalDate.of(2026, 12, 10), parana = null, category = CATEGORY_EKADASHI)

        val reminders = calculateReminders(listOf(withinWindow, beyondWindow), dhakaZone, now)
        assertTrue(reminders.all { it.vrataDate == LocalDate.of(2026, 10, 20) })
    }

    @Test
    fun testPastRemindersDropped() {
        // Now is Oct 12, 2026 at 12:00
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 12), LocalTime.of(12, 0), dhakaZone)
        val vrataDate = LocalDate.of(2026, 10, 13)
        val parana = Parana(date = LocalDate.of(2026, 10, 14), start = LocalTime.of(6, 12), end = null)
        val vrata = Vrata(type = "ekadashi", date = vrataDate, parana = parana, category = CATEGORY_EKADASHI)

        val reminders = calculateReminders(listOf(vrata), dhakaZone, now)

        // 2-day reminder was Oct 11 08:00 (past relative to Oct 12 12:00) -> dropped
        assertFalse(reminders.any { it.kind == ReminderKind.TWO_DAY })

        // 1-day reminder is Oct 12 20:00 (future) -> included
        assertTrue(reminders.any { it.kind == ReminderKind.ONE_DAY })

        // Parana reminder is Oct 13 20:00 (future) -> included
        assertTrue(reminders.any { it.kind == ReminderKind.PARANA })
    }

    @Test
    fun testParanaOnlyWhenStartOrEndPresent() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 1), LocalTime.of(0, 0), dhakaZone)
        val vrataNoParana = Vrata(type = "ekadashi", date = LocalDate.of(2026, 10, 15), parana = null, category = CATEGORY_EKADASHI)

        val reminders = calculateReminders(listOf(vrataNoParana), dhakaZone, now)
        assertEquals(2, reminders.size)
        assertFalse(reminders.any { it.kind == ReminderKind.PARANA })
    }

    @Test
    fun testStableIdGeneration() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 1), LocalTime.of(0, 0), dhakaZone)
        val vrata = Vrata(type = "ekadashi", date = LocalDate.of(2026, 10, 13), parana = null, category = CATEGORY_EKADASHI)

        val list1 = calculateReminders(listOf(vrata), dhakaZone, now)
        val list2 = calculateReminders(listOf(vrata), dhakaZone, now)

        assertEquals(list1.size, list2.size)
        assertEquals(list1[0].id, list2[0].id)
    }

    @Test
    fun testNonEkadashiHasNoReminders() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 1), LocalTime.of(0, 0), dhakaZone)
        val vrata = Vrata(type = "gaura_purnima", date = LocalDate.of(2026, 10, 15), parana = null, category = CATEGORY_OTHER)
        assertTrue(calculateReminders(listOf(vrata), dhakaZone, now).isEmpty())
    }

    @Test
    fun testParanaReminderWithEndOnly() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 1), LocalTime.of(0, 0), dhakaZone)
        val parana = Parana(date = LocalDate.of(2026, 10, 16), start = null, end = LocalTime.of(9, 28))
        val vrata = Vrata(type = "ekadashi", date = LocalDate.of(2026, 10, 15), parana = parana, category = CATEGORY_EKADASHI)
        assertTrue(calculateReminders(listOf(vrata), dhakaZone, now).any { it.kind == ReminderKind.PARANA })
    }
}
