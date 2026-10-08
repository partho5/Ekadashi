package io.github.vaishnavavrata

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Repo {
    const val DATA_URL = "https://raw.githubusercontent.com/OWNER/REPO/main/data/vratas.json"
    const val FILE_NAME = "vratas.json"

    fun load(context: Context): List<Vrata> {
        val cacheFile = File(context.filesDir, FILE_NAME)
        if (cacheFile.exists()) {
            try {
                val jsonString = cacheFile.readText()
                val vratas = parseVratasJson(jsonString)
                if (vratas.isNotEmpty()) {
                    return vratas
                }
            } catch (e: Exception) {
                // Cache file unreadable or invalid JSON; fallback to asset seed
            }
        }
        return loadFromAsset(context)
    }

    fun loadFromAsset(context: Context): List<Vrata> {
        return try {
            val jsonString = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
            parseVratasJson(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun refresh(context: Context, customUrl: String = DATA_URL): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(customUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.useCaches = false

            val currentETag = Config.getETag(context)
            if (!currentETag.isNullOrBlank()) {
                conn.setRequestProperty("If-None-Match", currentETag)
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                false
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonContent = conn.inputStream.bufferedReader().use { it.readText() }
                val newVratas = parseVratasJson(jsonContent)
                if (newVratas.isNotEmpty()) {
                    val tmpFile = File(context.filesDir, "$FILE_NAME.tmp")
                    val cacheFile = File(context.filesDir, FILE_NAME)
                    tmpFile.writeText(jsonContent)
                    if (tmpFile.exists()) {
                        if (cacheFile.exists()) {
                            cacheFile.delete()
                        }
                        tmpFile.renameTo(cacheFile)
                    }

                    val newETag = conn.getHeaderField("ETag") ?: conn.getHeaderField("etag")
                    if (!newETag.isNullOrBlank()) {
                        Config.setETag(context, newETag)
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
