package com.seoultech.onde

data class User(
    val userIdHash: String,
    val nickname: String,
    val ootd: String,
    val smallTalk: String,
    val rssi: Int
)