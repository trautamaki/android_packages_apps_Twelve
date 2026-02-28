/*
 * SPDX-FileCopyrightText: 2024-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.ext.setProgressCompat
import org.lineageos.twelve.ext.updatePadding
import org.lineageos.twelve.models.ActivityTab
import org.lineageos.twelve.models.Album
import org.lineageos.twelve.models.Artist
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.Genre
import org.lineageos.twelve.models.Playlist
import org.lineageos.twelve.ui.recyclerview.SimpleListAdapter
import org.lineageos.twelve.ui.recyclerview.UniqueItemDiffCallback
import org.lineageos.twelve.ui.views.ActivityTabView
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.viewmodels.ActivityViewModel

/**
 * User activity, notifications and recommendations.
 */
class ActivityFragment : Fragment(R.layout.fragment_activity) {
    // View models
    private val viewModel by viewModels<ActivityViewModel>()

    // Views
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)

    // RecyclerView
    private val adapter by lazy {
        object : SimpleListAdapter<ActivityTab, ActivityTabView>(
            UniqueItemDiffCallback(),
            ::ActivityTabView,
        ) {
            override fun ViewHolder.onBindView(item: ActivityTab) {
                view.setOnItemClickListener { mediaItem ->
                    when (mediaItem) {
                        is Album -> findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_album,
                            AlbumFragment.createBundle(mediaItem.uri)
                        )

                        is Artist -> findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_artist,
                            ArtistFragment.createBundle(mediaItem.uri)
                        )

                        is Audio -> viewModel.playAudio(
                            mediaItem,
                            item.items.filterIsInstance<Audio>()
                        )

                        is Genre -> findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_genre,
                            GenreFragment.createBundle(mediaItem.uri)
                        )

                        is Playlist -> findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_playlist,
                            PlaylistFragment.createBundle(mediaItem.uri)
                        )
                    }
                }
                view.setOnItemLongClickListener { mediaItem ->
                    findNavController().navigateSafe(
                        R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                        MediaItemBottomSheetDialogFragment.createBundle(mediaItem.uri)
                    )
                    true
                }

                view.setActivityTab(item)
            }
        }
    }

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            v.updatePadding(
                insets,
                start = true,
                end = true,
                bottom = true,
            )

            windowInsets
        }

        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null

        super.onDestroyView()
    }

    private suspend fun loadData() {
        lifecycleScope.launch {
            viewModel.lastfmActivityTabs.collectLatest { tabs ->
                val filtered = adapter.currentList.filter { !it.id.startsWith("lastfm_") }
                adapter.submitList(tabs + filtered)
            }
        }

        viewModel.activity.collectLatest {
            linearProgressIndicator.setProgressCompat(it)

            when (it) {
                is FlowResult.Loading -> {
                }

                is FlowResult.Success -> {
                    val lastfmTabs =                         adapter.currentList.filter { tab -> tab.id.startsWith("lastfm_") }
                    adapter.submitList(lastfmTabs + it.data)

                    val isEmpty = it.data.isEmpty() && lastfmTabs.isEmpty()
                    recyclerView.isVisible = !isEmpty
                    noElementsLinearLayout.isVisible = isEmpty
                }

                is FlowResult.Error -> {
                    recyclerView.isVisible = false
                    noElementsLinearLayout.isVisible = true
                }
            }
        }
    }
}
