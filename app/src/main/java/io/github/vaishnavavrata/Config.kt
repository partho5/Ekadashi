package io.github.vaishnavavrata

import android.content.Context
import android.telephony.TelephonyManager
import java.time.ZoneId
import java.util.Locale

enum class Country(
    val code: String,
    val displayNameEn: String,
    val displayNameNative: String,
    val defaultTimezone: String
) {
    BD("BD", "Bangladesh", "বাংলাদেশ", "Asia/Dhaka"),
    IN("IN", "India", "भारत", "Asia/Kolkata"),
    OTHER("OTHER", "Other", "Other", "");

    fun getTimezone(systemZoneId: ZoneId = ZoneId.systemDefault()): ZoneId {
        return when (this) {
            BD -> ZoneId.of("Asia/Dhaka")
            IN -> ZoneId.of("Asia/Kolkata")
            OTHER -> systemZoneId
        }
    }

    companion object {
        fun fromCode(code: String?): Country {
            if (code == null) return OTHER
            return when (code.trim().uppercase(Locale.ROOT)) {
                "BD" -> BD
                "IN" -> IN
                else -> OTHER
            }
        }
    }
}

object Config {
    private const val PREFS_NAME = "vaishnavavrata_config"

    private const val KEY_COUNTRY = "country"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TIMEZONE = "timezone"
    private const val KEY_SETUP_DONE = "setupDone"
    private const val KEY_ETAG = "etag"
    private const val KEY_SCHEDULED_IDS = "scheduledIds"
    private const val KEY_NOTIF_ASKED = "notifAsked"

    // Pure detection logic for country
    fun inferCountryFromInputs(
        networkIso: String?,
        simIso: String?,
        localeCountry: String?,
        timezoneId: String?
    ): Country {
        fun isoToCountry(iso: String?): Country? {
            if (iso.isNullOrBlank()) return null
            val upper = iso.trim().uppercase(Locale.ROOT)
            return when (upper) {
                "BD" -> Country.BD
                "IN" -> Country.IN
                else -> Country.OTHER
            }
        }

        isoToCountry(networkIso)?.let { return it }
        isoToCountry(simIso)?.let { return it }
        isoToCountry(localeCountry)?.let { return it }

        if (!timezoneId.isNullOrBlank()) {
            val tz = timezoneId.trim()
            if (tz.equals("Asia/Dhaka", ignoreCase = true)) return Country.BD
            if (tz.equals("Asia/Kolkata", ignoreCase = true) || tz.equals("Asia/Calcutta", ignoreCase = true)) return Country.IN
        }

        return Country.OTHER
    }

    // Pure detection logic for language
    fun inferLanguageFromInputs(deviceLang: String?, country: Country): String {
        val lang = deviceLang?.trim()?.lowercase(Locale.ROOT)
        if (lang == "en" || lang == "bn" || lang == "hi") {
            return lang
        }
        return when (country) {
            Country.BD -> "bn"
            Country.IN -> "hi"
            Country.OTHER -> "en"
        }
    }

    fun inferCountry(context: Context): Country {
        val telephony = try {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        } catch (e: Exception) {
            null
        }
        val networkIso = try { telephony?.networkCountryIso } catch (e: Exception) { null }
        val simIso = try { telephony?.simCountryIso } catch (e: Exception) { null }
        val localeCountry = try { Locale.getDefault().country } catch (e: Exception) { null }
        val tzId = try { ZoneId.systemDefault().id } catch (e: Exception) { null }

        return inferCountryFromInputs(networkIso, simIso, localeCountry, tzId)
    }

    fun inferLanguage(context: Context, country: Country): String {
        val deviceLang = try { Locale.getDefault().language } catch (e: Exception) { null }
        return inferLanguageFromInputs(deviceLang, country)
    }

    fun isSetupDone(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETUP_DONE, false)
    }

    fun setSetupDone(context: Context, done: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETUP_DONE, done).apply()
    }

    fun getCountry(context: Context): Country {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_COUNTRY, null)
        return if (!saved.isNullOrBlank()) {
            Country.fromCode(saved)
        } else {
            inferCountry(context)
        }
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        return if (!saved.isNullOrBlank()) {
            saved
        } else {
            inferLanguage(context, getCountry(context))
        }
    }

    fun getTimezone(context: Context): ZoneId {
        val country = getCountry(context)
        return when (country) {
            Country.BD -> ZoneId.of("Asia/Dhaka")
            Country.IN -> ZoneId.of("Asia/Kolkata")
            Country.OTHER -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedTz = prefs.getString(KEY_TIMEZONE, null)
                if (!savedTz.isNullOrBlank()) {
                    try {
                        ZoneId.of(savedTz)
                    } catch (e: Exception) {
                        ZoneId.systemDefault()
                    }
                } else {
                    ZoneId.systemDefault()
                }
            }
        }
    }

    fun saveConfig(
        context: Context,
        country: Country,
        language: String,
        timezoneId: String? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString(KEY_COUNTRY, country.code)
            .putString(KEY_LANGUAGE, language)
            .putBoolean(KEY_SETUP_DONE, true)

        if (country == Country.OTHER) {
            val tz = timezoneId ?: try { ZoneId.systemDefault().id } catch (e: Exception) { "UTC" }
            editor.putString(KEY_TIMEZONE, tz)
        } else {
            editor.remove(KEY_TIMEZONE)
        }
        editor.apply()
    }

    fun updateTimezoneIfOther(context: Context): ZoneId {
        val country = getCountry(context)
        if (country == Country.OTHER) {
            val sysTz = try { ZoneId.systemDefault().id } catch (e: Exception) { "UTC" }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TIMEZONE, sysTz).apply()
            return ZoneId.of(sysTz)
        }
        return getTimezone(context)
    }

    fun isNotifAsked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIF_ASKED, false)
    }

    fun setNotifAsked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
    }

    fun getETag(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ETAG, null)
    }

    fun setETag(context: Context, etag: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ETAG, etag).apply()
    }

    fun getScheduledIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()) ?: emptySet()
    }

    fun setScheduledIds(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, ids).apply()
    }
}
