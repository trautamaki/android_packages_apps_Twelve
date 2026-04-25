/*
 * SPDX-FileCopyrightText: 2025-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lineageos.twelve.database.TwelveDatabase
import org.lineageos.twelve.database.entities.SearchHistory
import org.lineageos.twelve.models.Album
import org.lineageos.twelve.models.Artist
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.Error
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.Genre
import org.lineageos.twelve.models.MediaItem
import org.lineageos.twelve.models.Playlist
import org.lineageos.twelve.models.Result
import org.lineageos.twelve.models.Result.Companion.map
import org.lineageos.twelve.models.SearchItem
import java.time.Instant

/**
 * Home page view model.
 */
class MainViewModel(application: Application) : TwelveViewModel(application) {
    val navigationProvider = mediaRepository.navigationProvider
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    private val searchQuery = MutableStateFlow("" to false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = searchQuery
        .mapLatest {
            val (query, immediate) = it
            if (!immediate && query.isNotEmpty()) {
                delay(500)
            }
            query
        }
        .flatMapLatest { query ->
            query.trim().takeIf { it.isNotEmpty() }?.let {
                mediaRepository.search(it)
                    .mapLatest { result ->
                        when (result) {
                            is Result.Success -> Result.Success<List<SearchItem>, Error>(
                                groupResults(result.data)
                            )

                            is Result.Error -> Result.Error<List<SearchItem>, Error>(
                                result.error,
                                result.throwable
                            )
                        }
                    }
                    .asFlowResult()
                    .onStart { emit(FlowResult.Loading()) }
            } ?: flowOf(FlowResult.Success<List<SearchItem>, Error>(listOf()))
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            FlowResult.Loading()
        )

    private fun groupResults(items: List<MediaItem<*>>): List<SearchItem> {
        val genres = items.filterIsInstance<Genre>()
        val artists = items.filterIsInstance<Artist>()
        val albums = items.filterIsInstance<Album>()
        val playlists = items.filterIsInstance<Playlist>()
        val audios = items.filterIsInstance<Audio>()

        return buildList {
            if (genres.isNotEmpty()) {
                add(SearchItem.Header("Genres"))
                genres.take(SEARCH_RESULTS_LIMIT).forEach { add(SearchItem.GenreItem(it)) }
            }
            if (artists.isNotEmpty()) {
                add(SearchItem.Header("Artists"))
                add(SearchItem.ArtistRow(artists.take(SEARCH_RESULTS_LIMIT)))
            }
            if (albums.isNotEmpty()) {
                add(SearchItem.Header("Albums"))
                add(SearchItem.AlbumRow(albums.take(SEARCH_RESULTS_LIMIT)))
            }
            if (playlists.isNotEmpty()) {
                add(SearchItem.Header("Playlists"))
                playlists.take(SEARCH_RESULTS_LIMIT).forEach { add(SearchItem.PlaylistItem(it)) }
            }
            if (audios.isNotEmpty()) {
                add(SearchItem.Header("Songs"))
                audios.forEach { add(SearchItem.AudioItem(it)) }
            }
        }
    }

    fun setSearchQuery(query: String, immediate: Boolean = false) {
        searchQuery.value = query to immediate

        if (immediate) {
            addHistoryItem(query)
        }
    }

    suspend fun playAllAudios() = mediaRepository.audios().firstOrNull()?.map { audios ->
        playAudio(audios.shuffled(), 0)
    } ?: Result.Error(Error.INVALID_RESPONSE)

    private val searchHistoryDao = TwelveDatabase.get(application).getSearchHistoryDao()

    val searchHistory = searchHistoryDao.getAll()
        .map { items -> items.map { it.query } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            listOf(),
        )

    fun addHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryDao.insert(SearchHistory(query, Instant.now()))
        }
    }

    fun removeSearchQuery(query: String) {
        viewModelScope.launch {
            searchHistoryDao.delete(SearchHistory(query, Instant.now()))
        }
    }

    companion object {
        private const val SEARCH_RESULTS_LIMIT = 3
    }
}
