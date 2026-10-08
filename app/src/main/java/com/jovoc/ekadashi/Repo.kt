package com.jovoc.ekadashi

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Repo {
    const val DATA_URL = "https://raw.githubusercontent.com/partho5/Ekadashi/refs/heads/main/data/vratas.json"
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

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                false
            } else {
                val jsonContent = conn.inputStream.bufferedReader().use { it.readText() }
                if (parseVratasJson(jsonContent).isEmpty()) {
                    false
                } else {
                    val cacheFile = File(context.filesDir, FILE_NAME)
                    val local = if (cacheFile.exists()) cacheFile.readText() else null
                    if (local == jsonContent) {
                        false
                    } else {
                        // Remote differs from local copy: overwrite, no merge.
                        val tmpFile = File(context.filesDir, "$FILE_NAME.tmp")
                        tmpFile.writeText(jsonContent)
                        if (cacheFile.exists()) cacheFile.delete()
                        tmpFile.renameTo(cacheFile)
                    }
                }
            }
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
