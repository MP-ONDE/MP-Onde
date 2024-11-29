import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.MaterialToolbar
import com.seoultech.onde.R
import com.seoultech.onde.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.delay
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Semaphore

class AIRecommendationBottomSheetFragment : DialogFragment() {
//    private val apiKey를 작성하면 됩니다
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_ai_recommendation, container, false)

        val toolbar: MaterialToolbar = view.findViewById(R.id.topAppBarSmallTalk)
        toolbar.setNavigationOnClickListener {
            dismiss() // DialogFragment 닫기
        }

        val crawledSentences: TextView = view.findViewById(R.id.crawledSentences)
        val askAiButton: Button = view.findViewById(R.id.askAiButton)
        val aiQuestionInput: EditText = view.findViewById(R.id.aiQuestionInput)
        val gptAnswer: TextView = view.findViewById(R.id.gptAnswer)

        // 처음에 크롤링해서 텍스트를 바로 표시하도록 설정
        fetchCrawledData(crawledSentences)

        // 버튼 클릭 시 크롤링 및 GPT 응답 요청
        askAiButton.setOnClickListener {
            val userQuestion = aiQuestionInput.text.toString().trim()

            // 크롤링 수행
            fetchCrawledData(crawledSentences)

            // GPT 질문
            if (userQuestion.isNotEmpty()) {
                fetchGPTResponse(userQuestion, gptAnswer)
            } else {
                Toast.makeText(context, "AI에게 질문을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun fetchCrawledData(crawledSentences: TextView) {
        val call = RetrofitClient.instance.getCrawledData()
        call.enqueue(object : Callback<List<String>> {
            override fun onResponse(
                call: Call<List<String>>,
                response: Response<List<String>>
            ) {
                if (response.isSuccessful) {
                    val data = response.body() ?: emptyList()

                    // 랜덤으로 하나의 문장 선택
                    val randomText = if (data.isNotEmpty()) {
                        data.random() // List에서 랜덤으로 하나 선택
                    } else {
                        "추천 멘트가 없습니다." // 데이터가 없을 경우 기본 문구
                    }

                    crawledSentences.text = randomText
                } else {
                    Log.e("APIError", "Response not successful: ${response.code()} - ${response.message()}")
                    crawledSentences.text = "서버 응답 실패: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.e("APIError", "Network error: ${t.message}", t)
                crawledSentences.text = "네트워크 오류: ${t.message}"
            }
        })
    }



    private fun fetchGPTResponse(userQuestion: String, gptAnswer: TextView) {
        val apiUrl = "https://api.openai.com/v1/chat/completions"
        val client = OkHttpClient()
        val semaphore = Semaphore(1) // 동시 요청 수 제한 (1로 설정)

        val jsonBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an assistant.") // 시스템 역할 지정
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuestion)
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

        CoroutineScope(Dispatchers.IO).launch {
            semaphore.acquire() // 요청 시작 시 세마포어 획득
            try {
                Log.d("GPTRequest", "요청 시작: $userQuestion")
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val gptResponse = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()

                    withContext(Dispatchers.Main) {
                        gptAnswer.text = gptResponse
                    }

                    Log.d("GPTRequest", "응답 성공: $gptResponse")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "GPT 응답 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }

                    Log.e("GPTRequest", "응답 실패: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                Log.e("GPTRequest", "네트워크 오류: ${e.message}", e)
            } finally {
                semaphore.release() // 요청 완료 시 세마포어 해제
                Log.d("GPTRequest", "요청 종료: $userQuestion")

                // 요청 간 딜레이 추가 (1초)
                delay(1000)
            }
        }
    }





    companion object {
        fun newInstance(): AIRecommendationBottomSheetFragment {
            return AIRecommendationBottomSheetFragment()
        }
    }
}
