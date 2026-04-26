/*
 * SPDX-FileCopyrightText: 2024-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.twelve.datasources.lastfm.models.toPopularTrack
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.FlowResult.Companion.flatMapLatestData
import org.lineageos.twelve.models.FlowResult.Companion.mapLatestData

class ArtistViewModel(application: Application) : TwelveViewModel(application) {
    private val artistUri = MutableStateFlow<Uri?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val artist = artistUri
        .filterNotNull()
        .flatMapLatest {
            mediaRepository.artist(it)
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun loadAlbum(artistUri: Uri) {
        this.artistUri.value = artistUri
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val artistTracks = artistUri
        .filterNotNull()
        .flatMapLatest {
            mediaRepository.artistTracks(it)
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val popularTracks =
        artist.flatMapLatestData { (artist, _) ->
            artist.name?.let { artistName ->
                lastfmRepository
                    .popularTracksByArtist(artistName)
                    .mapLatestData { queryResult ->
                        queryResult.toptracks
                            ?.track
                            ?.map { it.toPopularTrack() }
                            ?: emptyList()
                    }
            } ?: flowOf(
                FlowResult.Success(emptyList())
            )
        }
            .flowOn(Dispatchers.IO)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                FlowResult.Loading()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrichedPopularTracks = popularTracks
        .flatMapLatestData { popular ->
            artistTracks.mapLatestData { artistTracksTab ->

                val localTracks = artistTracksTab.items.filterIsInstance<Audio>()

                // Group local tracks by normalized title
                val localMap = localTracks
                    .groupBy { it.normalizedTitle() }
                    .toMutableMap()

                val usedUris = mutableSetOf<Uri>()

                popular.mapNotNull { popularTrack ->
                    val key = popularTrack.normalizedTitle()

                    val match = localMap[key]
                        ?.firstOrNull { it.uri !in usedUris }

                    match?.also { usedUris += it.uri }
                        ?.copy(listenCount = popularTrack.listenerCount)
                }
                    .distinctBy { it.uri }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    fun buildSortedQueue(
        popularTracks: List<Audio>,
        artistTracks: List<Audio>
    ): List<Audio> {
        val popularityMap = popularTracks.associateBy { it.uri }

        return artistTracks
            .map { track ->
                popularityMap[track.uri] ?: track.copy(listenCount = 0)
            }
            .sortedByDescending { it.listenCount ?: 0 }
    }
}
