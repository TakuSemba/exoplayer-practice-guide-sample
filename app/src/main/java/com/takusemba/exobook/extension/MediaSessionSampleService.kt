package com.takusemba.exobook.extension

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_GENRE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.takusemba.exobook.App.Companion.CHANNEL_ID
import com.takusemba.exobook.R
import java.util.*

class MediaSessionSampleService : MediaBrowserServiceCompat() {

    private val notificationManager by lazy {
        PlayerNotificationManager.createWithNotificationChannel(
            this,
            CHANNEL_ID,
            R.string.channel_name,
            R.string.channel_description,
            NOTIFICATION_ID,
            object :
                PlayerNotificationManager.MediaDescriptionAdapter {

                override fun createCurrentContentIntent(player: Player): PendingIntent? = null

                override fun getCurrentContentText(player: Player): CharSequence? = null

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    val controller: MediaControllerCompat = mediaSession.controller
                    return controller.metadata.description.title ?: "no title"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    val controller: MediaControllerCompat = mediaSession.controller
                    return controller.metadata.description.iconBitmap
                }
            },
            object : PlayerNotificationManager.NotificationListener {

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(false)
                    }
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            }
        )
    }

    private val mediaSession by lazy { MediaSessionCompat(application, TAG) }
    private val userAgent by lazy { Util.getUserAgent(application, "SampleApp") }
    private val dataSourceFactory by lazy { DefaultDataSourceFactory(application, userAgent) }
    private val player by lazy { SimpleExoPlayer.Builder(this).build() }
    private val mediaSessionConnector by lazy { MediaSessionConnector(mediaSession) }

    private var currentIndex = 0

    data class Song(
        val id: String,
        val title: String,
        val album: String,
        val artist: String,
        val genre: String,
        val source: String,
        val image: String,
        val duration: Long
    )

    override fun onCreate() {
        super.onCreate()

        val queueNavigator = object : TimelineQueueNavigator(mediaSession) {

            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                return SONGS[currentIndex].toMediaMetadata().description
            }
        }

        val playbackPreparer = object : MediaSessionConnector.PlaybackPreparer {

            override fun getSupportedPrepareActions(): Long {
                return MediaSessionConnector.PlaybackPreparer.ACTIONS
            }

            override fun onPrepareFromMediaId(
                mediaId: String,
                playWhenReady: Boolean,
                extras: Bundle
            ) {
                val song = SONGS.find { it.id == mediaId } ?: return
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(song.source))
                player.prepare(mediaSource)
                player.playWhenReady = playWhenReady
                mediaSession.setMetadata(song.toMediaMetadata())
                currentIndex = SONGS.indexOf(song)
            }

            override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle) {
                val song = SONGS.find { it.source == uri.toString() } ?: return
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri)
                player.prepare(mediaSource)
                player.playWhenReady = playWhenReady
                mediaSession.setMetadata(song.toMediaMetadata())
                currentIndex = SONGS.indexOf(song)
            }

            override fun onPrepareFromSearch(
                query: String,
                playWhenReady: Boolean,
                extras: Bundle
            ) {
                val song = SONGS.find { it.title == query } ?: return
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(song.source))
                player.prepare(mediaSource)
                player.playWhenReady = playWhenReady
                mediaSession.setMetadata(song.toMediaMetadata())
                currentIndex = SONGS.indexOf(song)
            }

            override fun onCommand(
                player: Player,
                controlDispatcher: ControlDispatcher,
                command: String,
                extras: Bundle?,
                cb: ResultReceiver?
            ): Boolean {
                return false
            }

            override fun onPrepare(playWhenReady: Boolean) {
                player.playWhenReady = playWhenReady
            }

        }
        mediaSession.isActive = true

        mediaSessionConnector.setQueueNavigator(queueNavigator)
        mediaSessionConnector.setPlaybackPreparer(playbackPreparer)
        mediaSessionConnector.setPlayer(player)

        notificationManager.setMediaSessionToken(mediaSession.sessionToken)
        notificationManager.setPlayer(player)

        sessionToken = mediaSession.sessionToken

        val concatenatingMediaSource = ConcatenatingMediaSource()
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        SONGS.forEach { song ->
            val mediaSource = mediaSourceFactory.createMediaSource(Uri.parse(song.source))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        player.prepare(concatenatingMediaSource)
        player.playWhenReady = true
        player.addListener(object : Player.EventListener {
            override fun onPositionDiscontinuity(reason: Int) {
                currentIndex = player.currentWindowIndex
            }
        })
        mediaSession.setMetadata(SONGS[currentIndex].toMediaMetadata())
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.isActive = false
        mediaSessionConnector.setPlayer(null)
        notificationManager.setPlayer(null)
        player.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == ROOT_ID) {
            result.sendResult(MEDIA_ITEMS.toMutableList())
        } else {
            result.sendResult(ArrayList())
        }
    }

    companion object {

        private const val TAG = "MediaSessionSample"
        private const val ROOT_ID = "media-session-sample"
        private const val NOTIFICATION_ID = 0

        private val SONGS = listOf(
            Song(
                id = "irsens_tale_01",
                title = "Intro (.udonthear)",
                album = "Irsen's Tale",
                artist = "Kai Engel",
                genre = "Ambient",
                source = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/01_-_Intro_udonthear.mp3",
                image = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/art.jpg",
                duration = 63000L
            ),
            Song(
                id = "irsens_tale_02",
                title = "Leaving",
                album = "Irsen's Tale",
                artist = "Kai Engel",
                genre = "Ambient",
                source = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/02_-_Leaving.mp3",
                image = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/art.jpg",
                duration = 170000L
            ),
            Song(
                id = "irsens_tale_03",
                title = "Irsen's Tale",
                album = "Irsen's Tale",
                artist = "Kai Engel",
                genre = "Ambient",
                source = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/03_-_Irsens_Tale.mp3",
                image = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/art.jpg",
                duration = 164000L
            )
        )

        private val MEDIA_METADATA = SONGS.map { song -> song.toMediaMetadata() }

        private val MEDIA_ITEMS = MEDIA_METADATA.map { metadata -> metadata.toMediaItem() }

        private fun Song.toMediaMetadata(): MediaMetadataCompat {
            return MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_MEDIA_ID, id)
                .putString(METADATA_KEY_ALBUM, album)
                .putString(METADATA_KEY_ARTIST, artist)
                .putString(METADATA_KEY_GENRE, genre)
                .putString(METADATA_KEY_ALBUM_ART_URI, image)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, image)
                .putString(METADATA_KEY_TITLE, title)
                .putLong(METADATA_KEY_DURATION, duration)
                .build()
        }

        private fun MediaMetadataCompat.toMediaItem(): MediaBrowserCompat.MediaItem {
            return MediaBrowserCompat.MediaItem(
                description,
                FLAG_PLAYABLE or FLAG_BROWSABLE
            )
        }
    }
}