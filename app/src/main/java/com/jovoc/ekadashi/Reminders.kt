package com.jovoc.ekadashi

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ReminderKind {
    TWO_DAY,
    ONE_DAY,
    PARANA
}

data class Reminder(
    val id: Int,
    val type: String,
    val vrataDate: LocalDate,
    val triggerAtMillis: Long,
    val kind: ReminderKind
)

fun calculateReminders(
    vratas: List<Vrata>,
    zone: ZoneId,
    now: ZonedDateTime
): List<Reminder> {
    val today = now.toLocalDate()
    val windowEnd = today.plusDays(60)
    val nowMillis = now.toInstant().toEpochMilli()
    val reminders = mutableListOf<Reminder>()

    for (vrata in vratas) {
        // Only ekadashi vratas notify; other vratas are in-app only.
        if (!vrata.isEkadashi || vrata.date < today || vrata.date > windowEnd) {
            continue
        }

        // 1. TWO_DAY: 2 days before at 08:00
        val twoDayTime = ZonedDateTime.of(vrata.date.minusDays(2), LocalTime.of(8, 0), zone)
        val twoDayMillis = twoDayTime.toInstant().toEpochMilli()
        if (twoDayMillis > nowMillis) {
            val id = "${vrata.type}|${vrata.date}|${ReminderKind.TWO_DAY.name}".hashCode()
            reminders.add(Reminder(id, vrata.type, vrata.date, twoDayMillis, ReminderKind.TWO_DAY))
        }

        // 2. ONE_DAY: 1 day before at 20:00
        val oneDayTime = ZonedDateTime.of(vrata.date.minusDays(1), LocalTime.of(20, 0), zone)
        val oneDayMillis = oneDayTime.toInstant().toEpochMilli()
        if (oneDayMillis > nowMillis) {
            val id = "${vrata.type}|${vrata.date}|${ReminderKind.ONE_DAY.name}".hashCode()
            reminders.add(Reminder(id, vrata.type, vrata.date, oneDayMillis, ReminderKind.ONE_DAY))
        }

        // 3. PARANA: 20:00 on the vrata day, only if parana has a start or end time
        if (vrata.parana?.start != null || vrata.parana?.end != null) {
            val paranaTime = ZonedDateTime.of(vrata.date, LocalTime.of(20, 0), zone)
            val paranaMillis = paranaTime.toInstant().toEpochMilli()
            if (paranaMillis > nowMillis) {
                val id = "${vrata.type}|${vrata.date}|${ReminderKind.PARANA.name}".hashCode()
                reminders.add(Reminder(id, vrata.type, vrata.date, paranaMillis, ReminderKind.PARANA))
            }
        }
    }

    return reminders
}
