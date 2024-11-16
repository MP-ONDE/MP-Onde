package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdditionalInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextNickname: EditText
    private lateinit var editTextGender: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextInterests: EditText
    private lateinit var editTextSns: EditText
    private lateinit var editTextOotd: EditText
    private lateinit var editTextSmallTalk: EditText
    private lateinit var buttonSaveInfo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional_info)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Find the views
        editTextNickname = findViewById(R.id.editTextNickname)
        editTextGender = findViewById(R.id.editTextGender)
        editTextAge = findViewById(R.id.editTextAge)
        editTextInterests = findViewById(R.id.editTextInterests)
        editTextSns = findViewById(R.id.editTextSns)
        editTextOotd = findViewById(R.id.editTextOotd)
        editTextSmallTalk = findViewById(R.id.editTextSmallTalk)
        buttonSaveInfo = findViewById(R.id.buttonSaveInfo)

        // Button click listener for saving user info
        buttonSaveInfo.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Create the userInfo map with Any type (for Firestore)
                val userInfo = hashMapOf<String, Any>(
                    "nickname" to editTextNickname.text.toString(),
                    "gender" to editTextGender.text.toString(),
                    "age" to editTextAge.text.toString(),
                    "interests" to editTextInterests.text.toString(),
                    "sns" to editTextSns.text.toString(),
                    "ootd" to editTextOotd.text.toString(),
                    "smallTalk" to editTextSmallTalk.text.toString()
                )
                // Save the information to Firestore
                saveUserInfoToFirestore(userId, userInfo)
            } else {
                Toast.makeText(this, "사용자 ID를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function to save user info to Firestore
    private fun saveUserInfoToFirestore(userId: String, userInfo: HashMap<String, Any>) {
        db.collection("users").document(userId).update(userInfo)
            .addOnSuccessListener {
                Toast.makeText(this, "정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                startMainActivity()
            }
            .addOnFailureListener {
                Toast.makeText(this, "정보 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to start MainActivity
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
