package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
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

        val playerView = findViewById<StyledPlayerView>(R.id.player_view)
        playerView.player = player
        playerView.setShowSubtitleButton(true)

        val captionMediaItem = when (TYPE) {
            CaptionType.TTML -> MediaItem.Subtitle(TTML_URL, MimeTypes.APPLICATION_TTML, "en")
            CaptionType.WEB_VTT -> MediaItem.Subtitle(WEB_VTT_URL, MimeTypes.TEXT_VTT, "en")
        }
        val mediaItem = MediaItem.Builder()
            .setUri(URI)
            .setSubtitles(listOf(captionMediaItem))
            .build()

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

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