package com.jovoc.ekadashi

import android.content.Context
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TithiType(val key: String) {
    PRATIPADA("pratipada"),
    DVITIYA("dvitiya"),
    TRITIYA("tritiya"),
    CHATURTHI("chaturthi"),
    PANCHAMI("panchami"),
    SHASHTI("shashti"),
    SAPTAMI("saptami"),
    ASHTAMI("ashtami"),
    NAVAMI("navami"),
    DASHAMI("dashami"),
    EKADASHI("ekadashi"),
    DVADASHI("dvadashi"),
    TRAYODASHI("trayodashi"),
    CHATURDASHI("chaturdashi"),
    PURNIMA("purnima"),
    AMAVASYA("amavasya"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(raw: String): TithiType {
            val clean = raw.trim().lowercase(Locale.ROOT)
            return when {
                clean.contains("প্রতিপদ") || clean.contains("प्रतिपदा") || clean.contains("pratipada") -> PRATIPADA
                clean.contains("দ্বিতীয়া") || clean.contains("द्वितीया") || clean.contains("dvitiya") || clean.contains("dwitiya") -> DVITIYA
                clean.contains("তৃতীয়া") || clean.contains("तृतीया") || clean.contains("tritiya") -> TRITIYA
                clean.contains("চতুর্থী") || clean.contains("चतुर्थी") || clean.contains("chaturthi") -> CHATURTHI
                clean.contains("পঞ্চমী") || clean.contains("पंचमी") || clean.contains("panchami") -> PANCHAMI
                clean.contains("ষষ্ঠী") || clean.contains("षष्ठी") || clean.contains("shashti") || clean.contains("shashthi") -> SHASHTI
                clean.contains("সপ্তমী") || clean.contains("सप्तमी") || clean.contains("saptami") -> SAPTAMI
                clean.contains("অষ্টমী") || clean.contains("अष्टमी") || clean.contains("ashtami") -> ASHTAMI
                clean.contains("নবমী") || clean.contains("नवमी") || clean.contains("navami") || clean.contains("nabami") -> NAVAMI
                clean.contains("দশমী") || clean.contains("दशमी") || clean.contains("dashami") -> DASHAMI
                clean.contains("একাদশী") || clean.contains("एकादशी") || clean.contains("ekadashi") -> EKADASHI
                clean.contains("দ্বাদশী") || clean.contains("द्वादशी") || clean.contains("dvadashi") || clean.contains("dwadashi") -> DVADASHI
                clean.contains("ত্রয়োদশী") || clean.contains("त्रयोदशी") || clean.contains("trayodashi") -> TRAYODASHI
                clean.contains("চতুর্দশী") || clean.contains("चतुर्दशी") || clean.contains("chaturdashi") -> CHATURDASHI
                clean.contains("পূর্ণিমা") || clean.contains("पूर्णिमा") || clean.contains("purnima") -> PURNIMA
                clean.contains("অমাবস্যা") || clean.contains("अमावस्या") || clean.contains("amavasya") -> AMAVASYA
                else -> UNKNOWN
            }
        }
    }
}

data class TithiSpan(
    val name: String,
    val type: TithiType,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

data class TithiSegment(
    val tithi: TithiSpan,
    val segmentStart: ZonedDateTime,
    val segmentEnd: ZonedDateTime,
    val durationSeconds: Long,
    val rawPercent: Float,
    val visualPercent: Float,
    val isActive: Boolean
)

data class DayTithiSchedule(
    val date: LocalDate,
    val sunriseToday: ZonedDateTime,
    val sunriseTomorrow: ZonedDateTime,
    val segments: List<TithiSegment>,
    val activeSegment: TithiSegment?,
    val forbiddenFood: String
)

object TithiHelper {

    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // Default fallback forbidden foods mapping
    private val DEFAULT_FOODS = mapOf(
        TithiType.PRATIPADA to Triple("Ash Gourd (Kushmanda)", "চালকুমড়া (কুষ্মাণ্ড)", "पेठा (कुष्मांड)"),
        TithiType.DVITIYA to Triple("Small Eggplant (Brihati)", "ছোট বেগুন (বৃহতী)", "छोटा बैंगन (बृहती)"),
        TithiType.TRITIYA to Triple("Pointed Gourd (Patol)", "পটল", "परवल"),
        TithiType.CHATURTHI to Triple("Radish (Mula)", "মূলা", "मूली"),
        TithiType.PANCHAMI to Triple("Wood Apple (Bel)", "বেল", "बेल"),
        TithiType.SHASHTI to Triple("Neem Leaves", "নিম পাতা", "नीम"),
        TithiType.SAPTAMI to Triple("Palm Fruit (Tal)", "তাল", "ताड़ का फल"),
        TithiType.ASHTAMI to Triple("Coconut", "নারকেল", "नारियल"),
        TithiType.NAVAMI to Triple("Bottle Gourd (Lau)", "লাউ", "लौकी"),
        TithiType.DASHAMI to Triple("Kalambi Greens", "কলমী শাক", "कलमी साग"),
        TithiType.EKADASHI to Triple("All Grains & Beans", "সমস্ত শস্য ও ডাল", "अन्न व दालें"),
        TithiType.DVADASHI to Triple("Masoor Dal / Pui Greens", "পুঁইশাক ও মসুর ডাল", "मसूर दाल व पोई साग"),
        TithiType.TRAYODASHI to Triple("Eggplant (Begun)", "বেগুন", "बैंगन"),
        TithiType.CHATURDASHI to Triple("Black Gram (Mashkalai)", "মাষকলাই ডাল", "उड़द दाल"),
        TithiType.PURNIMA to Triple("Non-Veg / Meat / Fish", "আমিষ আহার", "मांसाहार"),
        TithiType.AMAVASYA to Triple("Non-Veg / Meat / Fish", "আমিষ আহার", "मांसाहार")
    )

    fun getForbiddenFood(type: TithiType, lang: String): String {
        val entry = DEFAULT_FOODS[type] ?: return ""
        return when (lang.lowercase(Locale.ROOT)) {
            "bn" -> entry.second
            "hi" -> entry.third
            else -> entry.first
        }
    }

    fun getTithiDisplayName(type: TithiType, lang: String, fallback: String): String {
        return when (type) {
            TithiType.PRATIPADA -> when (lang) { "bn" -> "প্রতিপদ"; "hi" -> "प्रतिपदा"; else -> "Pratipada" }
            TithiType.DVITIYA -> when (lang) { "bn" -> "দ্বিতীয়া"; "hi" -> "द्वितीया"; else -> "Dvitiya" }
            TithiType.TRITIYA -> when (lang) { "bn" -> "তৃতীয়া"; "hi" -> "तृतीया"; else -> "Tritiya" }
            TithiType.CHATURTHI -> when (lang) { "bn" -> "চতুর্থী"; "hi" -> "चतुर्थी"; else -> "Chaturthi" }
            TithiType.PANCHAMI -> when (lang) { "bn" -> "পঞ্চমী"; "hi" -> "पंचमी"; else -> "Panchami" }
            TithiType.SHASHTI -> when (lang) { "bn" -> "ষষ্ঠী"; "hi" -> "षष्ठी"; else -> "Shashti" }
            TithiType.SAPTAMI -> when (lang) { "bn" -> "সপ্তমী"; "hi" -> "सप्तमी"; else -> "Saptami" }
            TithiType.ASHTAMI -> when (lang) { "bn" -> "অষ্টমী"; "hi" -> "अष्टमी"; else -> "Ashtami" }
            TithiType.NAVAMI -> when (lang) { "bn" -> "নবমী"; "hi" -> "नवमी"; else -> "Nabami" }
            TithiType.DASHAMI -> when (lang) { "bn" -> "দশমী"; "hi" -> "दशमी"; else -> "Dashami" }
            TithiType.EKADASHI -> when (lang) { "bn" -> "একাদশী"; "hi" -> "एकादशी"; else -> "Ekadashi" }
            TithiType.DVADASHI -> when (lang) { "bn" -> "দ্বাদশী"; "hi" -> "द्वादशी"; else -> "Dvadashi" }
            TithiType.TRAYODASHI -> when (lang) { "bn" -> "ত্রয়োদশী"; "hi" -> "त्रयोदशी"; else -> "Trayodashi" }
            TithiType.CHATURDASHI -> when (lang) { "bn" -> "চতুর্দশী"; "hi" -> "चतुर्दशी"; else -> "Chaturdashi" }
            TithiType.PURNIMA -> when (lang) { "bn" -> "পূর্ণিমা"; "hi" -> "पूर्णिमा"; else -> "Purnima" }
            TithiType.AMAVASYA -> when (lang) { "bn" -> "অমাবস্যা"; "hi" -> "अमावस्या"; else -> "Amavasya" }
            TithiType.UNKNOWN -> fallback
        }
    }

    /**
     * Parses the JSON containing tithi transitions.
     */
    fun parseTithisJson(jsonString: String, targetZone: ZoneId): List<TithiSpan> {
        val root = JSONObject(jsonString)
        val tzStr = root.optString("timezone", "")
        val sourceZone = when {
            tzStr.contains("Bangladesh", ignoreCase = true) || tzStr.contains("UTC+06:00") -> ZoneId.of("Asia/Dhaka")
            tzStr.contains("India", ignoreCase = true) || tzStr.contains("UTC+05:30") -> ZoneId.of("Asia/Kolkata")
            else -> ZoneId.of("Asia/Dhaka")
        }

        val array = root.optJSONArray("tithis") ?: return emptyList()
        val list = mutableListOf<TithiSpan>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("tithi", "").trim()
            val startStr = obj.optString("start", "")
            val endStr = obj.optString("end", "")
            if (name.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) continue

            try {
                val startLocal = LocalDateTime.parse(startStr, DATE_TIME_FORMATTER)
                val endLocal = LocalDateTime.parse(endStr, DATE_TIME_FORMATTER)

                val startZoned = startLocal.atZone(sourceZone).withZoneSameInstant(targetZone)
                val endZoned = endLocal.atZone(sourceZone).withZoneSameInstant(targetZone)

                list.add(
                    TithiSpan(
                        name = name,
                        type = TithiType.fromString(name),
                        start = startZoned,
                        end = endZoned
                    )
                )
            } catch (e: Exception) {
                // Skip malformed record
            }
        }
        return list.sortedBy { it.start }
    }

    /**
     * Computes the day's timeline segments and active forbidden food.
     * Enforces minimum width threshold (Option 1) so short-duration segments remain readable.
     */
    fun computeDaySchedule(
        date: LocalDate,
        allTithis: List<TithiSpan>,
        targetZone: ZoneId,
        now: ZonedDateTime,
        lang: String,
        lat: Double = 23.8103,
        lon: Double = 90.4125,
        minWidthPercent: Float = 0.08f // Minimum visual width: 8%
    ): DayTithiSchedule? {
        if (allTithis.isEmpty()) return null

        val sunriseToday = SunriseCalculator.calculateSunrise(date, lat, lon, targetZone)
        val sunriseTomorrow = SunriseCalculator.calculateSunrise(date.plusDays(1), lat, lon, targetZone)

        val totalDayDuration = Duration.between(sunriseToday, sunriseTomorrow).seconds
        if (totalDayDuration <= 0) return null

        // Find tithis that intersect [sunriseToday, sunriseTomorrow]
        val overlapping = allTithis.filter { tithi ->
            tithi.end.isAfter(sunriseToday) && tithi.start.isBefore(sunriseTomorrow)
        }
        if (overlapping.isEmpty()) return null

        // 1. Calculate raw durations & percentages
        data class RawSeg(val tithi: TithiSpan, val segStart: ZonedDateTime, val segEnd: ZonedDateTime, val durSec: Long, val rawP: Float)

        val rawList = overlapping.map { tithi ->
            val segStart = if (tithi.start.isAfter(sunriseToday)) tithi.start else sunriseToday
            val segEnd = if (tithi.end.isBefore(sunriseTomorrow)) tithi.end else sunriseTomorrow
            val durSec = Duration.between(segStart, segEnd).seconds.coerceAtLeast(0)
            val rawP = (durSec.toFloat() / totalDayDuration.toFloat()).coerceIn(0f, 1f)
            RawSeg(tithi, segStart, segEnd, durSec, rawP)
        }

        // 2. Adjust for minimum visual width (Option 1)
        val n = rawList.size
        val visualPercentages = if (n > 1) {
            val adjusted = rawList.map { it.rawP }.toMutableList()
            var underMinCount = 0
            var deficit = 0f

            for (i in 0 until n) {
                if (adjusted[i] < minWidthPercent) {
                    deficit += (minWidthPercent - adjusted[i])
                    adjusted[i] = minWidthPercent
                    underMinCount++
                }
            }

            if (deficit > 0f && underMinCount < n) {
                // Deduct deficit proportionally from segments above the minimum
                val excessSum = adjusted.filterIndexed { i, _ -> rawList[i].rawP >= minWidthPercent }.sum()
                if (excessSum > deficit) {
                    for (i in 0 until n) {
                        if (rawList[i].rawP >= minWidthPercent) {
                            val proportion = adjusted[i] / excessSum
                            adjusted[i] -= deficit * proportion
                        }
                    }
                }
            }
            adjusted
        } else {
            listOf(1.0f)
        }

        // 3. Build segments and detect which is active
        var activeSeg: TithiSegment? = null

        val segments = rawList.mapIndexed { idx, raw ->
            val isNowActive = (now.isEqual(raw.segStart) || now.isAfter(raw.segStart)) && now.isBefore(raw.segEnd)
            val seg = TithiSegment(
                tithi = raw.tithi,
                segmentStart = raw.segStart,
                segmentEnd = raw.segEnd,
                durationSeconds = raw.durSec,
                rawPercent = raw.rawP,
                visualPercent = visualPercentages[idx],
                isActive = isNowActive
            )
            if (isNowActive) activeSeg = seg
            seg
        }

        // If 'now' is outside this day (viewing past or future day), pick the dominant segment (longest duration)
        val resolvedActive = activeSeg ?: segments.maxByOrNull { it.durationSeconds }

        val forbiddenFood = if (resolvedActive != null) {
            getForbiddenFood(resolvedActive.tithi.type, lang)
        } else ""

        return DayTithiSchedule(
            date = date,
            sunriseToday = sunriseToday,
            sunriseTomorrow = sunriseTomorrow,
            segments = segments,
            activeSegment = resolvedActive,
            forbiddenFood = forbiddenFood
        )
    }

    /**
     * Loads tithis from app storage or bundled asset seed.
     */
    fun loadTithis(context: Context, targetZone: ZoneId): List<TithiSpan> {
        val fileName = "tithis_1month.json"
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            parseTithisJson(jsonString, targetZone)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
