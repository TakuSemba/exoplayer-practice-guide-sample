package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.takusemba.exobook.R

class SchedulerSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button)

        val button = findViewById<Button>(R.id.button)
        button.text = getString(R.string.button_start_download)
        button.setOnClickListener {
            val downloadRequest = DownloadRequest(
                URI.toString(),
                DownloadRequest.TYPE_PROGRESSIVE,
                URI,
                emptyList(),
                null,
                null
            )
//            DownloadService.sendRemoveDownload(
//                this,
//                SchedulerSampleService::class.java,
//                CONTENT_ID,
//                false
//            )
            DownloadService.sendAddDownload(
                this,
                SchedulerSampleService::class.java,
                downloadRequest,
                false
            )
        }
    }

    companion object {

        private const val CONTENT_ID = "big_buck_bunny"

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    }
}