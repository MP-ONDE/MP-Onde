package com.seoultech.onde

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseUtils {

    fun saveFcmTokenToFirestore(userId: String) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("FCM 토큰 가져오기 실패: ${task.exception}")
                    return@addOnCompleteListener
                }

                // FCM 토큰 가져오기 성공
                val token = task.result
                println("FCM 토큰: $token")

                // Firestore에 토큰 저장
                val db = FirebaseFirestore.getInstance()
                val userDocument = db.collection("users").document(userId)

                userDocument.set(mapOf("fcmToken" to token), SetOptions.merge())
                    .addOnSuccessListener {
                        println("FCM 토큰 저장 성공")
                    }
                    .addOnFailureListener { e ->
                        println("FCM 토큰 저장 실패: $e")
                    }
            }
    }
}
