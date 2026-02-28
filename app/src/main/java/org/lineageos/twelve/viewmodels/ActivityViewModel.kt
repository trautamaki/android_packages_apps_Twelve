/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.twelve.R
import org.lineageos.twelve.datasources.lastfm.models.ChartArtistsQueryResult
import org.lineageos.twelve.models.ActivityTab
import org.lineageos.twelve.models.Artist
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.LocalizedString

class ActivityViewModel(application: Application) : TwelveViewModel(application) {

    private val country = "finland" // TODO

    val activity = mediaRepository.activity()
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FlowResult.Loading())

    val globalTrendingArtists = lastfmRepository.globalTrendingArtists()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FlowResult.Loading())

    val localTrendingArtists = lastfmRepository.localTrendingArtists(country)
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FlowResult.Loading())

    // combine all local library tracks to match against Last.fm results
    private val allLocalArtists = mediaRepository.artists()
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FlowResult.Loading())

    @OptIn(ExperimentalCoroutinesApi::class)
    val lastfmActivityTabs: StateFlow<List<ActivityTab>> = combine(
        allLocalArtists,
        globalTrendingArtists,
        localTrendingArtists,
    ) { allResults ->
        val localArtists = (allResults[0] as? FlowResult.Success<*, *>)
            ?.data as? List<Artist> ?: emptyList<Artist>().also {
            return@combine emptyList()
        }

        val globalArtists = allResults[1] as FlowResult<ChartArtistsQueryResult, Error>
        val localArtistsChart = allResults[2] as FlowResult<ChartArtistsQueryResult, Error>

        fun matchArtists(
            result: FlowResult<ChartArtistsQueryResult, Error>,
        ): List<Artist> {
            val raw = (result as? FlowResult.Success)?.data?.artists?.artist ?: run {
                return emptyList()
            }
            return raw.mapNotNull { lastfmArtist ->
                localArtists.firstOrNull {
                    it.name?.equals(lastfmArtist.name, ignoreCase = true) == true
                }
            }
        }

        listOfNotNull(
            matchArtists(globalArtists).takeIf { it.isNotEmpty() }?.let {
                ActivityTab(
                    "lastfm_global_artists",
                    LocalizedString.StringResIdLocalizedString(R.string.lastfm_global_trending_artists),
                    it,
                )
            },
            matchArtists(localArtistsChart).takeIf { it.isNotEmpty() }?.let {
                ActivityTab(
                    "lastfm_local_artists",
                    LocalizedString.StringResIdLocalizedString(R.string.lastfm_local_trending_artists),
                    it,
                )
            },
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun playAudio(start: Audio, rest: List<Audio>) {
        val audioList = mutableListOf(start).apply {
            addAll(rest.filter { it.uri != start.uri })
        }
        playAudio(audioList, 0)
    }
}
