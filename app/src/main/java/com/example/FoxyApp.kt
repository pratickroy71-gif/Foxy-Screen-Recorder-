package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.example.data.AppDatabase
import com.example.service.ScreenRecordService

class FoxyApp : Application() {

    companion object {
        lateinit var instance: FoxyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Pre-initialize Room Database
        AppDatabase.getDatabase(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                ScreenRecordService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableLights(true)
                lightColor = Color.MAGENTA
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
