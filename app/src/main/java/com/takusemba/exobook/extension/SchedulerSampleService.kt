package com.takusemba.exobook.extension

import android.app.Notification
import com.google.android.exoplayer2.ext.workmanager.WorkManagerScheduler
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.util.NotificationUtil
import com.takusemba.exobook.App
import com.takusemba.exobook.App.Companion.CHANNEL_ID
import com.takusemba.exobook.R

class SchedulerSampleService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.channel_name,
    R.string.channel_description
) {

    private val downloadListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download
        ) {
            val notification: Notification = when (download.state) {
                Download.STATE_COMPLETED -> {
                    (application as App).notificationHelper.buildDownloadCompletedNotification(
                        android.R.drawable.stat_sys_download_done, null, null
                    )
                }
                Download.STATE_FAILED -> {
                    (application as App).notificationHelper.buildDownloadFailedNotification(
                        android.R.drawable.stat_sys_download_done, null, null
                    )
                }
                else -> return
            }
            NotificationUtil.setNotification(
                this@SchedulerSampleService,
                NOTIFICATION_ID + 1,
                notification
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        downloadManager.addListener(downloadListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadManager.removeListener(downloadListener)
    }

    override fun getDownloadManager(): DownloadManager {
        return (application as App).downloadManager
    }

    override fun getForegroundNotification(downloads: MutableList<Download>): Notification {
        return (application as App).notificationHelper
            .buildProgressNotification(android.R.drawable.stat_sys_download, null, null, downloads)
    }

    override fun getScheduler(): Scheduler? {
        return WorkManagerScheduler(WORK_NAME)
    }

    companion object {

        private const val NOTIFICATION_ID = 200
        private const val WORK_NAME = "DownloadScheduler"
    }
}