package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.takusemba.exobook.R

class BasicSampleActivity : AppCompatActivity() {

    private val userAgent by lazy { Util.getUserAgent(this, "SampleApp") }
    private val player by lazy { SimpleExoPlayer.Builder(this).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        val childMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)

        val mediaSource = when (TYPE) {
            MediaSourceType.DEFAULT -> childMediaSource
            MediaSourceType.CLIPPING -> ClippingMediaSource(childMediaSource, 0L, 60_000_000L)
            MediaSourceType.LOOPING -> LoopingMediaSource(childMediaSource)
            MediaSourceType.CONCATENATING -> ConcatenatingMediaSource(childMediaSource)
        }

        player.addListener(object : Player.EventListener {

            override fun onTimelineChanged(timeline: Timeline, reason: Int) = Unit

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) = Unit

            override fun onLoadingChanged(isLoading: Boolean) = Unit

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) = Unit

            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) = Unit

            override fun onIsPlayingChanged(isPlaying: Boolean) = Unit

            override fun onRepeatModeChanged(repeatMode: Int) = Unit

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = Unit

            override fun onPlayerError(error: ExoPlaybackException) = Unit

            override fun onPositionDiscontinuity(reason: Int) = Unit

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = Unit

            override fun onSeekProcessed() = Unit
        })

        player.addAnalyticsListener(object : AnalyticsListener {})

        player.addVideoListener(object : VideoListener {})

        player.addAudioListener(object : AudioListener {})

        player.addMetadataOutput { metadata -> }

        player.addTextOutput { cue -> }

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.prepare(mediaSource)
        player.playWhenReady = true
    }

    override fun onStart() {
        super.onStart()
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    companion object {

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        private val TYPE = MediaSourceType.DEFAULT

        enum class MediaSourceType { DEFAULT, CLIPPING, LOOPING, CONCATENATING }
    }
}