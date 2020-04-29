package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.takusemba.exobook.App
import com.takusemba.exobook.R

class SchedulerSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    private val downloadStateListener = object : DownloadManager.Listener {

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
                else -> return
            }
            findViewById<TextView>(R.id.download_text_view).text = text
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        val toggleButton = findViewById<Button>(R.id.toggle_download_button)
        toggleButton.setOnClickListener {
            val downloadManager = (application as App).downloadManager
            val download = downloadManager.downloadIndex.getDownload(CONTENT_ID)
            if (download != null) {
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
        (application as App).downloadManager.removeListener(downloadStateListener)
    }

    private fun initializePlayer() {
        val player = SimpleExoPlayer.Builder(this).build()
        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val dataSourceFactory = (application as App).buildCacheDataSourceFactory()
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
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

    private fun addDownload() {
        val downloadRequest = DownloadRequest(
            CONTENT_ID,
            DownloadRequest.TYPE_PROGRESSIVE,
            URI,
            emptyList(),
            null,
            null
        )
        DownloadService.sendAddDownload(
            this,
            SchedulerSampleService::class.java,
            downloadRequest,
            false
        )
    }

    private fun removeDownload() {
        DownloadService.sendRemoveDownload(
            this,
            SchedulerSampleService::class.java,
            CONTENT_ID,
            false
        )
    }

    companion object {

        private const val CONTENT_ID = "big_buck_bunny"

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    }
}