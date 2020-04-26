package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util

class LeanbackSampleFragment : VideoSupportFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val player = SimpleExoPlayer.Builder(requireContext()).build()
        val adapter = LeanbackPlayerAdapter(requireContext(), player, UPDATE_INTERVAL)
        val playerGlue = PlaybackTransportControlGlue(activity, adapter)

        playerGlue.host = VideoSupportFragmentGlueHost(this)
        playerGlue.subtitle = "Leanback Subtitle"
        playerGlue.title = "Leanback Title"

        val userAgent = Util.getUserAgent(requireContext(), "SampleApp")
        val dataSourceFactory = DefaultHttpDataSourceFactory(userAgent)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)
        player.prepare(mediaSource)
    }

    companion object {
        private const val UPDATE_INTERVAL = 1000

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    }
}