package org.lineageos.twelve.datasources.lastfm

import org.lineageos.twelve.R
import org.lineageos.twelve.models.ProviderArgument

class LastfmDataSource {
    companion object {
        val ARG_SERVER = ProviderArgument(
            key = "lastfm_server",
            type = String::class,
            nameStringResId = R.string.last_fm_server,
            required = true,
            hidden = false,
        )

        val ARG_API_KEY = ProviderArgument(
            key = "lastfm_api_key",
            type = String::class,
            nameStringResId = R.string.last_fm_api_key,
            required = true,
            hidden = true,
        )
    }
}
