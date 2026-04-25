/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getColorFromAttr
import org.lineageos.twelve.ext.loadThumbnail
import org.lineageos.twelve.models.Album

class SearchAlbumCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private val imageView: ShapeableImageView
    private val titleTextView: TextView
    private val artistTextView: TextView

    init {
        orientation = VERTICAL
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp120 = (120 * resources.displayMetrics.density).toInt()
        layoutParams = LayoutParams(dp120, LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp8, dp8, dp8, dp8)
        }

        imageView = ShapeableImageView(context).apply {
            layoutParams = LayoutParams(dp120, dp120)
            // Rounded rect for albums
            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(12 * resources.displayMetrics.density)
                .build()
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        titleTextView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            setPadding(dp4, dp4, dp4, 0)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            maxLines = 1
            textAlignment = TEXT_ALIGNMENT_CENTER
        }

        artistTextView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            setPadding(dp4, 0, dp4, 0)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
            maxLines = 1
            textAlignment = TEXT_ALIGNMENT_CENTER
        }

        addView(imageView)
        addView(titleTextView)
        addView(artistTextView)
    }

    fun bind(album: Album) {
        imageView.loadThumbnail(
            album.thumbnail,
            placeholder = R.drawable.ic_album,
        )
        titleTextView.text = album.title
        artistTextView.text = album.artistName
    }
}
