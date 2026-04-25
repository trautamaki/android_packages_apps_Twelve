/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getColorFromAttr
import org.lineageos.twelve.models.Album
import org.lineageos.twelve.models.Artist
import org.lineageos.twelve.models.SearchItem
import org.lineageos.twelve.ui.views.ListItem
import org.lineageos.twelve.ui.views.SearchAlbumCardView
import org.lineageos.twelve.ui.views.SearchArtistCardView

class SearchListAdapter(
    private val onArtistClick: (SearchItem.ArtistItem) -> Unit,
    private val onArtistLongClick: (SearchItem.ArtistItem) -> Unit,
    private val onAlbumClick: (SearchItem.AlbumItem) -> Unit,
    private val onAlbumLongClick: (SearchItem.AlbumItem) -> Unit,
    private val onAudioClick: (SearchItem.AudioItem) -> Unit,
    private val onAudioLongClick: (SearchItem.AudioItem) -> Unit,
    private val onGenreClick: (SearchItem.GenreItem) -> Unit,
    private val onGenreHoldClick: (SearchItem.GenreItem) -> Unit,
    private val onPlaylistClick: (SearchItem.PlaylistItem) -> Unit,
    private val onPlaylistLongClick: (SearchItem.PlaylistItem) -> Unit,
) : ListAdapter<SearchItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is SearchItem.Header -> VIEW_TYPE_HEADER
        is SearchItem.ArtistRow, is SearchItem.AlbumRow -> VIEW_TYPE_HORIZONTAL_ROW
        else -> VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_HEADER -> HeaderViewHolder(MaterialTextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            val px16 = (16 * resources.displayMetrics.density).toInt()
            val px4 = (4 * resources.displayMetrics.density).toInt()
            setPadding(px16, px16, px16, px4)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
            setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorPrimaryFixed))
        })

        VIEW_TYPE_HORIZONTAL_ROW -> HorizontalRowViewHolder(RecyclerView(parent.context))
        else -> ItemViewHolder(ListItem(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SearchItem.ArtistRow -> (holder as HorizontalRowViewHolder).recyclerView.adapter =
                ArtistCardAdapter(item.artists)

            is SearchItem.AlbumRow -> (holder as HorizontalRowViewHolder).recyclerView.adapter =
                AlbumCardAdapter(item.albums)

            else -> (holder as ItemViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(val view: MaterialTextView) : RecyclerView.ViewHolder(view) {
        fun bind(item: SearchItem.Header) {
            view.text = item.title
        }
    }

    inner class ItemViewHolder(val view: ListItem) : RecyclerView.ViewHolder(view) {
        fun bind(item: SearchItem) {
            when (item) {
                is SearchItem.ArtistItem -> {
                    view.setLeadingIconImage(R.drawable.ic_person)
                    view.headlineText = item.artist.name
                    view.supportingText =
                        view.context.getString(R.string.artist_albums_header) // type hint
                }

                is SearchItem.AlbumItem -> {
                    view.setOnClickListener { onAlbumClick(item) }
                    view.setOnLongClickListener { onAlbumLongClick(item); true }
                    view.setTrailingIconImage(R.drawable.ic_album)
                    view.headlineText = item.album.title
                    view.supportingText = item.album.artistName
                }

                is SearchItem.AudioItem -> {
                    view.setOnClickListener { onAudioClick(item) }
                    view.setOnLongClickListener { onAudioLongClick(item); true }
                    view.setTrailingIconImage(R.drawable.ic_music_note)
                    view.headlineText = item.audio.title
                    view.supportingText = item.audio.artistName
                }

                is SearchItem.GenreItem -> {
                    view.setOnClickListener { onGenreClick(item) }
                    view.setOnLongClickListener { onGenreHoldClick(item); true }
                    view.setTrailingIconImage(R.drawable.ic_genres)
                    view.headlineText = item.genre.name
                    view.supportingText = null
                }

                is SearchItem.PlaylistItem -> {
                    view.setOnClickListener { onPlaylistClick(item) }
                    view.setOnLongClickListener { onPlaylistLongClick(item); true }
                    view.setTrailingIconImage(R.drawable.ic_playlist_play)
                    view.headlineText = item.playlist.name
                    view.supportingText = null
                }

                is SearchItem.Header -> Unit // handled above
                else -> {} // TODO
            }
        }
    }

    class HorizontalRowViewHolder(val recyclerView: RecyclerView) :
        RecyclerView.ViewHolder(recyclerView) {
        init {
            recyclerView.layoutManager = LinearLayoutManager(
                recyclerView.context,
                LinearLayoutManager.HORIZONTAL,
                false,
            )
            val dp8 = (8 * recyclerView.resources.displayMetrics.density).toInt()
            recyclerView.setPadding(dp8, 0, dp8, dp8)
            recyclerView.clipToPadding = false
            recyclerView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    // Inline adapters for the horizontal rows
    private inner class ArtistCardAdapter(
        private val artists: List<Artist>,
    ) : RecyclerView.Adapter<ArtistCardAdapter.ViewHolder>() {
        inner class ViewHolder(val view: SearchArtistCardView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(SearchArtistCardView(parent.context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val artist = artists[position]
            holder.view.bind(artist)
            holder.view.setOnClickListener { onArtistClick(SearchItem.ArtistItem(artist)) }
            holder.view.setOnLongClickListener { onArtistLongClick(SearchItem.ArtistItem(artist)); true }
        }

        override fun getItemCount() = artists.size
    }

    private inner class AlbumCardAdapter(
        private val albums: List<Album>,
    ) : RecyclerView.Adapter<AlbumCardAdapter.ViewHolder>() {
        inner class ViewHolder(val view: SearchAlbumCardView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(SearchAlbumCardView(parent.context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val album = albums[position]
            holder.view.bind(album)
            holder.view.setOnClickListener { onAlbumClick(SearchItem.AlbumItem(album)) }
            holder.view.setOnLongClickListener { onAlbumLongClick(SearchItem.AlbumItem(album)); true }
        }

        override fun getItemCount() = albums.size
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_HORIZONTAL_ROW = 2

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchItem>() {
            override fun areItemsTheSame(oldItem: SearchItem, newItem: SearchItem) =
                when (oldItem) {
                    is SearchItem.Header if newItem is SearchItem.Header ->
                        oldItem.title == newItem.title

                    is SearchItem.ArtistItem if newItem is SearchItem.ArtistItem ->
                        oldItem.artist.areItemsTheSame(newItem.artist)

                    is SearchItem.AlbumItem if newItem is SearchItem.AlbumItem ->
                        oldItem.album.areItemsTheSame(newItem.album)

                    is SearchItem.AudioItem if newItem is SearchItem.AudioItem ->
                        oldItem.audio.areItemsTheSame(newItem.audio)

                    is SearchItem.GenreItem if newItem is SearchItem.GenreItem ->
                        oldItem.genre.areItemsTheSame(newItem.genre)

                    is SearchItem.PlaylistItem if newItem is SearchItem.PlaylistItem ->
                        oldItem.playlist.areItemsTheSame(newItem.playlist)

                    is SearchItem.ArtistRow if newItem is SearchItem.ArtistRow ->
                        oldItem.artists.map { it.uri } == newItem.artists.map { it.uri }

                    is SearchItem.AlbumRow if newItem is SearchItem.AlbumRow ->
                        oldItem.albums.map { it.uri } == newItem.albums.map { it.uri }

                    else -> false
                }

            override fun areContentsTheSame(oldItem: SearchItem, newItem: SearchItem) =
                when (oldItem) {
                    is SearchItem.Header if newItem is SearchItem.Header ->
                        oldItem.title == newItem.title

                    is SearchItem.ArtistItem if newItem is SearchItem.ArtistItem ->
                        oldItem.artist.areContentsTheSame(newItem.artist)

                    is SearchItem.AlbumItem if newItem is SearchItem.AlbumItem ->
                        oldItem.album.areContentsTheSame(newItem.album)

                    is SearchItem.AudioItem if newItem is SearchItem.AudioItem ->
                        oldItem.audio.areContentsTheSame(newItem.audio)

                    is SearchItem.GenreItem if newItem is SearchItem.GenreItem ->
                        oldItem.genre.areContentsTheSame(newItem.genre)

                    is SearchItem.PlaylistItem if newItem is SearchItem.PlaylistItem ->
                        oldItem.playlist.areContentsTheSame(newItem.playlist)

                    else -> false
                }
        }
    }
}
