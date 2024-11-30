package com.seoultech.onde

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ChatRoomActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()
    private lateinit var topicTextView: TextView
    private lateinit var chatRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        topicTextView = findViewById(R.id.topicTextView)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        // 예시: 두 사용자 ID
        val user1Id = "8NcSKV9oD7gs3jsErhYy2ZTLcfg2"  // 첫 번째 사용자 ID
        val user2Id = "T8oi5JNn1Qft9LumZ3619pAI2cR2"  // 두 번째 사용자 ID

        fetchUsersAndGenerateTopic(user1Id, user2Id)
    }

    private fun fetchUsersAndGenerateTopic(user1Id: String, user2Id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firestore에서 두 사용자 정보 가져오기
                val user1 = firestore.collection("users").document(user1Id).get().await()
                val user2 = firestore.collection("users").document(user2Id).get().await()

                val interests1 = user1.getString("interests") ?: ""
                val interests2 = user2.getString("interests") ?: ""

                // GPT로 주제 생성 요청
                val topic = generateSmallTalkTopic(interests1, interests2)

                withContext(Dispatchers.Main) {
                    // 주제를 UI에 표시
                    topicTextView.text = topic
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    topicTextView.text = "주제를 가져오는 데 실패했습니다."
                }
            }
        }
    }

    private suspend fun generateSmallTalkTopic(interests1: String, interests2: String): String {
        val apiUrl = "https://api.openai.com/v1/chat/completions"
//        val apiKey를 작성하면 됩니다
        val prompt = "사용자 A는 '$interests1'에 관심이 있고, 사용자 B는 '$interests2'에 관심이 있습니다. 두 관심사와 관련된 흥미로운 스몰토크 주제를 한 줄로 제안해주세요."

        val jsonBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 50)
            put("temperature", 0.7)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } else {
                "GPT 응답 실패"
            }
        } catch (e: Exception) {
            "네트워크 오류: ${e.message}"
        }
    }
}

