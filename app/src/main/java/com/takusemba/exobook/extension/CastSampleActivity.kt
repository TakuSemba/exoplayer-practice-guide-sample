package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.DefaultMediaItemConverter
import com.google.android.exoplayer2.ext.cast.MediaItem
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.takusemba.exobook.R

class CastSampleActivity : AppCompatActivity() {

    private val uri =
        Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val player = SimpleExoPlayer.Builder(this).build()
        val mediaItemConverter = DefaultMediaItemConverter()
        val castContext = CastContext.getSharedInstance(this)
        val castPlayer = CastPlayer(castContext)

        castPlayer.setSessionAvailabilityListener(object : SessionAvailabilityListener {

            override fun onCastSessionAvailable() {
                val item = MediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(MimeTypes.VIDEO_MP4)
                    .setTitle("Big Buck Bunny")
                    .build()
                castPlayer.loadItem(mediaItemConverter.toMediaQueueItem(item), 0L)
            }

            override fun onCastSessionUnavailable() {
                castPlayer.stop()
            }
        })

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val userAgent = Util.getUserAgent(this, "SampleApp")
        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)

        player.prepare(mediaSource)
        player.playWhenReady = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_cast, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.item_cast)
        return true
    }
}