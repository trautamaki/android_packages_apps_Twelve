package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class ChartArtistsQueryResult(
    val artists: ChartArtists? = null,
)
