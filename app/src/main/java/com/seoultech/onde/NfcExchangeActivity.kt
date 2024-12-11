package com.seoultech.onde

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NfcExchangeActivity : AppCompatActivity() {

    private lateinit var instagramNfcUtils: InstagramNfcUtils
    private lateinit var statusTextView: TextView
    private lateinit var nfcImageView: ImageView
    private lateinit var enableNfcButton: Button
    private lateinit var backButton: ImageButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_exchange)

        // 뷰 초기화
        statusTextView = findViewById(R.id.statusTextView)
        nfcImageView = findViewById(R.id.nfcImageView)
        enableNfcButton = findViewById(R.id.enableNfcButton)
        backButton = findViewById(R.id.backButton)

        // NFC 유틸리티 초기화
        instagramNfcUtils = InstagramNfcUtils(this)

        // 현재 사용자의 인스타그램 정보 가져오기
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    val sns = document.getString("sns") ?: ""
                    instagramNfcUtils.setInstagramUsername(sns)
                    statusTextView.text = "내 인스타그램: $sns"
                }
        }

        // NFC 콜백 설정
        instagramNfcUtils.setNfcCallback { receivedUsername ->
            showInstagramDialog(receivedUsername)
        }

        // 버튼 리스너 설정
        enableNfcButton.setOnClickListener {
            instagramNfcUtils.enableNfcForegroundDispatch()
            statusTextView.text = "NFC 교환 준비됨"
            Toast.makeText(this, "기기를 맞대어 인스타그램 주소를 교환하세요", Toast.LENGTH_LONG).show()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showInstagramDialog(username: String) {
        val instagramUrl = if (username.startsWith("http")) username else "https://instagram.com/$username"
        AlertDialog.Builder(this)
            .setTitle("인스타그램 프로필")
            .setMessage("받은 인스타그램 주소: $username")
            .setPositiveButton("프로필 열기") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(instagramUrl))
                startActivity(intent)
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        instagramNfcUtils.enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        instagramNfcUtils.disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        instagramNfcUtils.handleIntent(intent)
    }
}