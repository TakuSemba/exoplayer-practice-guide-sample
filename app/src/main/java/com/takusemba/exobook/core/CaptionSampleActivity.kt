package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.takusemba.exobook.R

class CaptionSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        val player = SimpleExoPlayer.Builder(this).build()

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val mediaItem = MediaItem.fromUri(URI)
        val dataSourceFactory = DefaultDataSourceFactory(this)
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
        val captionMediaSource = when (TYPE) {
            CaptionType.TTML -> {
                val captionFormat = Format.createTextSampleFormat(
                    /* id= */ null,
                    /* sampleMimeType= */ MimeTypes.APPLICATION_TTML,
                    /* selectionFlags= */ C.SELECTION_FLAG_DEFAULT,
                    /* language= */ "en"
                )
                SingleSampleMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(TTML_URL, captionFormat, C.TIME_UNSET)
            }
            CaptionType.WEB_VTT -> {
                val captionFormat = Format.createTextSampleFormat(
                    /* id= */ null,
                    /* sampleMimeType= */ MimeTypes.TEXT_VTT,
                    /* selectionFlags= */ C.SELECTION_FLAG_DEFAULT,
                    /* language= */ "en"
                )
                SingleSampleMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(WEB_VTT_URL, captionFormat, C.TIME_UNSET)
            }
        }
        val mediaSource = MergingMediaSource(videoSource, captionMediaSource)

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    companion object {

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        private val TTML_URL = Uri.parse("asset:///caption/ttml_sample.xml")

        private val WEB_VTT_URL = Uri.parse("asset:///caption/webvtt_sample.vtt")

        private val TYPE = CaptionType.WEB_VTT

        enum class CaptionType { TTML, WEB_VTT }
    }
}