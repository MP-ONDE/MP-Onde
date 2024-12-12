package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // 스플래시 화면 레이아웃 연결

        // 전체 화면(Immersive Mode) 설정
        hideSystemUI()

        // 스플래시 화면을 2초 동안 보여준 후 자동 로그인 체크
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = (application as App).checkLoginAndRedirect()
            startActivity(intent)
            finish()
        }, 2000) // 2초 대기
    }
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 몰입 모드
                        or View.SYSTEM_UI_FLAG_FULLSCREEN    // 상태바 숨김
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // 네비게이션 바 숨김
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }
}
