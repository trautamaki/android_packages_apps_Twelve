package org.lineageos.twelve.repositories

import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.lineageos.twelve.datasources.lastfm.LastfmClient
import org.lineageos.twelve.datasources.lastfm.models.ArtistTracksQueryResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.Result.Success
import java.util.concurrent.TimeUnit

class LastfmRepository(
    private val client: LastfmClient,
    private val cache: LastfmCache,
) {
    fun popularTracksByArtist(artistName: String) = flow {
        val cacheKey = "popular_$artistName"
        val cached = cache.get(cacheKey)

        if (cached != null) {
            android.util.Log.e("LastfmRepository", "Cache hit for $cacheKey")
            emit(Success(Json.decodeFromString<ArtistTracksQueryResult>(cached)))

            // Only fetch fresh if cache is older than 48 hours
            if (!cache.isStale(cacheKey, TimeUnit.HOURS.toMillis(48))) return@flow
        }

        android.util.Log.e("LastfmRepository", "Refresh cache $cacheKey")

        try {
            val fresh = client.getPopularTracksByArtist(artistName)
            if (fresh is Success) {
                cache.put(cacheKey, Json.encodeToString(fresh.data))
                emit(fresh)
            }
        } catch (_: Exception) {
        }
    }.asFlowResult()
}
