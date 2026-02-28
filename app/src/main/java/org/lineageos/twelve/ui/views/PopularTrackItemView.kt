package org.lineageos.twelve.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import org.lineageos.twelve.R
import org.lineageos.twelve.models.PopularTrack

class PopularTrackItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val trackNumberTextView by lazy { findViewById<TextView>(R.id.trackNumberTextView) }
    private val trackNameTextView by lazy { findViewById<TextView>(R.id.trackNameTextView) }
    private val listenersTextView by lazy { findViewById<TextView>(R.id.listenersTextView) }

    init {
        setCardBackgroundColor(
            resources.getColorStateList(R.color.list_item_background, context.theme)
        )
        cardElevation = 0f
        strokeWidth = 0

        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        inflate(context, R.layout.item_popular_track, this)
    }

    fun setItem(track: PopularTrack, position: Int) {
        trackNumberTextView.text = (position + 1).toString()
        trackNameTextView.text = track.name
        listenersTextView.text = track.listenerCount?.let {
            formatListeners(it)
        } ?: ""
    }

    private fun formatListeners(count: Int) = when {
        count >= 1_000_000 -> "%.1fM listeners".format(count / 1_000_000f)
        count >= 1_000 -> "${count / 1_000}K listeners"
        else -> "$count listeners"
    }
}
