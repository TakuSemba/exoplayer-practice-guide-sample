package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.ui.StyledPlayerView
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
        val playerView = findViewById<StyledPlayerView>(R.id.player_view)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setAdViewProvider(playerView)
            .setAdsLoaderProvider {
                initializeAdsLoader()
                return@setAdsLoaderProvider adsLoader
            }

        val player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        this.player = player

        playerView.player = player

        val mediaItem = MediaItem.Builder()
            .setUri(URI)
            .setAdTagUri(AD_URI)
            .build()

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun initializeAdsLoader() {
        if (adsLoader != null) {
            releaseAdsLoader()
        }
        adsLoader = ImaAdsLoader.Builder(this)
            .setMaxMediaBitrate(1_000_000)
            .setMediaLoadTimeoutMs(5000)
            .setMediaLoadTimeoutMs(5000)
            .setAdEventListener { adEvent -> /* do something */ }
            .build()
        adsLoader?.setPlayer(player)
    }

    private fun releasePlayer() {
        releaseAdsLoader()
        player?.stop()
        player?.release()
        player = null
    }

    private fun releaseAdsLoader() {
        adsLoader?.stop()
        adsLoader?.release()
        adsLoader = null
    }

    companion object {

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        private val AD_URI =
            Uri.parse("https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpost&cmsid=496&vid=short_onecue&correlator=")
    }
}