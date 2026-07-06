package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatDatabase
import com.example.data.MessageEntity
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getDatabase(application)
    private val messageDao = db.messageDao()

    val messages: StateFlow<List<MessageEntity>> = messageDao.getAllMessagesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _apiKeyWarning = MutableStateFlow(false)
    val apiKeyWarning: StateFlow<Boolean> = _apiKeyWarning.asStateFlow()

    init {
        checkApiKey()
    }

    private fun checkApiKey() {
        val key = BuildConfig.GEMINI_API_KEY
        val isPlaceholder = key.isEmpty() || 
                key == "MY_GEMINI_API_KEY" || 
                key == "YOUR_API_KEY" || 
                key.contains("PLACEHOLDER", ignoreCase = true)
        _apiKeyWarning.value = isPlaceholder
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            // Check API key again
            checkApiKey()

            // 1. Insert User Message
            val userMsg = MessageEntity(sender = "USER", text = text)
            messageDao.insertMessage(userMsg)

            // 2. Set loading state
            _isGenerating.value = true

            try {
                // 3. Collect history to send to Gemini (to support context/chat flow!)
                val history = messages.value.filter { !it.isError && !it.isPending }
                val contents = mutableListOf<Content>()

                // Limit conversation history to last 10 turns to avoid exceeding context limits
                val limitedHistory = history.takeLast(10)
                for (msg in limitedHistory) {
                    val role = if (msg.sender == "USER") "user" else "model"
                    contents.add(
                        Content(
                            parts = listOf(Part(text = msg.text))
                        )
                    )
                }

                // Add current message if not already included (it was just saved, so it is in `messages.value`)
                if (contents.isEmpty() || contents.last().parts.firstOrNull()?.text != text) {
                    contents.add(
                        Content(parts = listOf(Part(text = text)))
                    )
                }

                val systemInstructionText = "You are a helpful, smart, and friendly AI chatbot helper called 'AI सहायक'. Answer concisely, naturally, and accurately in the language of the user (Hindi, English, or Hinglish as requested)."
                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
                )

                // 4. API Call
                val apiKey = BuildConfig.GEMINI_API_KEY
                val response = RetrofitClient.service.generateContent(apiKey, request)

                // 5. Insert AI Response
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!aiText.isNullOrBlank()) {
                    val aiMsg = MessageEntity(sender = "AI", text = aiText)
                    messageDao.insertMessage(aiMsg)
                } else {
                    val errorMsg = MessageEntity(
                        sender = "AI",
                        text = "माफ़ कीजिये, मुझे कोई उत्तर नहीं मिला। कृपया पुनः प्रयास करें।",
                        isError = true
                    )
                    messageDao.insertMessage(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = MessageEntity(
                    sender = "AI",
                    text = "त्रुटि: ${e.localizedMessage ?: "नेटवर्क समस्या, कृपया जांचें।"} (सुनिश्चित करें कि आपका API कुंजी सही ढंग से कॉन्फ़िगर किया गया है)",
                    isError = true
                )
                messageDao.insertMessage(errorMsg)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.clearChat()
        }
    }
}
