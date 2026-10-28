package com.example.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object UpdateNotificationHelper {

    const val CHANNEL_ID = "ultra_player_updates_channel"
    const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_UPDATE_DIALOG = "extra_open_update_dialog"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates & Releases"
            val descriptionText = "Notifications for new Ultra Video Player Pro versions and features"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(context: Context, updateInfo: UpdateInfo) {
        // Ensure channel exists
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("🚀 Ultra Video Player Pro ${updateInfo.latestVersionName} Available!")
            .setContentText("Tap to update: Auto-rotation, Quick Play ▶️, and v${updateInfo.latestVersionName} features.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Ultra Video Player Pro ${updateInfo.latestVersionName} is now ready!\n\n" +
                    updateInfo.releaseNotes.ifBlank {
                        "• 🔄 Smart Video-Adaptive Rotation\n" +
                        "• ▶️ Circular Quick-Play Action\n" +
                        "• ⚡ Seamless Auto-Installer\n" +
                        "• 🎛️ 10-Option Video Action Sheet"
                    }
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
