package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class TopTracks(
    val track: List<Track> = emptyList()
)
