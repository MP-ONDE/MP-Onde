package com.seoultech.onde

import android.util.Base64
import java.security.MessageDigest

object HashUtils {
    // 사용자 ID 해시 함수 (8바이트 사용)
    fun generateUserIdHash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray(Charsets.UTF_8))
        val shortHash = hash.copyOf(8) // 앞의 8바이트 사용
        return Base64.encodeToString(shortHash, Base64.NO_WRAP)
    }
}
