package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.emsg.EventMessage
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Log
import com.takusemba.exobook.R

class MetadataSampleActivity : AppCompatActivity() {

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

        val dataSourceFactory = DefaultDataSourceFactory(this)
        val mediaSource = when (metadataType) {
            MetadataType.ID3 -> {
                val mediaItem = MediaItem.fromUri(HLS_URI)
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
            MetadataType.EMSG -> {
                val mediaItem = MediaItem.fromUri(DASH_URI)
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
        }

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()

        player.addMetadataOutput { metadata ->
            repeat(metadata.length()) { index ->
                val data = metadata[index]
                if (data is TextInformationFrame) {
                    Log.d("TimedMetadata", "${player.currentPosition}ms: ${data.value}")
                }
                if (data is EventMessage) {
                    Log.d(
                        "EventMessage",
                        "${player.currentPosition}ms: ${String(data.messageData)}"
                    )
                }
            }
        }

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    companion object {

        private val HLS_URI = Uri.parse("asset:///m3u8/id3_sample.m3u8")
        private val DASH_URI = Uri.parse("asset:///mpd/emsg_sample.mpd")

        private val metadataType = MetadataType.ID3

        enum class MetadataType { ID3, EMSG }
    }
}