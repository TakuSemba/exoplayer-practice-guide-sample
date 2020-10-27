package com.takusemba.exobook.extension

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import androidx.media2.common.MediaMetadata
import androidx.media2.session.MediaSession
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.media2.MediaSessionUtil
import com.google.android.exoplayer2.ext.media2.SessionCallbackBuilder
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.takusemba.exobook.App.Companion.CHANNEL_ID_MEDIA_SESSION
import java.util.ArrayList
import java.util.concurrent.Executors
import  com.google.android.exoplayer2.MediaMetadata as ExoMediaMetadata

class MediaSessionSampleService : MediaBrowserServiceCompat() {

    private val notificationManager by lazy {
        PlayerNotificationManager(
            this,
            CHANNEL_ID_MEDIA_SESSION,
            NOTIFICATION_ID,
            object :
                PlayerNotificationManager.MediaDescriptionAdapter {

                override fun createCurrentContentIntent(player: Player): PendingIntent? = null

                override fun getCurrentContentText(player: Player): CharSequence? = null

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return mediaSessionManager.getTitle()
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    return mediaSessionManager.getLargeIcon()
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

    private val player by lazy { SimpleExoPlayer.Builder(this).build() }
    private val mediaSessionManager by lazy { Media2MediaSessionManager(this) }

    data class Song(
        val id: String,
        val title: String,
        val album: String,
        val artist: String,
        val genre: String,
        val mediaUrl: String,
        val iconUrl: String,
        val duration: Long,
    )

    override fun onCreate() {
        super.onCreate()

        notificationManager.setPlayer(player)
        mediaSessionManager.setPlayer(player)

        val mediaSessionToken = mediaSessionManager.getMediaSessionToken()
        if (mediaSessionToken != null) {
            notificationManager.setMediaSessionToken(mediaSessionToken)
            sessionToken = mediaSessionToken
        }

        val mediaItems = SONGS.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.mediaUrl)
                .setMediaMetadata(ExoMediaMetadata.Builder().setTitle(song.title).build())
                .build()
        }
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
//        Handler().postDelayed({ player.play() }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.setPlayer(null)
        mediaSessionManager.release()
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

    private interface MediaSessionManager {

        fun getMediaSessionToken(): MediaSessionCompat.Token?

        fun getTitle(): CharSequence

        fun getLargeIcon(): Bitmap?

        fun setPlayer(player: Player)

        fun release()
    }

    class Media2MediaSessionManager(private val context: Context) : MediaSessionManager {

        private var mediaSession: MediaSession? = null

        override fun getMediaSessionToken(): MediaSessionCompat.Token? {
            val currentMediaSession = mediaSession ?: return null
            return MediaSessionUtil.getSessionCompatToken(currentMediaSession)
        }

        override fun getTitle(): CharSequence {
            val metadata = mediaSession?.player?.currentMediaItem?.metadata
            return metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "no title"
        }

        override fun getLargeIcon(): Bitmap? {
            val metadata = mediaSession?.player?.currentMediaItem?.metadata
            return metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        }

        override fun setPlayer(player: Player) {
            val sessionPlayerConnector = SessionPlayerConnector(player)
            val sessionCallback = SessionCallbackBuilder(context, sessionPlayerConnector).build()
            mediaSession = MediaSession.Builder(context, sessionPlayerConnector)
                .setSessionCallback(Executors.newSingleThreadExecutor(), sessionCallback)
                .build()
        }

        override fun release() {
            mediaSession?.close()
            mediaSession = null
        }

    }

    class LegacyMediaSessionManager(context: Context) : MediaSessionManager {

        private val mediaSession by lazy { MediaSessionCompat(context, TAG) }
        private val mediaSessionConnector by lazy { MediaSessionConnector(mediaSession) }

        override fun getMediaSessionToken(): MediaSessionCompat.Token? {
            return mediaSession.sessionToken
        }

        override fun getTitle(): CharSequence {
            val controller: MediaControllerCompat = mediaSession.controller
            return controller.metadata.description.title ?: "no title"
        }

        override fun getLargeIcon(): Bitmap? {
            val controller: MediaControllerCompat = mediaSession.controller
            return controller.metadata.description.iconBitmap
        }

        override fun setPlayer(player: Player) {
            val queueNavigator = object : TimelineQueueNavigator(mediaSession) {

                override fun getMediaDescription(
                    player: Player,
                    windowIndex: Int
                ): MediaDescriptionCompat {
                    return SONGS[windowIndex].toMediaMetadata().description
                }
            }
            val playbackPreparer = object : MediaSessionConnector.PlaybackPreparer {

                override fun getSupportedPrepareActions(): Long {
                    return MediaSessionConnector.PlaybackPreparer.ACTIONS
                }

                override fun onPrepareFromMediaId(
                    mediaId: String,
                    playWhenReady: Boolean,
                    extras: Bundle?
                ) {
                    val song = SONGS.find { it.id == mediaId } ?: return
                    val mediaItem = MediaItem.fromUri(Uri.parse(song.mediaUrl))
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = playWhenReady
                    mediaSession.setMetadata(song.toMediaMetadata())
                }

                override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
                    val song = SONGS.find { it.mediaUrl == uri.toString() } ?: return
                    onPrepareFromMediaId(song.id, playWhenReady, extras)
                }

                override fun onPrepareFromSearch(
                    query: String,
                    playWhenReady: Boolean,
                    extras: Bundle?
                ) {
                    val song = SONGS.find { it.title == query } ?: return
                    onPrepareFromMediaId(song.id, playWhenReady, extras)
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
            mediaSessionConnector.setQueueNavigator(queueNavigator)
            mediaSessionConnector.setPlaybackPreparer(playbackPreparer)
            mediaSessionConnector.setPlayer(player)

            mediaSession.setMetadata(SONGS[0].toMediaMetadata())
            mediaSession.isActive = true

        }

        override fun release() {
            mediaSession.isActive = false
            mediaSessionConnector.setPlayer(null)
        }

        companion object {

            private const val TAG = "MediaSessionSample"

            private fun Song.toMediaMetadata(): MediaMetadataCompat {
                return MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_MEDIA_ID, id)
                    .putString(METADATA_KEY_ALBUM, album)
                    .putString(METADATA_KEY_ARTIST, artist)
                    .putString(METADATA_KEY_GENRE, genre)
                    .putString(METADATA_KEY_ALBUM_ART_URI, iconUrl)
                    .putString(METADATA_KEY_DISPLAY_ICON_URI, iconUrl)
                    .putString(METADATA_KEY_TITLE, title)
                    .putLong(METADATA_KEY_DURATION, duration)
                    .build()
            }
        }

    }

    companion object {

        private const val ROOT_ID = "media-session-sample"
        private const val NOTIFICATION_ID = 200

        private val SONGS = listOf(
            Song(
                id = "irsens_tale_01",
                title = "Intro (.udonthear)",
                album = "Irsen's Tale",
                artist = "Kai Engel",
                genre = "Ambient",
                mediaUrl = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/01_-_Intro_udonthear.mp3",
                iconUrl = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/art.jpg",
                duration = 63000L
            ),
            Song(
                id = "irsens_tale_02",
                title = "Leaving",
                album = "Irsen's Tale",
                artist = "Kai Engel",
                genre = "Ambient",
                mediaUrl = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/02_-_Leaving.mp3",
                iconUrl = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/art.jpg",
                duration = 170000L
            ),
            Song(
                id = "irsens_tale_03",
                title = "Irsen's Tale",
                album = "Irsen's Tale",
                artist = "Kai Engel",
                genre = "Ambient",
                mediaUrl = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/03_-_Irsens_Tale.mp3",
                iconUrl = "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/art.jpg",
                duration = 164000L
            )
        )

        private val MEDIA_ITEMS = SONGS.map { song -> song.toMediaItem() }

        private fun Song.toMediaItem(): MediaBrowserCompat.MediaItem {
            return MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(id)
                    .setTitle(title)
                    .setMediaUri(Uri.parse(mediaUrl))
                    .setIconUri(Uri.parse(iconUrl))
                    .build(),
                FLAG_PLAYABLE or FLAG_BROWSABLE
            )
        }
    }
}