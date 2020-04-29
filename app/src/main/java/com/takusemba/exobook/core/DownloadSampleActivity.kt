package com.takusemba.exobook.core

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.takusemba.exobook.App
import com.takusemba.exobook.R
import com.takusemba.exobook.extension.SchedulerSampleService

class DownloadSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button)

        val button = findViewById<Button>(R.id.button)
        button.text = getString(R.string.button_toggle_download)
        button.setOnClickListener {
            val downloadManager = (application as App).downloadManager
            val download = downloadManager.downloadIndex.getDownload(CONTENT_ID)
            if (download != null) {
                DownloadService.sendRemoveDownload(
                    this,
                    SchedulerSampleService::class.java,
                    CONTENT_ID,
                    false
                )
            } else {
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
                    DownloadSampleService::class.java,
                    downloadRequest,
                    false
                )
            }
        }
    }

    companion object {

        private const val CONTENT_ID = "big_buck_bunny"

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    }
}