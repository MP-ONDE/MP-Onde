package com.seoultech.onde

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextUsername: EditText
    private lateinit var editTextProfile: EditText
    private lateinit var buttonSave: Button
    private lateinit var progressBar: ProgressBar

    private var userId: String? = null
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextUsername = findViewById(R.id.editTextUsername)
        editTextProfile = findViewById(R.id.editTextProfile)
        buttonSave = findViewById(R.id.buttonSave)
        progressBar = findViewById(R.id.progressBar)

        userId = auth.currentUser?.uid

        if (userId != null) {
            isLoading = true
            showLoading(true)
            db.collection("users").document(userId!!).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        editTextUsername.setText(document.getString("username") ?: "")
                        editTextProfile.setText(document.getString("profile") ?: "")
                    }
                    isLoading = false
                    showLoading(false)
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "사용자 정보 불러오기 실패: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                    showLoading(false)
                }
        }

        buttonSave.setOnClickListener {
            val username = editTextUsername.text.toString()
            val profile = editTextProfile.text.toString()

            if (userId != null) {
                val userUpdates = mapOf(
                    "username" to username,
                    "profile" to profile
                )
                db.collection("users").document(userId!!).set(userUpdates, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "프로필 업데이트 성공",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "프로필 업데이트 실패: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } else {
                Toast.makeText(this, "사용자 인증 정보가 없습니다.", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = ProgressBar.VISIBLE
            buttonSave.isEnabled = false
        } else {
            progressBar.visibility = ProgressBar.GONE
            buttonSave.isEnabled = true
        }
    }
}
