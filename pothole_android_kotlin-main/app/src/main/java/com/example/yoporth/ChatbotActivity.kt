package com.example.yoporth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(val content: String, val isUser: Boolean)

class ChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var progressBar: ProgressBar
    
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val DEEPSEEK_API_KEY = "sk-f4f1103135f64215a288a870bdee5614"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        progressBar = findViewById(R.id.progressBar)

        chatAdapter = ChatAdapter(messages)
        recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = chatAdapter
        }

        buttonSend.setOnClickListener {
            val query = editTextMessage.text.toString().trim()
            if (query.isNotEmpty()) {
                addMessage(query, true)
                editTextMessage.text.clear()
                getAIResponse(query)
            }
        }
        
        // Initial bot message
        addMessage("Hello! I am your Monkey Pox Detector assistant. Ask me anything about Monkey Pox symptoms, prevention, or treatment.", false)
    }

    private fun addMessage(content: String, isUser: Boolean) {
        messages.add(ChatMessage(content, isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerViewChat.scrollToPosition(messages.size - 1)
    }

    private fun getAIResponse(userQuery: String) {
        progressBar.visibility = View.VISIBLE
        buttonSend.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // System prompt to restrict topics
                val systemPrompt = "You are Monkey Pox Detector AI. You ONLY answer questions related to Monkey Pox disease, symptoms, prevention, and healthcare advice regarding Monkey Pox. If the user asks about anything else, politely decline and say you can only help with Monkey Pox-related inquiries."

                val json = JSONObject().apply {
                    put("model", "deepseek-chat")
                    val messagesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userQuery)
                        })
                    }
                    put("messages", messagesArray)
                }

                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://api.deepseek.com/chat/completions")
                    .addHeader("Authorization", "Bearer $DEEPSEEK_API_KEY")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseData = response.body?.string()
                    if (response.isSuccessful && responseData != null) {
                        val responseJson = JSONObject(responseData)
                        val botResponse = responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                        withContext(Dispatchers.Main) {
                            addMessage(botResponse, false)
                        }
                    } else {
                        throw Exception("API error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatbotActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    buttonSend.isEnabled = true
                }
            }
        }
    }

    inner class ChatAdapter(private val chatMessages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = chatMessages[position]
            holder.bind(message)
        }

        override fun getItemCount() = chatMessages.size

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(R.id.textViewMessage)
            private val cardView: MaterialCardView = itemView.findViewById(R.id.cardViewMessage)
            private val container: LinearLayout = itemView as LinearLayout

            fun bind(message: ChatMessage) {
                textView.text = message.content
                val params = cardView.layoutParams as LinearLayout.LayoutParams
                
                if (message.isUser) {
                    container.gravity = android.view.Gravity.END
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.red_primary))
                    textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    params.marginStart = 100
                    params.marginEnd = 0
                } else {
                    container.gravity = android.view.Gravity.START
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                    params.marginStart = 0
                    params.marginEnd = 100
                }
                cardView.layoutParams = params
            }
        }
    }
}
