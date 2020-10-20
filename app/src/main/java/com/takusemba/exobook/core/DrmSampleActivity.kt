package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.drm.LocalMediaDrmCallback
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.takusemba.exobook.R

class DrmSampleActivity : AppCompatActivity() {

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
        val mediaSource = when (DRM_SCHEME_TYPE) {
            DrmSchemeType.CLEARKEY -> {
                val mediaItem = MediaItem.fromUri(CLEARKEY_URI)
                val drmCallback = LocalMediaDrmCallback(CLEARKEY_RESPONSE.toByteArray())
                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        C.CLEARKEY_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(drmCallback)
                DashMediaSource.Factory(dataSourceFactory)
                    .setDrmSessionManager(drmSessionManager)
                    .createMediaSource(mediaItem)
            }
            DrmSchemeType.WIDEVINE -> {
                val drmCallback = HttpMediaDrmCallback(
                    "https://proxy.uat.widevine.com/proxy?provider=widevine_test",
                    DefaultHttpDataSourceFactory()
                )
                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(
                        C.WIDEVINE_UUID,
                        FrameworkMediaDrm.DEFAULT_PROVIDER
                    )
                    .build(drmCallback)
                val mediaItem = MediaItem.fromUri(WIDEVINE_URI)
                DashMediaSource.Factory(dataSourceFactory)
                    .setDrmSessionManager(drmSessionManager)
                    .createMediaSource(mediaItem)
            }
        }

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

        private val CLEARKEY_URI = Uri.parse("asset:///mpd/clearkey_sample.mpd")
        private val WIDEVINE_URI = Uri.parse("asset:///mpd/widevine_sample.mpd")

        private val CLEARKEY_RESPONSE = """
            {
                "keys":[
                    {
                        "kty":"oct",
                        "k":"VUYfNUpUK+ub3DZY8921Aw",
                        "kid":"7VUqBLkuMg+BvVsAKZeFEQ"
                    }
                ],
                "type":"temporary"
            }
        """.trimIndent()

        private val DRM_SCHEME_TYPE = DrmSchemeType.WIDEVINE

        enum class DrmSchemeType { CLEARKEY, WIDEVINE }
    }
}