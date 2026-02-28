package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    @SerialName("name")
    val name: String? = null,

    @SerialName("listeners")
    val listeners: String? = null,

    @SerialName("playcount")
    val playcount: Int? = null,

    @SerialName("artist")
    val artist: Artist? = null
)
