package com.seoultech.onde

data class ChatMessage(
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList()
)
