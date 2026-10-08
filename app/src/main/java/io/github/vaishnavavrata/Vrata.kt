package io.github.vaishnavavrata

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

val IST: ZoneId = ZoneId.of("Asia/Kolkata")

data class Parana(
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?
) {
    fun startIn(z: ZoneId): ZonedDateTime? =
        start?.let { ZonedDateTime.of(date, it, IST).withZoneSameInstant(z) }

    fun endIn(z: ZoneId): ZonedDateTime? =
        end?.let { ZonedDateTime.of(date, it, IST).withZoneSameInstant(z) }
}

const val CATEGORY_EKADASHI = "ekadashi"
const val CATEGORY_OTHER = "other"

data class Vrata(
    val type: String,
    val date: LocalDate,
    val parana: Parana?,
    val category: String = CATEGORY_OTHER,
    val note: String? = null
) {
    val isEkadashi: Boolean get() = category == CATEGORY_EKADASHI
}

fun parseVratasJson(jsonString: String): List<Vrata> {
    val result = mutableListOf<Vrata>()
    val root = try {
        JSONObject(jsonString)
    } catch (e: Exception) {
        return emptyList()
    }

    val array = root.optJSONArray("vratas") ?: return emptyList()

    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue

        val type = obj.optString("type").takeIf { it.isNotBlank() } ?: continue
        val dateStr = obj.optString("date").takeIf { it.isNotBlank() } ?: continue
        val date = try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            continue
        }

        var parana: Parana? = null
        if (!obj.isNull("parana") && obj.has("parana")) {
            val paranaObj = obj.optJSONObject("parana")
            if (paranaObj != null) {
                val paranaDateStr = paranaObj.optString("date").takeIf { it.isNotBlank() }
                val paranaDate = paranaDateStr?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: DateTimeParseException) {
                        null
                    }
                }

                if (paranaDate != null) {
                    val startStr = paranaObj.optString("start").takeIf { it.isNotBlank() }
                    val start = startStr?.let {
                        try {
                            LocalTime.parse(it)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }

                    val endStr = paranaObj.optString("end").takeIf { it.isNotBlank() }
                    val end = endStr?.let {
                        try {
                            LocalTime.parse(it)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }

                    parana = Parana(date = paranaDate, start = start, end = end)
                }
            }
        }

        val category = if (obj.optString("category") == CATEGORY_EKADASHI) CATEGORY_EKADASHI else CATEGORY_OTHER
        val note = if (obj.isNull("note")) null else obj.optString("note").takeIf { it.isNotBlank() }

        result.add(Vrata(type = type, date = date, parana = parana, category = category, note = note))
    }

    return result
}
