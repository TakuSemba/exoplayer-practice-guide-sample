package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.R

class CodecSampleActivity : AppCompatActivity() {

    private val renderersFactory by lazy {
        DefaultRenderersFactory(this).setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
    }
    private val player by lazy { SimpleExoPlayer.Builder(this, renderersFactory).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val userAgent = Util.getUserAgent(this, "SampleApp")
        val dataSourceFactory = DefaultHttpDataSourceFactory(userAgent)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)

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
    }
}