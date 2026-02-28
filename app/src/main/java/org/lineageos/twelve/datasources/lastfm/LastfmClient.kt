package org.lineageos.twelve.datasources.lastfm

import androidx.core.net.toUri
import okhttp3.OkHttpClient
import org.lineageos.twelve.datasources.lastfm.models.ArtistTracksQueryResult
import org.lineageos.twelve.models.Result
import org.lineageos.twelve.utils.Api
import org.lineageos.twelve.utils.ApiRequest
import org.lineageos.twelve.utils.mapToError

class LastfmClient(
    private val server: String, private val apiKeyProvider: () -> String,
) {
    private val okHttpClient = OkHttpClient.Builder().build()

    private val api = Api(okHttpClient, server.toUri())

    suspend fun getPopularTracksByArtist(artistName: String): Result<ArtistTracksQueryResult, org.lineageos.twelve.models.Error> {
        val key = apiKeyProvider()
        if (key.isBlank()) return Result.Error(org.lineageos.twelve.models.Error.NOT_FOUND)

        return ApiRequest.get<ArtistTracksQueryResult>(
            listOf("2.0"),
            listOf(
                "method" to "artist.gettoptracks",
                "artist" to artistName,
                "limit" to 5,
                "api_key" to key,
                "format" to "json"
            )
        ).execute(api).mapToError()
    }
}
