package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.takusemba.exobook.R

class ImaSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null
    private var adsLoader: AdsLoader? = null

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
        val adsLoader = ImaAdsLoader.Builder(this)
            .setMaxMediaBitrate(1_000_000)
            .setMediaLoadTimeoutMs(5000)
            .setMediaLoadTimeoutMs(5000)
            .setAdEventListener { adEvent -> /* do something */ }
            .buildForAdTag(AD_URI)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val mediaItem = MediaItem.fromUri(URI)
        val dataSourceFactory = DefaultDataSourceFactory(this)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        adsLoader.setPlayer(player)
        val adsMediaSource = AdsMediaSource(
            mediaSource,
            dataSourceFactory,
            adsLoader,
            playerView
        )
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        adsLoader?.stop()
        adsLoader?.release()
        player = null
        adsLoader = null
    }

    companion object {

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        private val AD_URI =
            Uri.parse("https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpost&cmsid=496&vid=short_onecue&correlator=")
    }
}