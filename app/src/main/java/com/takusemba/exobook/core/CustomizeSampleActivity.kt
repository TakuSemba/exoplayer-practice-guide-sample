package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.App
import com.takusemba.exobook.R
import java.util.Collections

class CustomizeSampleActivity : AppCompatActivity() {

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

        val parameters = DefaultTrackSelector.ParametersBuilder(this)
            .setMaxAudioChannelCount(2)
            .setMaxVideoBitrate(1_000_000)
            .setPreferredAudioLanguage("ja")
            .build()

        val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory(
            AdaptiveTrackSelection.DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
            AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
            AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
            AdaptiveTrackSelection.DEFAULT_BANDWIDTH_FRACTION
        )

        val trackSelector = DefaultTrackSelector(
            parameters,
            adaptiveTrackSelectionFactory
        )

        val renderersFactory = DefaultRenderersFactory(this)
            .setMediaCodecSelector(MyMediaCodecSelector())

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        val player = SimpleExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setBandwidthMeter(getDefaultBandwidthMeter())
            .build()

        val playerView = findViewById<StyledPlayerView>(R.id.player_view)
        playerView.player = player


        val mediaItem = MediaItem.fromUri(URI)
        val dataSourceFactory = DefaultDataSourceFactory(this)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(MyLoadErrorHandlingPolicy())
            .createMediaSource(mediaItem)

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun getDefaultBandwidthMeter(): BandwidthMeter {
        return when (BANDWIDTH_SCOPE) {
            BandwidthScope.SESSION -> DefaultBandwidthMeter.Builder(this)
                .setInitialBitrateEstimate(DEFAULT_INITIAL_BANDWIDTH)
                .setResetOnNetworkTypeChange(true)
                .build()
            BandwidthScope.APPLICATION -> {
                return singletonBandwidthMeter ?: DefaultBandwidthMeter.Builder(this)
                    .setInitialBitrateEstimate(DEFAULT_INITIAL_BANDWIDTH)
                    .setResetOnNetworkTypeChange(true)
                    .build()
                    .also { bandwidthMeter ->
                        singletonBandwidthMeter = bandwidthMeter
                    }
            }
            BandwidthScope.LIFETIME -> {
                return singletonBandwidthMeter ?: DefaultBandwidthMeter.Builder(this)
                    .setInitialBitrateEstimate(
                        (application as App).prefs.getLong(
                            KEY_LAST_ESTIMATED_BANDWIDTH,
                            DEFAULT_INITIAL_BANDWIDTH
                        )
                    )
                    .setResetOnNetworkTypeChange(true)
                    .build()
                    .also { bandwidthMeter ->
                        singletonBandwidthMeter = bandwidthMeter
                    }
            }
        }
    }

    private class MyLoadErrorHandlingPolicy :
        LoadErrorHandlingPolicy by DefaultLoadErrorHandlingPolicy() {

        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
            val exception = loadErrorInfo.exception
            if (exception is HttpDataSource.InvalidResponseCodeException) {
                val responseCode = exception.responseCode
                return if (responseCode in 500..599) RETRY_DELAY else C.TIME_UNSET
            }
            return C.TIME_UNSET
        }

        override fun getMinimumLoadableRetryCount(dataType: Int): Int {
            return MINIMUM_RETRY_COUNT
        }

        companion object {
            private const val RETRY_DELAY = 5000L
            private const val MINIMUM_RETRY_COUNT = 3
        }
    }

    class MyMediaCodecSelector : MediaCodecSelector {

        private val exoDefault: MediaCodecSelector = MediaCodecSelector.DEFAULT

        override fun getDecoderInfos(
            mimeType: String,
            requiresSecureDecoder: Boolean,
            requiresTunnelingDecoder: Boolean
        ): List<MediaCodecInfo> {
            val exoDefaultMediaCodecInfos = exoDefault.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            val mutableMediaCodecInfos = exoDefaultMediaCodecInfos.toMutableList()
            applyWorkarounds(mutableMediaCodecInfos)
            return Collections.unmodifiableList(mutableMediaCodecInfos)
        }

        private fun applyWorkarounds(codecInfos: MutableList<MediaCodecInfo>) {
            if (Util.MODEL == "HogeDevice") {
                applyHogeDeviceWorkaround(codecInfos)
            }
        }

        private fun applyHogeDeviceWorkaround(codecInfos: MutableList<MediaCodecInfo>) {
            if (codecInfos.size > 1 && codecInfos.any { it.name == "HugaDecoder" }) {
                val index = codecInfos.indexOfFirst { it.name == "HugaDecoder" }
                val hugaDecoder = codecInfos.removeAt(index)
                codecInfos.add(hugaDecoder)
            }
        }
    }

    companion object {

        private var singletonBandwidthMeter: BandwidthMeter? = null

        private val URI =
            Uri.parse("https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8")

        private val BANDWIDTH_SCOPE = BandwidthScope.SESSION

        enum class BandwidthScope { SESSION, APPLICATION, LIFETIME }

        private const val DEFAULT_INITIAL_BANDWIDTH = 500_000L
        private const val KEY_LAST_ESTIMATED_BANDWIDTH = "key_last_estimated_bandwidth"
    }
}