package com.seoultech.onde

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseMessaging", "새로운 FCM 토큰: $token")

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        // 유저가 로그인 상태일 경우에만 토큰 저장
        if (userId != null) {
            saveFcmTokenToFirestore(userId, token)
        } else {
            Log.w("MyFirebaseMessaging", "유저가 로그인되지 않아 토큰을 저장할 수 없습니다.")
        }
    }

    private fun saveFcmTokenToFirestore(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocument = db.collection("users").document(userId)

        userDocument.update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("MyFirebaseMessaging", "FCM 토큰 저장 성공")
            }
            .addOnFailureListener { e ->
                Log.e("MyFirebaseMessaging", "FCM 토큰 저장 실패: $e")
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // notification 필드가 있을 경우 제목/내용을 추출
        val title = remoteMessage.notification?.title ?: "새 메시지"
        val body = remoteMessage.notification?.body ?: "새로운 메시지가 도착했습니다."

        // data 필드 처리
        val userId = remoteMessage.data["userId"]
        val senderId = remoteMessage.data["senderId"]
        val chatId = remoteMessage.data["chatId"]

        // 필요한 데이터가 없으면 알림 표시를 안함
        if (userId == null || senderId == null || chatId == null) {
            Log.w("MyFirebaseMessaging", "필수 데이터 누락, 알림을 표시하지 않습니다.")
            return
        }

        // 알림 클릭 시 ChatRoomActivity로 이동
        val intent = Intent(this, ChatRoomActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("userId", senderId)
            putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 실제 프로젝트 아이콘 리소스로 변경 필요
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Android 13 이상에서 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w("MyFirebaseMessaging", "POST_NOTIFICATIONS 권한이 없어 알림을 표시하지 못합니다.")
                return
            }
        }

        with(NotificationManagerCompat.from(this)) {
            notify(chatId.hashCode(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        // 오레오 이상에서 필요
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
}
