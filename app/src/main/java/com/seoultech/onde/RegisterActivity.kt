package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonResendEmail: Button

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextEmail = findViewById(R.id.registerEmail)
        editTextPassword = findViewById(R.id.registerPassword)
        buttonRegister = findViewById(R.id.registerButton)
        progressBar = findViewById(R.id.registerProgressBar)
        buttonResendEmail = findViewById(R.id.buttonResendEmail)

        buttonResendEmail.visibility = Button.GONE

        buttonRegister.setOnClickListener {
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

            progressBar.visibility = ProgressBar.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = ProgressBar.GONE
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "인증 메일이 전송되었습니다. 3분 내에 인증을 완료하세요.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Firestore에 사용자 정보 저장 (임시 상태)
                                    saveUserToFirestore(user.uid, false)

                                    // 5분 타이머 설정
                                    startDeleteUserTimer(user)
                                    startLoginActivity()

                                } else {
                                    Toast.makeText(
                                        this,
                                        "이메일 인증 전송 실패: ${emailTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    buttonResendEmail.visibility = Button.VISIBLE
                                }
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "회원가입 실패: ${task.exception?.message}. 로그인 화면으로 돌아갑니다",
                            Toast.LENGTH_LONG
                        ).show()
                        startLoginActivity()
                    }
                }
        }
        buttonResendEmail.setOnClickListener {
            val user = auth.currentUser
            user?.sendEmailVerification()
                ?.addOnCompleteListener { emailTask ->
                    if (emailTask.isSuccessful) {
                        Toast.makeText(
                            this,
                            "이메일이 다시 전송되었습니다. 이메일을 확인하고 인증을 완료하세요.",
                            Toast.LENGTH_LONG
                        ).show()
                        buttonResendEmail.visibility = Button.GONE
                    } else {
                        Toast.makeText(
                            this,
                            "이메일 재전송 실패: ${emailTask.exception?.message}, 로그인 화면으로 돌아갑니다",
                            Toast.LENGTH_LONG
                        ).show()
                        startLoginActivity()
                        finish()
                    }
                }
        }
    }

    private fun startDeleteUserTimer(user: FirebaseUser) {
        Handler(Looper.getMainLooper()).postDelayed({
            // 사용자 인증 여부 확인
            user.reload().addOnCompleteListener {
                if (user.isEmailVerified) {
                    // 인증 완료 시 Firestore 업데이트
                    saveUserVerificationStatus(user.uid, true)
                    Toast.makeText(this, "이메일 인증이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 인증이 완료되지 않으면 사용자 계정 삭제
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(this, "이메일 인증이 완료되지 않아 계정이 삭제되었습니다.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "계정 삭제 실패: ${deleteTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }, 5 * 60 * 1000) // 3분 후 실행
    }

    private fun saveUserToFirestore(userId: String, verified: Boolean) {
        val user = hashMapOf(
            "userId" to userId,
            "username" to null,  // 기본 값
            "profile" to null, // 기본 값
            "verified" to verified  // 이메일 인증 여부 저장
        )
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Firestore에 사용자 정보 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Firestore에 사용자 정보 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserVerificationStatus(userId: String, verified: Boolean) {
        db.collection("users").document(userId).update("verified", verified)
            .addOnSuccessListener {
                Toast.makeText(this, "이메일 인증 상태가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "이메일 인증 상태 업데이트 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // 액티비티 종료 시 코루틴 취소
    }
}




