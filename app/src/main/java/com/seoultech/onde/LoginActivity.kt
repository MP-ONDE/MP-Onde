package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var textTitle: TextView
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonToggleLogin: Button

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        textTitle = findViewById(R.id.textTitle)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonToggleLogin = findViewById(R.id.buttonToggleLogin)

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "유효한 이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) {
                // 로그인 처리
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                            startMainActivity()
                        } else {
                            Toast.makeText(
                                this,
                                "로그인 실패: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("LoginActivity", "로그인 실패: ${task.exception?.message}")
                        }
                    }
            } else {
                // 회원가입 처리
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                saveUserToFirestore(userId)
                                startMainActivity()
                            } else {
                                Log.e("LoginActivity", "사용자 ID가 null입니다.")
                                Toast.makeText(this, "사용자 ID를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "회원가입 실패: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("LoginActivity", "회원가입 실패: ${task.exception?.message}")
                        }
                    }
            }
        }

        buttonToggleLogin.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                textTitle.text = "로그인"
                buttonLogin.text = "로그인"
                buttonToggleLogin.text = "회원가입 하기"
            } else {
                textTitle.text = "회원가입"
                buttonLogin.text = "회원가입"
                buttonToggleLogin.text = "로그인 하기"
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveUserToFirestore(userId: String) {
        try {
            val userIdHash = HashUtils.generateUserIdHash(userId)
            Log.d("Firestore", "LoginActivity 저장될 userIdHash: $userIdHash")

            val user = hashMapOf(
                "userId" to userId,
                "userIdHash" to userIdHash,  // 올바르게 할당
                "username" to "기본 사용자명",
                "profile" to "기본 프로필 내용"
            )
            db.collection("users").document(userId).set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "로그인 사용자 등록 성공", Toast.LENGTH_LONG).show()
                    Log.d("Firestore", "LoginActivity 사용자 문서 생성 성공: $userIdHash")
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "로그인 사용자 정보 저장 실패: ${exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("Firestore", "LoginActivity 사용자 정보 저장 실패: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e("LoginActivity", "LoginActivity 사용자 정보 저장 중 오류 발생: ${e.message}")
            Toast.makeText(this, "사용자 정보 저장 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
