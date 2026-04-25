/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.twelve.models.Artist
import org.lineageos.twelve.models.SearchItem
import org.lineageos.twelve.ui.views.SearchArtistCardView

class ArtistCardAdapter(
    private val artists: List<Artist>,
    private val onClick: (SearchItem.ArtistItem) -> Unit,
    private val onLongClick: (SearchItem.ArtistItem) -> Unit,
) : RecyclerView.Adapter<ArtistCardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        // your card view here
        SearchArtistCardView(parent.context)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]
        holder.view.bind(artist)
        holder.view.setOnClickListener { onClick(SearchItem.ArtistItem(artist)) }
        holder.view.setOnLongClickListener { onLongClick(SearchItem.ArtistItem(artist)); true }
    }

    override fun getItemCount() = artists.size

    class ViewHolder(val view: SearchArtistCardView) : RecyclerView.ViewHolder(view)
}
