package com.seoultech.onde

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class App : Application() {
    companion object {
        private lateinit var instance: App
        fun getInstance(): App = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun checkLoginAndRedirect(): Intent {
        val auth = FirebaseAuth.getInstance()
        return if (auth.currentUser != null) {
            // 로그인된 상태
            Intent(this, MainActivity::class.java)
        } else {
            // 로그인되지 않은 상태
            Intent(this, LoginActivity::class.java)
        }
    }
}