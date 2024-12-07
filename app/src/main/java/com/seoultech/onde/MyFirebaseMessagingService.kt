package com.seoultech.onde

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "onde_notifications"
        private const val CHANNEL_NAME = "ONDE Notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // data 필드 처리
        val userId = remoteMessage.data["userId"] ?: return
        val chatId = remoteMessage.data["chatId"] ?: return

        // 알림 제목과 본문
        val title = remoteMessage.notification?.title ?: "새 메시지"
        val body = remoteMessage.notification?.body ?: "새로운 메시지가 도착했습니다."

        // 알림 클릭 시 ChatRoomActivity로 이동할 인텐트 생성
        val intent = Intent(this, ChatRoomActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("userId", userId)
            putExtra("chatId", chatId)
        }

        // PendingIntent 생성
        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(), // 알림마다 고유 ID를 사용
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌더
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // 알림 매니저를 통해 알림 표시
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(chatId.hashCode(), notificationBuilder.build())
    }

    // 알림 채널 생성
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ONDE 앱 알림 채널"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("새로운 FCM 토큰: $token")

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: "unknown"

        if (userId != null) {
            saveFcmTokenToFirestore(userId, token)
        }
    }

    private fun saveFcmTokenToFirestore(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocument = db.collection("users").document(userId)

        userDocument.set(mapOf("fcmToken" to token))
            .addOnSuccessListener {
                println("새로운 FCM 토큰 저장 성공")
            }
            .addOnFailureListener { e ->
                println("새로운 FCM 토큰 저장 실패: $e")
            }
    }
}
