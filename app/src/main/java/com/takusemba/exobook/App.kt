package com.takusemba.exobook

import android.app.Application
import com.google.android.exoplayer2.util.NotificationUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannel(
            this,
            "channel-id",
            R.string.channel_name,
            R.string.channel_description,
            NotificationUtil.IMPORTANCE_DEFAULT
        )
    }
}