package org.lineageos.twelve.datasources.lastfm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    @SerialName("name")
    val name: String? = null
)
