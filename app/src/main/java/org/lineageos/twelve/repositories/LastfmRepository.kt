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
    val json: Json = Json {
        ignoreUnknownKeys = true
    },
) {
    fun popularTracksByArtist(artistName: String) = flow {
        val cacheKey = "popular_$artistName"
        val cached = cache.get(cacheKey)

        if (cached != null) {
            emit(Success(json.decodeFromString<ArtistTracksQueryResult>(cached)))
            if (!cache.isStale(cacheKey, TimeUnit.HOURS.toMillis(48))) return@flow
        }

        try {
            val fresh = client.getPopularTracksByArtist(artistName)
            if (fresh is Success) {
                cache.put(cacheKey, json.encodeToString(fresh.data))
                emit(fresh)
            }
        } catch (_: Exception) {
        }
    }.asFlowResult()

    fun globalTrendingArtists() = cachedFlow(
        cacheKey = "global_trending_artists",
        maxAgeMs = TimeUnit.HOURS.toMillis(6),
        fetch = { client.getGlobalTrendingArtists() },
    )

    fun localTrendingArtists(country: String) = cachedFlow(
        cacheKey = "local_trending_artists_$country",
        maxAgeMs = TimeUnit.HOURS.toMillis(6),
        fetch = { client.getLocalTrendingArtists(country) },
    )

    // Generic cache-then-fetch helper used by all methods above
    private inline fun <reified T> cachedFlow(
        cacheKey: String,
        maxAgeMs: Long,
        crossinline fetch: suspend () -> org.lineageos.twelve.models.Result<T, org.lineageos.twelve.models.Error>,
    ) = flow {
        val cached = cache.get(cacheKey)

        if (cached != null) {
            emit(Success(json.decodeFromString<T>(cached)))
            if (!cache.isStale(cacheKey, maxAgeMs)) return@flow
        }

        try {
            val fresh = fetch()
            if (fresh is Success) {
                cache.put(cacheKey, json.encodeToString(fresh.data))
                emit(fresh)
            }
        } catch (_: Exception) {
        }
    }.asFlowResult()
}
