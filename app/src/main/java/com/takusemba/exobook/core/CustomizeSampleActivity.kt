package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.R
import java.io.IOException
import java.util.*

class CustomizeSampleActivity : AppCompatActivity() {

    private val player by lazy {

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
            .createDefaultLoadControl()

        val bandwidthMeter = DefaultBandwidthMeter.Builder(this)
            .setSlidingWindowMaxWeight(DefaultBandwidthMeter.DEFAULT_SLIDING_WINDOW_MAX_WEIGHT)
            .build()

        SimpleExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val userAgent = Util.getUserAgent(this, "SampleApp")
        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(MyLoadErrorHandlingPolicy())
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

    private class MyLoadErrorHandlingPolicy :
        LoadErrorHandlingPolicy by DefaultLoadErrorHandlingPolicy() {

        override fun getRetryDelayMsFor(
            dataType: Int,
            loadDurationMs: Long,
            exception: IOException?,
            errorCount: Int
        ): Long {
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

        override fun getPassthroughDecoderInfo(): MediaCodecInfo? {
            return exoDefault.passthroughDecoderInfo
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

        private val URI =
            Uri.parse("https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8")
    }
}