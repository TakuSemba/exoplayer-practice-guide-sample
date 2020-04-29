package com.takusemba.exobook

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.Util
import java.io.File

class App : Application() {

    private val userAgent by lazy { Util.getUserAgent(this, "SampleApp") }
    private val downloadDirectory by lazy { getExternalFilesDir(null) ?: filesDir }
    private val databaseProvider by lazy { ExoDatabaseProvider(this) }
    private val downloadCache by lazy {
        val downloadContentDirectory = File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
        SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), databaseProvider)
    }
    val downloadManager by lazy {
        DownloadManager(this, databaseProvider, downloadCache, buildCacheDataSourceFactory())
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

    fun buildCacheDataSourceFactory(): CacheDataSourceFactory {
        val upstreamFactory =
            DefaultDataSourceFactory(this, DefaultHttpDataSourceFactory(userAgent))
        return CacheDataSourceFactory(
            downloadCache,
            upstreamFactory,
            FileDataSource.Factory(),
            /* cacheWriteDataSinkFactory= */ null,
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            /* eventListener= */ null
        );
    }

    companion object {

        const val CHANNEL_ID = "channel-id"
        private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
        private const val PREFERENCES_NAME = "SampleApp"
    }
}