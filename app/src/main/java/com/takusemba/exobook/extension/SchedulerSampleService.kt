package com.takusemba.exobook.extension

import android.app.Notification
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ext.workmanager.WorkManagerScheduler
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.Util
import java.io.File

class SchedulerSampleService : DownloadService(NOTIFICATION_ID) {

    private val userAgent by lazy { Util.getUserAgent(this, "SampleApp") }
    private val dataSourceFactory by lazy { DefaultHttpDataSourceFactory(userAgent) }
    private val databaseProvider by lazy { ExoDatabaseProvider(this) }
    private val downloadContentDirectory by lazy {
        File(getExternalFilesDir(null), DOWNLOAD_CONTENT_DIRECTORY)
    }
    private val downloadCache by lazy {
        SimpleCache(
            downloadContentDirectory,
            NoOpCacheEvictor(),
            databaseProvider
        )
    }

    override fun onCreate() {
        super.onCreate()
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download
            ) {
                val notification: Notification = when (download.state) {
                    Download.STATE_COMPLETED -> {
                        DownloadNotificationHelper(
                            this@SchedulerSampleService,
                            "channel-id"
                        ).buildDownloadCompletedNotification(
                            android.R.drawable.stat_sys_download_done,
                            null,
                            null
                        )
                    }
                    Download.STATE_FAILED -> {
                        DownloadNotificationHelper(
                            this@SchedulerSampleService,
                            "channel-id"
                        ).buildDownloadFailedNotification(
                            android.R.drawable.stat_sys_download_done,
                            null,
                            null
                        )
                    }
                    else -> return
                }
                NotificationUtil.setNotification(
                    this@SchedulerSampleService,
                    NOTIFICATION_ID,
                    notification
                )
            }
        })
    }

    override fun getDownloadManager(): DownloadManager {
        return DownloadManager(
            this,
            databaseProvider,
            downloadCache,
            dataSourceFactory
        )
    }

    override fun getForegroundNotification(downloads: MutableList<Download>): Notification {
        return DownloadNotificationHelper(this, "channel-id")
            .buildProgressNotification(
                android.R.drawable.stat_sys_download,
                null,
                null,
                downloads
            )
    }

    override fun getScheduler(): Scheduler? {
        return WorkManagerScheduler(WORK_NAME)
    }

    companion object {

        private const val NOTIFICATION_ID = 0
        private const val WORK_NAME = "DownloadScheduler"
        private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
    }
}