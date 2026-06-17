package com.example.masterenglishfluency.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterenglishfluency.api.GeminiVoiceService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ChatViewModel : ViewModel() {

    private val systemInstruction =
        "You are a friendly English practice assistant inside a language learning app. " +
            "The user is practicing their English. Reply naturally, politely correct any clear " +
            "grammar mistakes inline, encourage them, and keep the conversation engaging. " +
            "Be concise (2-4 sentences). Reply with plain text only - no JSON, no markdown, no bullet points."

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                "Hi! I'm your English practice assistant. Voice mode is ready whenever you are.",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isConversationActive = MutableStateFlow(false)
    val isConversationActive: StateFlow<Boolean> = _isConversationActive.asStateFlow()

    private val _voiceStatus = MutableStateFlow("Tap the mic to start")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeakingResponse = MutableStateFlow(false)
    val isSpeakingResponse: StateFlow<Boolean> = _isSpeakingResponse.asStateFlow()

    private val isSending = AtomicBoolean(false)
    private var activeReplyJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var appContext: Context? = null
    private var pendingSpeechCompletion: CompletableDeferred<Unit>? = null
    private var currentUtteranceId: String? = null

    fun sendMessage(userText: String) {
        handleUserMessage(userText, shouldSpeakReply = false)
    }

    fun sendVoiceMessage(userText: String) {
        handleUserMessage(userText, shouldSpeakReply = true)
    }

    fun toggleVoiceConversation(context: Context) {
        if (_isConversationActive.value) {
            stopVoiceConversation()
        } else {
            startVoiceConversation(context)
        }
    }

    fun startVoiceConversation(context: Context) {
        val safeContext = context.applicationContext
        appContext = safeContext
        if (!SpeechRecognizer.isRecognitionAvailable(safeContext)) {
            _isConversationActive.value = false
            _voiceStatus.value = "Speech recognition not available"
            return
        }
        ensureVoiceEngines(safeContext)
        _isConversationActive.value = true
        _voiceStatus.value = "Listening..."
        startListeningInternal()
    }

    fun stopVoiceConversation() {
        _isConversationActive.value = false
        _isListening.value = false
        _isThinking.value = false
        _isSpeakingResponse.value = false
        _isLoading.value = false
        _voiceStatus.value = "Tap the mic to start"

        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Failed to cancel speech recognizer", e)
        }

        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Failed to stop TTS", e)
        }

        pendingSpeechCompletion?.complete(Unit)
        pendingSpeechCompletion = null
        currentUtteranceId = null
        activeReplyJob?.cancel()
        isSending.set(false)
    }

    private fun handleUserMessage(userText: String, shouldSpeakReply: Boolean) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return
        if (!isSending.compareAndSet(false, true)) return

        if (shouldSpeakReply) {
            _voiceStatus.value = "Thinking..."
            _isThinking.value = true
            _isListening.value = false
        } else {
            _isLoading.value = true
        }

        _messages.value = _messages.value + ChatMessage(trimmed, isUser = true)
        val historySnapshot = _messages.value

        activeReplyJob?.cancel()
        activeReplyJob = viewModelScope.launch {
            try {
                val reply = try {
                    fetchGeminiReply(historySnapshot)
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Gemini unavailable, using offline reply", e)
                    offlineReply(trimmed)
                }

                _messages.value = _messages.value + ChatMessage(reply, isUser = false)

                if (shouldSpeakReply) {
                    speakReplyAndResume(reply)
                }
            } finally {
                _isLoading.value = false
                _isThinking.value = false
                isSending.set(false)
            }
        }
    }

    private fun ensureVoiceEngines(context: Context) {
        if (speechRecognizer == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceStatus.value = "Speech recognition not available"
                return
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (_isConversationActive.value) {
                            _isListening.value = true
                            _voiceStatus.value = "Listening..."
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        if (_isConversationActive.value) {
                            _isListening.value = true
                            _voiceStatus.value = "Listening..."
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        if (_isConversationActive.value && !_isThinking.value && !_isSpeakingResponse.value) {
                            _isListening.value = false
                        }
                    }

                    override fun onError(error: Int) {
                        Log.w("ChatViewModel", "SpeechRecognizer error: $error")
                        _isListening.value = false
                        if (_isConversationActive.value && !_isThinking.value && !_isSpeakingResponse.value) {
                            _voiceStatus.value = "Listening..."
                            viewModelScope.launch {
                                delay(350)
                                startListeningInternal()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val recognized = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()

                        _isListening.value = false
                        if (recognized.isBlank()) {
                            if (_isConversationActive.value && !_isThinking.value && !_isSpeakingResponse.value) {
                                _voiceStatus.value = "Listening..."
                                viewModelScope.launch {
                                    delay(250)
                                    startListeningInternal()
                                }
                            }
                            return
                        }

                        try {
                            speechRecognizer?.cancel()
                        } catch (e: Exception) {
                            Log.w("ChatViewModel", "Failed to cancel recognizer after result", e)
                        }

                        sendVoiceMessage(recognized)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        if (textToSpeech == null) {
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engine.language = Locale.US
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            if (_isConversationActive.value) {
                                _isSpeakingResponse.value = true
                                _voiceStatus.value = "Speaking..."
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId != null && utteranceId == currentUtteranceId) {
                                pendingSpeechCompletion?.complete(Unit)
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            if (utteranceId != null && utteranceId == currentUtteranceId) {
                                pendingSpeechCompletion?.complete(Unit)
                            }
                        }
                    })
                }
            }
            textToSpeech = engine
        }
    }

    private fun startListeningInternal() {
        val context = appContext ?: return
        if (!_isConversationActive.value) return
        if (_isThinking.value || _isSpeakingResponse.value || isSending.get()) return

        ensureVoiceEngines(context)
        if (speechRecognizer == null) {
            _voiceStatus.value = "Speech recognition not available"
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk with AI")
        }

        try {
            _isListening.value = true
            _voiceStatus.value = "Listening..."
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to start speech recognition", e)
            _isListening.value = false
            _voiceStatus.value = "Listening failed"
        }
    }

    private suspend fun speakReplyAndResume(reply: String) {
        val tts = textToSpeech ?: return
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        val completion = CompletableDeferred<Unit>()
        pendingSpeechCompletion = completion

        _isSpeakingResponse.value = true
        _voiceStatus.value = "Speaking..."

        val result = withContext(Dispatchers.Main) {
            tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
        if (result != TextToSpeech.SUCCESS) {
            completion.complete(Unit)
        }

        withTimeoutOrNull(15000L) {
            completion.await()
        }
        pendingSpeechCompletion = null
        currentUtteranceId = null
        _isSpeakingResponse.value = false

        if (_isConversationActive.value) {
            _voiceStatus.value = "Listening..."
            delay(250)
            startListeningInternal()
        } else {
            _voiceStatus.value = "Tap the mic to start"
        }
    }

    private suspend fun fetchGeminiReply(history: List<ChatMessage>): String =
        withContext(Dispatchers.IO) {
            val apiKey = GeminiVoiceService.GEMINI_API_KEY
            if (apiKey.isBlank()) throw IllegalStateException("No API key configured")

            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Understood! I'll act as your friendly English practice assistant.")
                        })
                    })
                })

                history.drop(1).forEach { msg ->
                    put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", msg.text) })
                        })
                    })
                }
            }

            val payload = JSONObject().apply {
                put("contents", contents)
            }

            val conn = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            ).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 20000

            OutputStreamWriter(conn.outputStream).use {
                it.write(payload.toString())
                it.flush()
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e("ChatViewModel", "Gemini error ${conn.responseCode}: $error")
                throw Exception("HTTP ${conn.responseCode}")
            }
        }

    private fun offlineReply(userText: String): String {
        val lower = userText.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") ->
                "Hello! Great to hear from you. How are you doing today? Tell me something about yourself!"
            lower.contains("how are you") ->
                "I'm doing well, thank you for asking! How about you - what have you been up to lately?"
            lower.contains("my name is") || lower.contains("i am") ->
                "Nice to meet you! What brings you here to practice English today?"
            lower.contains("weather") ->
                "The weather is a classic conversation topic! Do you prefer sunny days or rainy ones, and why?"
            lower.contains("favourite") || lower.contains("favorite") ->
                "Interesting choice! Can you describe it in a bit more detail? That would be great practice!"
            lower.contains("work") || lower.contains("job") ->
                "That sounds interesting! What do you enjoy most about it?"
            lower.contains("learn") || lower.contains("study") || lower.contains("english") ->
                "Great goal! Consistent practice is the key. Keep chatting and you'll improve quickly!"
            lower.contains("thank") ->
                "You're very welcome! Keep it up - you're doing great. What else would you like to talk about?"
            lower.length < 10 ->
                "Nice! Can you expand on that a little? Full sentences help you practice your fluency."
            else ->
                "That's really interesting! Could you tell me more about it? Expanding your thoughts in English is excellent practice."
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Failed to destroy speech recognizer", e)
        }
        speechRecognizer = null
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Failed to shutdown TTS", e)
        }
        textToSpeech = null
    }
}
