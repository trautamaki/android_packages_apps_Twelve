package org.lineageos.twelve.repositories

import kotlinx.coroutines.flow.flow
import org.lineageos.twelve.datasources.lastfm.LastfmClient
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult

class LastfmRepository(
    private val client: LastfmClient,
) {
    fun popularTracksByArtist(artistName: String) = flow {
        emit(client.getPopularTracksByArtist(artistName))
    }.asFlowResult()
}
