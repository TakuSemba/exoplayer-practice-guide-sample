package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.takusemba.exobook.R

class RtmpSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val player = SimpleExoPlayer.Builder(this).build()

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val dataSourceFactory = RtmpDataSourceFactory()
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)

        player.prepare(mediaSource)
        player.playWhenReady = true
    }

    companion object {

        private val URI = Uri.parse("rtmp://ip-address:1935/live/ext-test")
    }
}