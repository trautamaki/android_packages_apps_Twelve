package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class ChartTracksQueryResult(
    val tracks: ChartTracks? = null,
)
