package com.example.masterenglishfluency.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterenglishfluency.BuildConfig
import com.example.masterenglishfluency.api.OpenAIRealtimeVoiceSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ChatViewModel : ViewModel() {

    private val openAiApiKey = BuildConfig.OPENAI_API_KEY

    private val _isAiConnected = MutableStateFlow(openAiApiKey.isNotBlank())
    val isAiConnected: StateFlow<Boolean> = _isAiConnected.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                "Tap the mic and speak naturally. I will answer out loud using OpenAI Realtime.",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isConversationActive = MutableStateFlow(false)
    val isConversationActive: StateFlow<Boolean> = _isConversationActive.asStateFlow()

    private val _voiceStatus = MutableStateFlow(
        if (openAiApiKey.isBlank()) "Add openai.api.key to local.properties"
        else "Tap the mic to start"
    )
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeakingResponse = MutableStateFlow(false)
    val isSpeakingResponse: StateFlow<Boolean> = _isSpeakingResponse.asStateFlow()

    private var realtimeSession: OpenAIRealtimeVoiceSession? = null

    // Added for voice-to-voice integration
    fun addMessage(text: String, isUser: Boolean) {
        _messages.value = _messages.value + ChatMessage(text, isUser)
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun setThinking(thinking: Boolean) {
        _isThinking.value = thinking
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        _messages.value = _messages.value + ChatMessage(trimmed, isUser = true)

        if (_isConversationActive.value && realtimeSession != null) {
            _isThinking.value = true
            realtimeSession?.sendTextTurn(trimmed)
            return
        }

        val reply = offlineReply(trimmed)
        _messages.value = _messages.value + ChatMessage(reply, isUser = false)
    }

    fun startVoiceConversation(context: Context) {
        if (openAiApiKey.isBlank()) {
            _voiceStatus.value = "OpenAI API key is not configured"
            Log.e(TAG, "startVoiceConversation blocked: OpenAI API key is blank")
            return
        }

        if (_isConversationActive.value) return

        Log.d(TAG, "Starting voice conversation with OpenAI Realtime")
        _isConversationActive.value = true
        _isLoading.value = true
        _isThinking.value = false
        _isSpeakingResponse.value = false
        _isListening.value = true
        _voiceStatus.value = "Connecting to OpenAI..."

        realtimeSession?.stop()
        realtimeSession = OpenAIRealtimeVoiceSession(
            context = context,
            apiKey = openAiApiKey,
            instructions = systemPrompt,
            voice = "alloy",
            onStatus = { status -> _voiceStatus.value = status },
            onConnectionStateChanged = { connected ->
                Log.d(TAG, "OpenAI connection state changed: connected=" + connected)
                _isConversationActive.value = connected
                _isLoading.value = false
            },
            onUserTranscript = { text ->
                if (text.isNotBlank()) {
                    _messages.value = _messages.value + ChatMessage(text, isUser = true)
                    _isThinking.value = true
                }
            },
            onAssistantTranscript = { text ->
                if (text.isNotBlank()) {
                    _messages.value = _messages.value + ChatMessage(text, isUser = false)
                    _isThinking.value = false
                }
            },
            onSpeakingStateChanged = { speaking ->
                _isSpeakingResponse.value = speaking
                if (!speaking && _isConversationActive.value) {
                    _isListening.value = true
                }
            },
            onError = { error ->
                Log.e(TAG, "OpenAI Realtime session error: " + error)
                _voiceStatus.value = error
                _isLoading.value = false
                _isThinking.value = false
                _isSpeakingResponse.value = false
                _isListening.value = false
            }
        )
        realtimeSession?.start()
    }

    fun stopVoiceConversation() {
        Log.d(TAG, "Stopping voice conversation")
        _isConversationActive.value = false
        _isListening.value = false
        _isThinking.value = false
        _isSpeakingResponse.value = false
        _isLoading.value = false
        _voiceStatus.value = "Tap the mic to start"
        realtimeSession?.stop()
        realtimeSession = null
    }

    private fun offlineReply(userText: String): String {
        val lower = userText.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hi there. I am ready when you are. What would you like to talk about?"
            lower.contains("how are you") ->
                "I am doing well. What about you?"
            lower.contains("thank") ->
                "You are welcome. What would you like to say next?"
            else ->
                "I heard you. If you want, tap the mic and we can continue with the live voice session."
        }
    }

    private val systemPrompt = """
You are Alex, a natural English-speaking conversation partner inside a language learning app.
Keep replies short, warm, and conversational.
Respond in plain spoken language only.
Do not use markdown, bullets, or JSON.
If the user speaks in Tamil or mixed Tamil-English, gently redirect back to English practice.
Match the user's tone and keep the conversation moving with one short follow-up question.
""".trimIndent()

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel cleared")
        realtimeSession?.stop()
        realtimeSession = null
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
