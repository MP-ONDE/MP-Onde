package com.seoultech.onde

data class User(
    val userIdHash: String,
    val userId: String,
    val nickname: String,
    val ootd: String,
    val smallTalk: String,
    val rssi: Int,
    var lastSeenTimestamp: Long // 마지막으로 인식된 시간
)