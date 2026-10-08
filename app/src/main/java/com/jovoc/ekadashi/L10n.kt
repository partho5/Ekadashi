package com.jovoc.ekadashi

import android.content.Context
import android.content.res.Configuration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

object L10n {
    fun attachBaseContext(context: Context, langCode: String): Context {
        val locale = getLocale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getLocale(langCode: String): Locale = when (langCode.lowercase(Locale.ROOT)) {
        "bn" -> Locale.forLanguageTag("bn-BD")
        "hi" -> Locale.forLanguageTag("hi-IN")
        else -> Locale.ENGLISH
    }

    fun getVrataName(context: Context, type: String): String {
        val resId = context.resources.getIdentifier("vrata_$type", "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            type.split("_").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
    }

    fun formatDate(date: LocalDate, locale: Locale, pattern: String = "EEE, d MMM"): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            .withDecimalStyle(DecimalStyle.of(locale))
        return date.format(formatter)
    }

    fun formatTime(time: ZonedDateTime, locale: Locale, pattern: String = "HH:mm"): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            .withDecimalStyle(DecimalStyle.of(locale))
        return time.format(formatter)
    }

    fun formatParanaWindow(
        context: Context,
        parana: Parana?,
        targetZone: ZoneId,
        locale: Locale
    ): String? {
        if (parana == null || (parana.start == null && parana.end == null)) {
            return null
        }

        val startTime = parana.startIn(targetZone)
        if (startTime == null) {
            // End-only parana: "within <time>"; "morning" wording only when the local time is morning.
            val endTime = parana.endIn(targetZone) ?: return null
            return if (endTime.hour in 4..11) {
                context.getString(R.string.parana_before_morning_fmt, formatTime(endTime, locale, "h:mm"))
            } else {
                context.getString(
                    R.string.parana_before_fmt,
                    formatDate(endTime.toLocalDate(), locale, "EEE, d MMM"),
                    formatTime(endTime, locale)
                )
            }
        }
        val startStr = formatTime(startTime, locale)
        val startDateStr = formatDate(startTime.toLocalDate(), locale, "EEE, d MMM")

        val endTime = parana.endIn(targetZone)
        return if (endTime != null) {
            val endStr = formatTime(endTime, locale)
            context.getString(R.string.parana_window_fmt, startDateStr, startStr, endStr)
        } else {
            context.getString(R.string.parana_after_fmt, startDateStr, startStr)
        }
    }
}
