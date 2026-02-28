package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.Serializable


@Serializable
data class ChartArtists(
    val artist: List<Artist> = emptyList(),
)
