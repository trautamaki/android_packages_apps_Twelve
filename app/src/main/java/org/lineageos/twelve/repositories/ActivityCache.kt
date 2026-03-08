package org.lineageos.twelve.repositories

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedActivityItem(
    val uri: String,
    val title: String? = null,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val thumbnailUri: String? = null,
    val type: String, // "audio", "album", "artist", "playlist"
)

@Serializable
data class CachedActivityTab(
    val id: String,
    val title: String,
    val items: List<CachedActivityItem>,
)

class ActivityCache(context: Context) {
    private val file = File(context.cacheDir, "activity_cache.json")

    fun get(): List<CachedActivityTab>? {
        if (!file.exists()) return null
        return runCatching {
            Json.decodeFromString<List<CachedActivityTab>>(file.readText())
        }.getOrNull()
    }

    fun put(tabs: List<CachedActivityTab>) {
        runCatching {
            file.writeText(Json.encodeToString(tabs))
        }
    }
}
