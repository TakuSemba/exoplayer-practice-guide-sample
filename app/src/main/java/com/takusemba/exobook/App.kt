package com.takusemba.exobook

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.NotificationUtil
import java.io.File
import java.util.concurrent.Executors

class App : Application() {

    private val downloadDirectory by lazy { getExternalFilesDir(null) ?: filesDir }
    private val databaseProvider by lazy { ExoDatabaseProvider(this) }
    private val downloadCache by lazy {
        val downloadContentDirectory = File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
        SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider)
    }
    val downloadManager by lazy {
        DownloadManager(
            this,
            databaseProvider,
            downloadCache,
            buildCacheDataSourceFactory(),
            Executors.newFixedThreadPool(6)
        )
    }
    val notificationHelper by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannel(
            this,
            CHANNEL_ID,
            R.string.channel_name,
            R.string.channel_description,
            NotificationUtil.IMPORTANCE_DEFAULT
        )
    }

    fun buildCacheDataSourceFactory(): CacheDataSource.Factory {
        val upstreamFactory =
            DefaultDataSourceFactory(this, DefaultHttpDataSourceFactory())
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    companion object {

        const val CHANNEL_ID = "channel-id"
        private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
        private const val PREFERENCES_NAME = "SampleApp"
    }
}