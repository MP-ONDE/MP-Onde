package com.seoultech.onde

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatRoomActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    private lateinit var topicTextView: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var chatId: String
    private lateinit var currentUserId: String
    private lateinit var otherUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        auth = FirebaseAuth.getInstance()

        currentUserId = auth.currentUser?.uid ?: ""
        otherUserId = intent.getStringExtra("userId") ?: ""


        if (currentUserId.isEmpty() || otherUserId.isEmpty()) {
            finish() // 필수 데이터 누락 시 종료
            return
        }

        topicTextView = findViewById(R.id.topicTextView)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        chatAdapter = ChatAdapter(messages, currentUserId)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        chatId = createChatRoomId(currentUserId, otherUserId)

        createOrGetChatRoom()
        listenForMessages()
        markAllMessagesAsRead()

        sendButton.setOnClickListener { sendMessage(messageEditText.text.toString()) }
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendButton.isEnabled = !s.isNullOrEmpty()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun createChatRoomId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1$user2" else "$user2$user1"
    }

    private fun createOrGetChatRoom() {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val chatData = hashMapOf(
                    "participants" to listOf(currentUserId, otherUserId),
                    "lastMessage" to "",
                    "lastMessageTimestamp" to 0L,
                    "unreadCount" to mapOf(currentUserId to 0, otherUserId to 0)
                )
                chatRef.set(chatData)
            }
        }
    }

    private fun markAllMessagesAsRead() {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.update("unreadCount.$currentUserId", 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val unreadMessages = db.collection("chats").document(chatId)
                    .collection("messages")
                    .whereNotIn("readBy", listOf(currentUserId))
                    .get()
                    .await()

                val batch = db.batch()
                unreadMessages.documents.forEach { doc ->
                    batch.update(doc.reference, "readBy", FieldValue.arrayUnion(currentUserId))
                }
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messages.clear()
                    for (doc in snapshots.documents) {
                        val message = doc.toObject(ChatMessage::class.java)
                        if (message != null) {
                            messages.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage(content: String) {
        if (content.isEmpty()) return

        val message = ChatMessage(
            senderId = currentUserId,
            content = content,
            timestamp = System.currentTimeMillis(),
            readBy = listOf(currentUserId)
        )

        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages")
            .add(message)
            .addOnSuccessListener {
                messageEditText.text.clear()

                val updates = mapOf(
                    "lastMessage" to content,
                    "lastMessageTimestamp" to message.timestamp,
                    "unreadCount.$otherUserId" to FieldValue.increment(1)
                )
                chatRef.update(updates)
            }
    }
}
