package com.jworks.kanjiquest.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jworks.kanjiquest.android.MainActivity
import com.jworks.kanjiquest.android.R
import com.jworks.kanjiquest.core.domain.repository.FeedbackRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FeedbackFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var feedbackRepository: FeedbackRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "feedback_updates"
        private const val CHANNEL_NAME = "Feedback Updates"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Register new FCM token with backend
        serviceScope.launch {
            // Get user email from shared preferences or UserSessionProvider
            val prefs = getSharedPreferences("kanjiquest_prefs", Context.MODE_PRIVATE)
            val userEmail = prefs.getString("user_email", null) ?: return@launch

            feedbackRepository.registerFcmToken(
                email = userEmail,
                appId = "kanjiquests",
                fcmToken = token,
                deviceInfo = mapOf(
                    "os" to "Android",
                    "osVersion" to Build.VERSION.RELEASE,
                    "device" to Build.MODEL
                )
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Create notification channel
        createNotificationChannel()

        // Extract data
        val feedbackId = message.data["feedback_id"] ?: "unknown"
        val newStatus = message.data["new_status"] ?: "updated"
        val completionNote = message.data["completion_note"]

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Need to add this icon
            .setContentTitle("Feedback Update")
            .setContentText("Your feedback is now: $newStatus")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (completionNote != null) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your feedback is now: $newStatus\n\nNote: $completionNote")
            )
        }

        // Create intent to open app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_feedback", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(pendingIntent)

        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for feedback status updates"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
