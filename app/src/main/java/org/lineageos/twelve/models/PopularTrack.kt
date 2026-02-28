package org.lineageos.twelve.models

data class PopularTrack(
    val name: String,
    val artist: String?,
    val listenerCount: Int?,
) : UniqueItem<PopularTrack> {
    override fun areItemsTheSame(other: PopularTrack) = name == other.name && artist == other.artist

    override fun areContentsTheSame(other: PopularTrack) = this == other
}
