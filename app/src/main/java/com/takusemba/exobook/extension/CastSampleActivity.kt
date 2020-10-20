package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.DefaultMediaItemConverter
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.takusemba.exobook.R
import com.takusemba.exobook.core.UiSampleActivity

class CastSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val mediaItemConverter = DefaultMediaItemConverter()
        val castContext = CastContext.getSharedInstance(this)
        val castPlayer = CastPlayer(castContext)

        castPlayer.setSessionAvailabilityListener(object : SessionAvailabilityListener {

            override fun onCastSessionAvailable() {
                val item = MediaItem.Builder()
                    .setUri(URI)
                    .setMimeType(MimeTypes.VIDEO_MP4)
                    .build()
                castPlayer.loadItem(mediaItemConverter.toMediaQueueItem(item), 0L)
            }

            override fun onCastSessionUnavailable() {
                castPlayer.stop()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_cast, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.item_cast)
        return true
    }

    private fun initializePlayer() {
        val player = SimpleExoPlayer.Builder(this).build()

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val mediaItem = MediaItem.fromUri(URI)
        val dataSourceFactory = DefaultDataSourceFactory(this)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

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
    }
}