package com.takusemba.exobook.extension

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media2.session.MediaSession
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.media2.MediaSessionUtil
import com.google.android.exoplayer2.ext.media2.SessionCallbackBuilder
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.takusemba.exobook.App.Companion.CHANNEL_ID_MEDIA_SESSION
import java.util.ArrayList
import java.util.concurrent.Executors
import androidx.media2.common.MediaMetadata as Media2MediaMetadata

class Media2SampleService : MediaBrowserServiceCompat() {

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
                    val metadata = mediaSession.player.currentMediaItem?.metadata
                    return metadata?.getString(Media2MediaMetadata.METADATA_KEY_TITLE) ?: "no title"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    val metadata = mediaSession.player.currentMediaItem?.metadata
                    return metadata?.getBitmap(Media2MediaMetadata.METADATA_KEY_ART)
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
    private val sessionPlayerConnector by lazy {
        SessionPlayerConnector(player).apply {
            setControlDispatcher(DefaultControlDispatcher())
        }
    }
    private val sessionCallback by lazy {
        SessionCallbackBuilder(this, sessionPlayerConnector).build()
    }
    private val mediaSession by lazy {
        MediaSession.Builder(this, sessionPlayerConnector)
            .setSessionCallback(Executors.newSingleThreadExecutor(), sessionCallback)
            .build()
    }

    data class Song(
        val id: String,
        val title: String,
        val album: String,
        val artist: String,
        val genre: String,
        val mediaUrl: String,
        val iconUrl: String,
        val duration: Long
    )

    override fun onCreate() {
        super.onCreate()

        val sessionToken = MediaSessionUtil.getSessionCompatToken(mediaSession)

        notificationManager.setMediaSessionToken(sessionToken)
        notificationManager.setPlayer(player)

        setSessionToken(sessionToken)

        val mediaItems = SONGS.map { song ->
            MediaItem.Builder()
                .setUri(song.mediaUrl)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).build())
                .build()
        }
        player.setAudioAttributes(AudioAttributes.DEFAULT, true)
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.close()
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