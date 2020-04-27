package com.takusemba.exobook.extension

import android.net.Uri
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class LeanbackSampleFragment : VideoSupportFragment() {

    private var player: SimpleExoPlayer? = null

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        val player = SimpleExoPlayer.Builder(requireContext()).build()
        val adapter = LeanbackPlayerAdapter(requireContext(), player, UPDATE_INTERVAL)
        val playerGlue = PlaybackTransportControlGlue(activity, adapter)
        playerGlue.host = VideoSupportFragmentGlueHost(this)
        playerGlue.subtitle = "Leanback Subtitle"
        playerGlue.title = "Leanback Title"

        val userAgent = Util.getUserAgent(requireContext(), "SampleApp")
        val dataSourceFactory = DefaultDataSourceFactory(requireContext(), userAgent)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.prepare(mediaSource)
        player.playWhenReady = true

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    companion object {

        private const val UPDATE_INTERVAL = 1000

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    }
}