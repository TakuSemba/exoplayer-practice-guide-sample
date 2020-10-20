package com.takusemba.exobook.extension

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cronet.CronetDataSourceFactory
import com.google.android.exoplayer2.ext.cronet.CronetEngineWrapper
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.takusemba.exobook.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.chromium.net.CronetEngine
import java.io.File
import java.util.concurrent.Executors

class NetworkSampleActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
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
        val player = SimpleExoPlayer.Builder(this).build()
        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player

        val dataSourceFactory = when (TYPE) {
            DataSourceFactoryType.OK_HTTP -> getOkHttpDataSourceFactory()
            DataSourceFactoryType.CRONET -> getCronetDataSourceFactory()
            DataSourceFactoryType.DEFAULT -> getDefaultHttpDataSourceFactory()
        }
        val mediaItem = MediaItem.fromUri(URI)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true

        this.player = player
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun getDefaultHttpDataSourceFactory(): DataSource.Factory {
        return DefaultHttpDataSourceFactory()
    }

    private fun getOkHttpDataSourceFactory(): DataSource.Factory {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        return OkHttpDataSourceFactory(okHttpClient)
    }

    private fun getCronetDataSourceFactory(): DataSource.Factory {
        val cronetEngine = CronetEngine.Builder(this)
            .enableQuic(true)
            .build()
        val cronetWrapper = CronetEngineWrapper(cronetEngine)
        val executor = Executors.newSingleThreadExecutor()
        return CronetDataSourceFactory(cronetWrapper, executor)
    }

    private fun CronetEngine.startNetLogForCronet() {
        val outputFile = File.createTempFile("cronet", ".json", cacheDir)
        startNetLogToFile(outputFile.toString(), true)
    }

    private fun CronetEngine.stopNetLogForCronet() {
        stopNetLog()
    }

    companion object {

        private val URI =
            Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")

        private val TYPE = DataSourceFactoryType.OK_HTTP

        enum class DataSourceFactoryType { DEFAULT, OK_HTTP, CRONET }
    }
}