package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var nicknameTextView: TextView
    private lateinit var smallTalkTextView: TextView
    private lateinit var ootdTextView: TextView
    private lateinit var ageTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var interestTextView: TextView
    private lateinit var profilePhoto : ImageView
    private lateinit var openChatRoomButton : Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        nicknameTextView = findViewById(R.id.nicknameTextView)
        smallTalkTextView = findViewById(R.id.smallTalkTextView)
        ootdTextView = findViewById(R.id.ootdTextView)
        ageTextView = findViewById(R.id.ageTextView)
        genderTextView = findViewById(R.id.genderTextView)
        interestTextView = findViewById(R.id.interestTextView)
        profilePhoto = findViewById(R.id.profilePhoto)
        openChatRoomButton = findViewById(R.id.openChatRoomButton)

        val userIdHash = intent.getStringExtra("userIdHash")

        if (userIdHash != null) {
            fetchUserProfile(userIdHash)
        } else {
            Log.e("ProfileActivity", "userIdHash가 전달되지 않았습니다.")
            finish()
        }
        //임의의 채팅방을 나타내기 위해 실험하는 테스트 코드

        openChatRoomButton.setOnClickListener {
            val intent = Intent(this, ChatRoomActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchUserProfile(userIdHash: String) {
        db.collection("users")
            .whereEqualTo("userIdHash", userIdHash)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val nickname = document.getString("nickname") ?: "Unknown"
                    val smallTalk = document.getString("smallTalk") ?: ""
                    val age = document.getString("age") ?: ""
                    val gender = document.getString("gender") ?: ""
                    val interest = document.getString("interest") ?: ""
                    val ootd = document.getString("ootd") ?: ""
//                    val userId = document.getString("userId") ?: ""
                    val photoUrl = document.getString("photoUrl")
                    if (photoUrl != null) {
                        Picasso.get()
                            .load(photoUrl)
                            .into(profilePhoto)
                    }


                    nicknameTextView.text = nickname
                    smallTalkTextView.text = smallTalk
                    ootdTextView.text = ootd
                    ageTextView.text = age
                    genderTextView.text = gender
                    interestTextView.text = interest

                    // 채팅방 열기 버튼 클릭 리스너 설정
                    openChatRoomButton.setOnClickListener {
                        val intent = Intent(this, ChatRoomActivity::class.java)
                        intent.putExtra("userIdHash", userIdHash)
                        startActivity(intent)
                    }

                } else {
                    Log.e("ProfileActivity", "사용자 정보를 찾을 수 없습니다: $userIdHash")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "사용자 정보 조회 실패: ${exception.message}")
                finish()
            }
    }
}