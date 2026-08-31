package com.example.chologo.notifications

import com.example.chologo.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM pushes sent by the standalone REST server (see
 * /server/src/index.ts's notify-accepted / notify-match endpoints).
 *
 * Android only auto-displays a notification-payload message for a
 * backgrounded or killed app - a foregrounded app must display it itself,
 * which is what onMessageReceived below does, reusing the same local
 * notification helper/channel already used for the on-device reminders.
 */
class ChologoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        UserRepository().registerFcmTokenForUser(uid)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification ?: return
        val title = notification.title ?: return
        val body = notification.body ?: ""

        ReminderNotifications.showNow(
            context = applicationContext,
            uniqueKey = message.messageId ?: System.currentTimeMillis().toString(),
            title = title,
            message = body
        )
    }
}
