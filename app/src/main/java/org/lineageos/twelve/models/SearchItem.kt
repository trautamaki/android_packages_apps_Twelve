/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.models

sealed class SearchItem {
    data class Header(val title: String) : SearchItem()
    data class ArtistRow(val artists: List<Artist>) : SearchItem()
    data class AlbumRow(val albums: List<Album>) : SearchItem()
    data class ArtistItem(val artist: Artist) : SearchItem()
    data class AlbumItem(val album: Album) : SearchItem()
    data class AudioItem(val audio: Audio) : SearchItem()
    data class GenreItem(val genre: Genre) : SearchItem()
    data class PlaylistItem(val playlist: Playlist) : SearchItem()
}
