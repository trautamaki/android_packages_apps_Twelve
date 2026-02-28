package org.lineageos.twelve.repositories

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

class LastfmCache(context: Context) {
    private val cacheDir = File(context.cacheDir, "lastfm").also { it.mkdirs() }
    private val maxAgeMs = TimeUnit.DAYS.toMillis(7)

    fun get(key: String): String? {
        val file = fileFor(key)
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > maxAgeMs) {
            file.delete()
            return null
        }
        return file.readText()
    }

    fun put(key: String, json: String) {
        fileFor(key).writeText(json)
    }

    fun isStale(key: String, maxAgeMs: Long): Boolean {
        val file = fileFor(key)
        if (!file.exists()) return true
        return System.currentTimeMillis() - file.lastModified() > maxAgeMs
    }

    private fun fileFor(key: String) =
        File(cacheDir, key.replace("/", "_").replace(" ", "_") + ".json")
}
