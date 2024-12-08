package com.seoultech.onde

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import java.util.HashSet

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var backButton: ImageButton
    private lateinit var dbHelper: MemoDatabaseHelper
    private val markedDates = HashSet<String>() // 메모가 저장된 날짜 목록

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 뷰 초기화
        calendarView = findViewById(R.id.calendarView)
        backButton = findViewById(R.id.backButton)
        dbHelper = MemoDatabaseHelper(this)

        // 저장된 날짜 불러오기
        loadMarkedDates()

        // 날짜 선택 이벤트 처리
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth"
            // 메모 추가 Dialog 호출
            val dialog = MemoDialogFragment(selectedDate, dbHelper) {
                // 메모가 저장된 날짜를 갱신
                markedDates.add(selectedDate)
                calendarView.invalidate() // 캘린더 갱신
            }
            dialog.show(supportFragmentManager, "MemoDialog")
        }

        // 뒤로가기 버튼 클릭 이벤트 처리
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadMarkedDates() {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT DISTINCT date FROM Memo", null)
        while (cursor.moveToNext()) {
            markedDates.add(cursor.getString(0)) // 저장된 날짜 목록에 추가
        }
        cursor.close()
    }

    override fun onResume() {
        super.onResume()
        calendarView.invalidate()
    }
}