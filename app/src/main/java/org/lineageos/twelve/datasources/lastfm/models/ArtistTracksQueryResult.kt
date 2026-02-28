package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.Serializable
import org.lineageos.twelve.models.PopularTrack

@Serializable
data class ArtistTracksQueryResult(
    val toptracks: TopTracks?
)

fun Track.toPopularTrack() = PopularTrack(
    name = name ?: "",
    artist = artist?.name,
    listenerCount = playcount,
)
