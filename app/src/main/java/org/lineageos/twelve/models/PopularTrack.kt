package org.lineageos.twelve.models

data class PopularTrack(
    val name: String,
    val artist: String?,
    val listenerCount: Int?,
) : UniqueItem<PopularTrack> {
    override fun areItemsTheSame(other: PopularTrack) = name == other.name && artist == other.artist

    override fun areContentsTheSame(other: PopularTrack) = this == other

    fun normalizedTitle(): String =
        name
            .lowercase()
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?]"), "")
            .replace(Regex("feat\\.? .*"), "")
            .replace(Regex("ft\\.? .*"), "")
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
