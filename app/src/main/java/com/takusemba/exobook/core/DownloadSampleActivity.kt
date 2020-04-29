package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.App
import com.takusemba.exobook.R
import java.io.IOException

class DownloadSampleActivity : AppCompatActivity() {

    private var downloadHelper: DownloadHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button)

        val button = findViewById<Button>(R.id.button)
        button.text = getString(R.string.button_toggle_download)
        button.setOnClickListener {
            val downloadManager = (application as App).downloadManager
            val download = downloadManager.downloadIndex.getDownload(CONTENT_ID)
            if (download != null) {
                removeDownload()
            } else {
                addDownload()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadHelper?.release()
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
                val downloadRequest = helper.getDownloadRequest(null)
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

    companion object {

        private const val TAG = "DownloadSampleActivity"
        private const val CONTENT_ID = "tears"

        private val URI =
            Uri.parse("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd")
    }
}