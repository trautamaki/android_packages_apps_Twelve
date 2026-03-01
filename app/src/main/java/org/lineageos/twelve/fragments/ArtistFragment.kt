/*
 * SPDX-FileCopyrightText: 2024-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.Bundle
import org.lineageos.twelve.ext.getParcelable
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.loadThumbnail
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.ext.setProgressCompat
import org.lineageos.twelve.ext.updatePadding
import org.lineageos.twelve.models.Album
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.Playlist
import org.lineageos.twelve.ui.recyclerview.SimpleListAdapter
import org.lineageos.twelve.ui.recyclerview.UniqueItemDiffCallback
import org.lineageos.twelve.ui.views.HorizontalMediaItemView
import org.lineageos.twelve.ui.views.PopularTrackItemView
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.viewmodels.ArtistViewModel
import kotlin.math.abs

/**
 * Single artist viewer.
 */
class ArtistFragment : CollapsingToolbarLayoutFragment(R.layout.fragment_artist) {
    // View models
    private val viewModel by viewModels<ArtistViewModel>()

    // Views
    private val albumsLinearLayout by getViewProperty<LinearLayout>(R.id.albumsLinearLayout)
    private val albumsRecyclerView by getViewProperty<RecyclerView>(R.id.albumsRecyclerView)
    private val artistButtonsLinearLayout by getViewProperty<LinearLayout>(R.id.artistButtonsLinearLayout)
    private val popularTracksLinearLayout by getViewProperty<LinearLayout>(R.id.popularTracksLinearLayout)
    private val popularTracksRecyclerView by getViewProperty<RecyclerView>(R.id.popularTracksRecyclerView)
    override val appBarLayout by getViewProperty<AppBarLayout>(R.id.appBarLayout)
    private val appearsInAlbumLinearLayout by getViewProperty<LinearLayout>(R.id.appearsInAlbumLinearLayout)
    private val appearsInAlbumRecyclerView by getViewProperty<RecyclerView>(R.id.appearsInAlbumRecyclerView)
    private val appearsInPlaylistLinearLayout by getViewProperty<LinearLayout>(R.id.appearsInPlaylistLinearLayout)
    private val appearsInPlaylistRecyclerView by getViewProperty<RecyclerView>(R.id.appearsInPlaylistRecyclerView)
    private val artistNameTextView by getViewProperty<TextView>(R.id.artistNameTextView)
    override val coordinatorLayout by getViewProperty<CoordinatorLayout>(R.id.coordinatorLayout)
    private val infoNestedScrollView by getViewProperty<NestedScrollView?>(R.id.infoNestedScrollView)
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val nestedScrollView by getViewProperty<NestedScrollView>(R.id.nestedScrollView)
    private val noElementsNestedScrollView by getViewProperty<NestedScrollView>(R.id.noElementsNestedScrollView)
    private val playFloatingActionButton by getViewProperty<FloatingActionButton>(
        R.id.playFloatingActionButton
    )
    private val shuffleFloatingActionButton by getViewProperty<FloatingActionButton>(
        R.id.shuffleFloatingActionButton
    )
    private val thumbnailImageView by getViewProperty<ImageView>(R.id.thumbnailImageView)
    private val toolbar by getViewProperty<MaterialToolbar>(R.id.toolbar)

    // Recyclerview
    private val createAlbumAdapter = {
        object : SimpleListAdapter<Album, HorizontalMediaItemView>(
            UniqueItemDiffCallback(),
            ::HorizontalMediaItemView,
        ) {
            override fun ViewHolder.onBindView(item: Album) {
                view.setOnClickListener {
                    findNavController().navigateSafe(
                        R.id.action_artistFragment_to_fragment_album,
                        AlbumFragment.createBundle(item.uri)
                    )
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_artistFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(
                            item.uri,
                            fromArtist = true,
                        )
                    )
                    true
                }

                view.setItem(item)
            }
        }
    }

    private val createPopularTrackAdapter = {
        object : SimpleListAdapter<Audio, PopularTrackItemView>(
            UniqueItemDiffCallback(),
            ::PopularTrackItemView,
        ) {
            override fun ViewHolder.onBindView(item: Audio) {
                view.setItem(item, bindingAdapterPosition)

                view.setOnClickListener {
                    viewModel.playAudio(listOf(item), 0)
                }

                view.playButton.setOnClickListener {
                    viewModel.playAudio(listOf(item), 0)
                }
            }
        }
    }

    private val albumsAdapter by lazy { createAlbumAdapter() }
    private val popularTracksAdapter by lazy { createPopularTrackAdapter() }
    private val appearsInAlbumAdapter by lazy { createAlbumAdapter() }
    private val appearsInPlaylistAdapter by lazy {
        object : SimpleListAdapter<Playlist, HorizontalMediaItemView>(
            UniqueItemDiffCallback(),
            ::HorizontalMediaItemView,
        ) {
            override fun ViewHolder.onBindView(item: Playlist) {
                view.setOnClickListener {
                    // TODO
                }
                view.setOnLongClickListener {
                    findNavController().navigateSafe(
                        R.id.action_albumFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(
                            item.uri,
                            fromArtist = true,
                        )
                    )
                    true
                }

                view.setItem(item)
            }
        }
    }

    // Arguments
    private val artistUri: Uri
        get() = requireArguments().getParcelable(ARG_ARTIST_URI, Uri::class)!!

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.menu.findItem(R.id.action_play)?.isVisible = false
        toolbar.menu.findItem(R.id.action_shuffle)?.isVisible = false

        appBarLayout.addOnOffsetChangedListener { appBar, verticalOffset ->
            val isCollapsed = abs(verticalOffset) >= appBar.totalScrollRange

            // show in toolbar only when collapsed
            toolbar.menu.findItem(R.id.action_play)?.isVisible = isCollapsed
            toolbar.menu.findItem(R.id.action_shuffle)?.isVisible = isCollapsed

            // hide label buttons when collapsed
            playFloatingActionButton.isVisible = !isCollapsed
            shuffleFloatingActionButton.isVisible = !isCollapsed
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_play -> {
                    play()
                    true
                }

                R.id.action_shuffle -> {
                    shufflePlay()
                    true
                }

                else -> false
            }
        }

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            v.updatePadding(
                insets,
                start = true,
                end = true,
            )

            windowInsets
        }

        infoNestedScrollView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updatePadding(
                    insets,
                    bottom = true,
                )

                windowInsets
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(nestedScrollView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(noElementsNestedScrollView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        // Play top songs, then rest of artists songs
        playFloatingActionButton.setOnClickListener {
            play()
        }

        shuffleFloatingActionButton.setOnClickListener {
            shufflePlay()
        }

        toolbar.setupWithNavController(findNavController())

        albumsRecyclerView.adapter = albumsAdapter
        popularTracksRecyclerView.adapter = popularTracksAdapter
        appearsInAlbumRecyclerView.adapter = appearsInAlbumAdapter
        appearsInPlaylistRecyclerView.adapter = appearsInPlaylistAdapter

        viewModel.loadAlbum(artistUri)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }

    override fun onDestroyView() {
        albumsRecyclerView.adapter = null
        popularTracksRecyclerView.adapter = null
        appearsInAlbumRecyclerView.adapter = null
        appearsInPlaylistRecyclerView.adapter = null

        super.onDestroyView()
    }

    private fun play() {
        lifecycleScope.launch {
            combine(
                viewModel.enrichedPopularTracks,
                viewModel.artistTracks,
            ) { popular, allTracks ->
                val popularList = (popular as? FlowResult.Success)?.data ?: emptyList()
                val allList = (allTracks as? FlowResult.Success)
                    ?.data?.items?.filterIsInstance<Audio>() ?: emptyList()
                viewModel.buildPlayQueue(popularList, allList)
            }.first { it.isNotEmpty() }.let { queue ->
                viewModel.playAudio(queue, 0)
            }
        }
    }

    private fun shufflePlay() {
        lifecycleScope.launch {
            viewModel.artistTracks.collectLatest { requestStatus ->
                if (requestStatus is FlowResult.Success) {
                    val tracks = requestStatus.data.items.filterIsInstance<Audio>()
                    viewModel.playAudio(tracks.shuffled(), 0)
                }
            }
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.artist.collectLatest {
                        linearProgressIndicator.setProgressCompat(it)

                        when (it) {
                            is FlowResult.Loading -> Unit

                            is FlowResult.Success -> {
                                val (artist, artistWorks) = it.data

                                artist.name?.also { artistName ->
                                    toolbar.title = artistName
                                    artistNameTextView.text = artistName
                                } ?: run {
                                    toolbar.setTitle(R.string.artist_unknown)
                                    artistNameTextView.setText(R.string.artist_unknown)
                                }

                                thumbnailImageView.loadThumbnail(
                                    artist.thumbnail,
                                    placeholder = R.drawable.ic_person
                                )

                                albumsAdapter.submitList(artistWorks.albums)
                                appearsInAlbumAdapter.submitList(artistWorks.appearsInAlbum)
                                appearsInPlaylistAdapter.submitList(artistWorks.appearsInPlaylist)

                                val isAlbumsEmpty = artistWorks.albums.isEmpty()
                                albumsLinearLayout.isVisible = !isAlbumsEmpty

                                val isAppearsInAlbumEmpty = artistWorks.appearsInAlbum.isEmpty()
                                appearsInAlbumLinearLayout.isVisible = !isAppearsInAlbumEmpty

                                val isAppearsInPlaylistEmpty =
                                    artistWorks.appearsInPlaylist.isEmpty()
                                appearsInPlaylistLinearLayout.isVisible = !isAppearsInPlaylistEmpty

                                val isEmpty = listOf(
                                    isAlbumsEmpty,
                                    isAppearsInAlbumEmpty,
                                    isAppearsInPlaylistEmpty,
                                ).all { empty -> empty }

                                nestedScrollView.isVisible = !isEmpty
                                noElementsNestedScrollView.isVisible = isEmpty
                                artistButtonsLinearLayout.isVisible = true
                            }

                            is FlowResult.Error -> {
                                Log.e(LOG_TAG, "Error loading artist", it.throwable)
                            }
                        }
                    }
                }

                launch {
                    viewModel.enrichedPopularTracks.collectLatest { result ->
                        when (result) {
                            is FlowResult.Loading -> {}

                            is FlowResult.Success -> {
                                val tracks = result.data
                                popularTracksAdapter.submitList(tracks)
                                popularTracksLinearLayout.isVisible = tracks.isNotEmpty()
                            }

                            is FlowResult.Error -> {
                                popularTracksAdapter.submitList(emptyList())
                                popularTracksLinearLayout.isVisible = false
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = ArtistFragment::class.simpleName!!

        private const val ARG_ARTIST_URI = "artist_uri"

        /**
         * Create a [Bundle] to use as the arguments for this fragment.
         * @param artistUri The URI of the artist to display
         */
        fun createBundle(
            artistUri: Uri,
        ) = Bundle {
            putParcelable(ARG_ARTIST_URI, artistUri)
        }
    }
}
