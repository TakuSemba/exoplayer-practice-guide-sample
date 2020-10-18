package com.takusemba.exobook.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.BaseDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.R
import java.io.IOException

class AesSampleActivity : AppCompatActivity() {

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

        val dataSourceFactory = DynamicDataSourceFactory(this)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(URI)

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

    class TestKeyDataSource : BaseDataSource(false) {

        private var dataSpec: DataSpec? = null
        private var encryptionKey: ByteArray? = null
        private var bytesRemaining: Int = 0

        @Throws(IOException::class)
        override fun open(dataSpec: DataSpec): Long {
            transferInitializing(dataSpec)
            val keySource = dataSpec.uri.host // aaabbbcccdddeee
            Log.d(TAG, "got key source: $keySource")
            encryptionKey = ByteArray(16) {
                // create key from keySource
                "49df89bd9c1fe52541d8449491aa723a".substring(it * 2, it * 2 + 2).toInt(16).toByte()
            }
            Log.d(TAG, "created key: ${String(checkNotNull(encryptionKey))}")
            bytesRemaining = encryptionKey?.size ?: 0
            this.dataSpec = dataSpec
            transferStarted(dataSpec)
            return bytesRemaining.toLong()
        }

        @Throws(IOException::class)
        override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
            if (bytesRemaining == 0) return -1
            val key = encryptionKey ?: return -1
            val length = key.size.coerceAtMost(readLength)
            System.arraycopy(key, offset, buffer, 0, length)
            bytesRemaining -= length
            bytesTransferred(length)
            return length
        }

        override fun getUri(): Uri? {
            return dataSpec?.uri
        }

        @Throws(IOException::class)
        override fun close() {
            transferEnded()
            encryptionKey = null
        }

        companion object {

            private const val TAG = "TestKeyDataSource"
        }
    }

    class TestKeyDataSourceFactory : DataSource.Factory {
        override fun createDataSource(): TestKeyDataSource {
            return TestKeyDataSource()
        }
    }


    class DynamicDataSource(
        private val mainDataSource: DataSource,
        private val testKeyDataSource: TestKeyDataSource
    ) : DataSource {

        private var dataSource: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            mainDataSource.addTransferListener(transferListener)
            testKeyDataSource.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val scheme: String? = dataSpec.uri.scheme
            this.dataSource = when (scheme) {
                "test" -> testKeyDataSource
                else -> mainDataSource
            }
            return Assertions.checkNotNull(dataSource).open(dataSpec)
        }

        @Throws(IOException::class)
        override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
            return Assertions.checkNotNull(dataSource).read(buffer, offset, readLength)
        }

        override fun getUri(): Uri? {
            return dataSource?.uri
        }

        @Throws(IOException::class)
        override fun close() {
            try {
                dataSource?.close()
            } finally {
                dataSource = null
            }
        }
    }

    class DynamicDataSourceFactory(context: Context) : DataSource.Factory {
        private val userAgent = Util.getUserAgent(context, "SampleApp")
        private val httpDataSourceFactory = DefaultDataSourceFactory(context, userAgent)
        private val testKeyDataSourceFactory = TestKeyDataSourceFactory()
        override fun createDataSource(): DataSource = DynamicDataSource(
            httpDataSourceFactory.createDataSource(),
            testKeyDataSourceFactory.createDataSource()
        )
    }

    companion object {

        private val URI = Uri.parse("asset:///m3u8/aes_sample.m3u8")
    }
}