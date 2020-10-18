package com.takusemba.exobook.core

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.analytics.PlaybackStats
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.R

class DebugSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var debugTextViewHelper: DebugTextViewHelper? = null
    private var statsListener: PlaybackStatsListener? = null

    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        val button = findViewById<Button>(R.id.select_tracks_button)
        button.setOnClickListener {
            val mappedTrackInfo = trackSelector?.currentMappedTrackInfo
            if (mappedTrackInfo != null) {
                if (dialog != null) {
                    dialog?.dismiss()
                    dialog = null
                }
                for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
                    val trackType = mappedTrackInfo.getRendererType(rendererIndex)
                    if (trackType == DIALOG_RENDERER_INDEX) {
                        dialog = TrackSelectionDialogBuilder(
                            this,
                            "Track Selection",
                            trackSelector as DefaultTrackSelector,
                            rendererIndex
                        ).build()
                        dialog?.show()
                    }
                }
            }
        }
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
        val trackSelector = DefaultTrackSelector(this)
        val player = SimpleExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val userAgent = Util.getUserAgent(this, "SampleApp")
        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)

        val eventLogger = EventLogger(trackSelector)
        player.addAnalyticsListener(eventLogger)

        val statsListener = PlaybackStatsListener(
            true,
            PlaybackStatsListener.Callback { _: EventTime, playbackStats: PlaybackStats ->
                Log.d("StatsListener", "joinTime: " + playbackStats.meanJoinTimeMs)
                Log.d("StatsListener", "RebufferTime: " + playbackStats.meanRebufferTimeMs)
                Log.d("StatsListener", "SeekTime: " + playbackStats.meanSeekTimeMs)
            }
        )
        player.addAnalyticsListener(statsListener)

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.prepare(mediaSource)
        player.playWhenReady = true

        val debugTextView = findViewById<TextView>(R.id.debug_text_view)
        debugTextViewHelper = DebugTextViewHelper(player, debugTextView)
        debugTextViewHelper?.start()

        this.statsListener = statsListener
        this.trackSelector = trackSelector
        this.player = player
    }

    private fun releasePlayer() {
        debugTextViewHelper?.stop()
        debugTextViewHelper = null
        statsListener?.finishAllSessions()
        statsListener = null
        player?.stop()
        player?.release()
        player = null
        trackSelector = null
    }

    companion object {

        private val URI =
            Uri.parse("https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8")

        private const val DIALOG_RENDERER_INDEX = C.TRACK_TYPE_VIDEO
    }
}