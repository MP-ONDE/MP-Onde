package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var buttonGoogleLogin: Button  // Google Sign-In button

    private val RC_SIGN_IN = 9001  // Request code for Google Sign-In

    // GoogleSignInHelper instance
    private lateinit var googleSignInHelper: GoogleSignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
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

            // Handle login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            if (user.isEmailVerified) {
                                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                                checkUserAdditionalInfo(user.uid)
                            } else {
                                Toast.makeText(
                                    this,
                                    "이메일 인증이 완료되지 않았습니다. 이메일을 확인해주세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "로그인 실패: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startAdditionalInfoActivity() {
        val intent = Intent(this, AdditionalInfoActivity::class.java)
        startActivity(intent)
        finish()
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
                startMainActivity()
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
                        // 사용자 정보가 이미 있으면 바로 MainActivity로
                        val nickname = document.getString("nickname")
                        if (nickname.isNullOrEmpty()) {
                            // nickname이 없으면 추가 정보 입력 화면으로
                            startAdditionalInfoActivity()
                        } else {
                            startMainActivity()
                        }
                    } else {
                        // Firestore에 정보가 없다면, 추가 정보 입력 화면으로
                        startAdditionalInfoActivity()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사용자 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
