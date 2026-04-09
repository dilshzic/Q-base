package com.algorithmx.q_base.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.algorithmx.q_base.MainActivity
import com.algorithmx.q_base.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_MESSAGES = "chat_messages"
        const val CHANNEL_SESSIONS = "session_invites"
        const val NOTIFICATION_ID_BASE = 1000
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val chatChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
            }

            val sessionChannel = NotificationChannel(
                CHANNEL_SESSIONS,
                "Session Invites",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for study session invites"
            }

            manager.createNotificationChannel(chatChannel)
            manager.createNotificationChannel(sessionChannel)
        }
    }

    fun showMessageNotification(chatId: String, senderName: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CHAT_ID", chatId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            chatId.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        with(NotificationManagerCompat.from(context)) {
            notify(chatId.hashCode(), builder.build())
        }
    }

    fun showSessionNotification(sessionId: String, title: String, description: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SESSION_ID", sessionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SESSIONS)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        with(NotificationManagerCompat.from(context)) {
            notify(sessionId.hashCode(), builder.build())
        }
    }
}
