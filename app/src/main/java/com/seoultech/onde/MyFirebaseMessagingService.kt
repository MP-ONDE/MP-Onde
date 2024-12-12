import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.seoultech.onde.ChatRoomActivity
import com.seoultech.onde.MainActivity
import com.seoultech.onde.ProfileActivity
import com.seoultech.onde.R

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

        val title = "New Smalltalk"

        val userId = remoteMessage.data["userId"]
        val senderId = remoteMessage.data["senderId"]
        val chatId = remoteMessage.data["chatId"]
        val messageContent = remoteMessage.notification?.body ?: "새로운 메시지가 도착했습니다."

        if (userId == null || senderId == null || chatId == null) {
            Log.w("MyFirebaseMessaging", "필수 데이터 누락, 알림을 표시하지 않습니다.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(senderId)
            .get()
            .addOnSuccessListener { document ->
                val senderNickname = document.getString("nickname") ?: "알 수 없는 사용자"
                val senderUserIdHash = document.getString("userIdHash")
                val body = "$senderNickname: $messageContent"

                // MainActivity용 Intent
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                // ProfileActivity용 Intent
                val profileIntent = Intent(this, ProfileActivity::class.java)
                profileIntent.putExtra("userId", senderUserIdHash)

                // ChatRoomActivity용 Intent
                val chatIntent = Intent(this, ChatRoomActivity::class.java)
                chatIntent.putExtra("userId", senderId)
                chatIntent.putExtra("chatId", chatId)

                val pendingIntent = PendingIntent.getActivities(
                    this,
                    chatId.hashCode(),
                    arrayOf(mainIntent, profileIntent, chatIntent),
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                        return@addOnSuccessListener
                    }
                }

                with(NotificationManagerCompat.from(this)) {
                    notify(chatId.hashCode(), builder.build())
                }
            }
    }

    private fun showNotification(title: String, body: String, pendingIntent: PendingIntent) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun showDefaultNotification(title: String, message: String, senderId: String, chatId: String) {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val chatIntent = Intent(this, ChatRoomActivity::class.java)
        chatIntent.putExtra("userId", senderId)
        chatIntent.putExtra("chatId", chatId)

        val intents = arrayOf(mainIntent, chatIntent)

        val pendingIntent = PendingIntent.getActivities(
            this,
            chatId.hashCode(),
            intents,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(title, message, pendingIntent)
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