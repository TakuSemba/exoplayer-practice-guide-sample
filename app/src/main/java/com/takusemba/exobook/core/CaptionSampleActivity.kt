package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.R

class CaptionSampleActivity : AppCompatActivity() {

    private val userAgent by lazy { Util.getUserAgent(this, "SampleApp") }
    private val player by lazy { SimpleExoPlayer.Builder(this).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)
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

        private val TTML_URL = Uri.parse("asset:///caption/ttml_sample.xml")

        private val WEB_VTT_URL = Uri.parse("asset:///caption/webvtt_sample.vtt")

        private val TYPE = CaptionType.WEB_VTT

        enum class CaptionType { TTML, WEB_VTT }
    }
}