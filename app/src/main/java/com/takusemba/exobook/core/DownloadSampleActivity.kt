package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.App
import com.takusemba.exobook.R
import java.io.IOException

class DownloadSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null
    private var downloadHelper: DownloadHelper? = null

    private val downloadStateListener = object : DownloadManager.Listener {

        override fun onInitialized(downloadManager: DownloadManager) {
            val download = downloadManager.downloadIndex.getDownload(CONTENT_ID) ?: return
            val text = when (download.state) {
                Download.STATE_DOWNLOADING -> {
                    getString(R.string.message_downloading)
                }
                Download.STATE_COMPLETED -> {
                    getString(R.string.message_downloaded_completed)
                }
                Download.STATE_REMOVING -> {
                    getString(R.string.message_downloaded_removing)
                }
                Download.STATE_FAILED -> {
                    getString(R.string.message_downloaded_failed)
                }
                Download.STATE_STOPPED -> {
                    getString(R.string.message_downloaded_stopped)
                }
                else -> ""
            }
            findViewById<TextView>(R.id.download_text_view).text = text
        }

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download
        ) {
            val text = when (download.state) {
                Download.STATE_DOWNLOADING -> {
                    getString(R.string.message_downloading)
                }
                Download.STATE_COMPLETED -> {
                    getString(R.string.message_downloaded_completed)
                }
                Download.STATE_REMOVING -> {
                    getString(R.string.message_downloaded_removing)
                }
                Download.STATE_FAILED -> {
                    getString(R.string.message_downloaded_failed)
                }
                Download.STATE_STOPPED -> {
                    getString(R.string.message_downloaded_stopped)
                }
                else -> ""
            }
            findViewById<TextView>(R.id.download_text_view).text = text
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            findViewById<TextView>(R.id.download_text_view).text = ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        val toggleButton = findViewById<Button>(R.id.toggle_download_button)
        toggleButton.setOnClickListener {
            val downloadManager = (application as App).downloadManager
            val download = downloadManager.downloadIndex.getDownload(CONTENT_ID)
            if (download?.state == Download.STATE_COMPLETED) {
                removeDownload()
            } else {
                addDownload()
            }
        }

        val playButton = findViewById<Button>(R.id.play_download_button)
        playButton.setOnClickListener {
            val downloadManager = (application as App).downloadManager
            val download = downloadManager.downloadIndex.getDownload(CONTENT_ID)
            if (download?.state == Download.STATE_COMPLETED) {
                initializePlayer()
            } else {
                Toast.makeText(
                    this,
                    R.string.message_downloaded_content_not_found,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        (application as App).downloadManager.addListener(downloadStateListener)
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadHelper?.release()
        (application as App).downloadManager.removeListener(downloadStateListener)
    }

    private fun addDownload() {
        if (this.downloadHelper != null) {
            downloadHelper?.release()
        }
        val userAgent = Util.getUserAgent(this, "SampleApp")
        val downloadHelper = DownloadHelper.forDash(
            this,
            URI,
            DefaultHttpDataSourceFactory(userAgent),
            DefaultRenderersFactory(this)
        )
        downloadHelper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(helper: DownloadHelper) {
                // select the lowest bitrate
                val parameters =
                    DefaultTrackSelector.Parameters.getDefaults(this@DownloadSampleActivity)
                        .buildUpon()
                        .setForceLowestBitrate(true)
                        .build()
                // clear all tracks for all periods
                for (periodIndex in 0 until downloadHelper.periodCount) {
                    downloadHelper.clearTrackSelections(periodIndex)
                    val mappedTrackInfo = helper.getMappedTrackInfo(periodIndex)
                    // enable only video track
                    for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
                        val trackType = mappedTrackInfo.getRendererType(rendererIndex)
                        if (trackType == C.TRACK_TYPE_VIDEO) {
                            downloadHelper.addTrackSelectionForSingleRenderer(
                                periodIndex,
                                rendererIndex,
                                parameters,
                                emptyList()
                            )
                        }
                    }
                }
                // download content
                val downloadRequest = helper.getDownloadRequest(CONTENT_ID, null)
                DownloadService.sendAddDownload(
                    this@DownloadSampleActivity,
                    DownloadSampleService::class.java,
                    downloadRequest,
                    false
                )
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                Log.d(TAG, "onPrepareError: ${e.message}")
            }
        })
        this.downloadHelper = downloadHelper
    }

    private fun removeDownload() {
        DownloadService.sendRemoveDownload(
            this,
            DownloadSampleService::class.java,
            CONTENT_ID,
            false
        )
    }

    private fun initializePlayer() {
        val player = SimpleExoPlayer.Builder(this).build()
        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val dataSourceFactory = (application as App).buildCacheDataSourceFactory()
        val mediaSource = DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(URI)

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.prepare(mediaSource)
        player.playWhenReady = true

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    companion object {

        private const val TAG = "DownloadSampleActivity"
        private const val CONTENT_ID = "tears"

        private val URI =
            Uri.parse("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd")
    }
}