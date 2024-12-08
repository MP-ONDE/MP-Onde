package com.seoultech.onde

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MemoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "MemoDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // 메모 테이블 생성
        db.execSQL("CREATE TABLE Memo (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, memo TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Memo")
        onCreate(db)
    }

    // 메모 저장
    fun saveMemo(date: String, memo: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("memo", memo)
        }
        db.insert("Memo", null, values)
    }

    // 특정 날짜의 메모 가져오기
    fun getMemo(date: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            "Memo", arrayOf("memo"), "date=?", arrayOf(date),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow("memo"))
        } else {
            null
        }.also { cursor.close() }
    }
}
