package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var textTitle: TextView
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonToggleLogin: Button
    private lateinit var buttonGoogleLogin: Button  // Google Sign-In button

    private var isLoginMode = true
    private val RC_SIGN_IN = 9001  // Request code for Google Sign-In

    // GoogleSignInHelper instance
    private lateinit var googleSignInHelper: GoogleSignInHelper

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
        buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin)  // Reference Google Sign-In button

        // Initialize GoogleSignInHelper
        googleSignInHelper = GoogleSignInHelper(this)

        buttonGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

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
                // Handle login
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                            checkUserAdditionalInfo(auth.currentUser?.uid)
                        } else {
                            Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                // Handle registration
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                saveUserToFirestore(userId)
                                startAdditionalInfoActivity()
                            } else {
                                Toast.makeText(this, "사용자 ID를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        buttonToggleLogin.setOnClickListener {
            isLoginMode = !isLoginMode
            textTitle.text = if (isLoginMode) "로그인" else "회원가입"
            buttonLogin.text = if (isLoginMode) "로그인" else "회원가입"
            buttonToggleLogin.text = if (isLoginMode) "회원가입 하기" else "로그인 하기"
        }
    }

    private fun startDailyInformationActivity() {
        val intent = Intent(this, DailyInformationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startAdditionalInfoActivity() {
        val intent = Intent(this, AdditionalInfoActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveUserToFirestore(userId: String) {
        val user = hashMapOf(
            "userId" to userId,
            "username" to "기본 사용자명",  // 기본 값
            "profile" to "기본 프로필 내용" // 기본 값
        )
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Firestore에 사용자 정보 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Firestore에 사용자 정보 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInHelper.signInWithGoogle()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = googleSignInHelper.getSignedInAccountFromIntent(data)
            googleSignInHelper.handleSignInResult(task, { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                // 로그인 후 DailyInformationActivity로 이동
                startDailyInformationActivity()
            }, { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            })
        }
    }

    // Firestore에서 추가 정보를 확인하여, 없으면 추가 정보 입력 화면으로 이동
    private fun checkUserAdditionalInfo(userId: String?) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // 사용자 정보가 이미 있으면 DailyInformationActivity로 이동
                        startDailyInformationActivity()
                    } else {
                        // Firestore에 정보가 없다면, 추가 정보 입력 화면으로 이동
                        startAdditionalInfoActivity()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사용자 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
