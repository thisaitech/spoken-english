package com.example.masterenglishfluency.api

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class OpenAIRealtimeVoiceSession(
    context: Context,
    private val apiKey: String,
    private val model: String = "gpt-4o-realtime-preview-2024-12",
    private val instructions: String,
    private val voice: String = "alloy",
    private val onStatus: (String) -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onUserTranscript: (String) -> Unit,
    private val onAssistantTranscript: (String) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()
    private val started = AtomicBoolean(false)

    private var socket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private var playbackPrimed = false
    private var audioChunksSent = AtomicInteger(0)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        audioChunksSent.set(0)
        Log.d(TAG, "Starting OpenAI Realtime session: model=$model voice=$voice")
        onStatus("Connecting to OpenAI...")

        val request = Request.Builder()
            .url("wss://api.openai.com/v1/realtime?model=$model")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket onOpen: code=${response.code} message=${response.message}")
                onConnectionStateChanged(true)
                onStatus("OpenAI Connected")
                sendSetup(webSocket)
                startMicrophoneStream(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                stopAudio()
                onConnectionStateChanged(false)
                onSpeakingStateChanged(false)
                onStatus("Tap the mic to start")
                started.set(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val responseSummary = response?.let { "http=${it.code} message=${it.message}" } ?: "no-http-response"
                Log.e(TAG, "Realtime API websocket failure ($responseSummary)", t)
                stopAudio()
                onConnectionStateChanged(false)
                onSpeakingStateChanged(false)
                onError("Connection Failed: ${t.message ?: "unknown error"}")
                onStatus("Connection Failed: ${t.message ?: "unknown error"}")
                started.set(false)
            }
        })
    }

    fun stop() {
        started.set(false)
        recordJob?.cancel()
        recordJob = null
        stopAudio()
        try {
            socket?.close(1000, "user-stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close socket", e)
        }
        socket = null
        scope.cancel()
    }

    fun sendTextTurn(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val payload = JSONObject().apply {
            put("type", "conversation.item.create")
            put("item", JSONObject().apply {
                put("type", "message")
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "input_text")
                        put("text", trimmed)
                    })
                })
            })
        }
        socket?.send(payload.toString())
        val responsePayload = JSONObject().apply {
            put("type", "response.create")
        }
        socket?.send(responsePayload.toString())
    }

    private fun sendSetup(webSocket: WebSocket) {
        val payload = JSONObject().apply {
            put("type", "session.update")
            put("session", JSONObject().apply {
                put("instructions", instructions)
                put("voice", voice)
                put("turn_detection", JSONObject().apply {
                    put("type", "server_vad")
                })
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")
            })
        }
        Log.d(TAG, "Sending session.update payload")
        webSocket.send(payload.toString())
    }

    private fun startMicrophoneStream(webSocket: WebSocket) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            onError("AudioRecord is not available on this device.")
            return
        }

        val bufferSize = max(minBuffer, SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE / 10)
        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("Unable to initialize the microphone.")
            return
        }

        audioRecord = record
        record.startRecording()
        onStatus("Recording Audio")
        Log.d(TAG, "AudioRecord started: sampleRate=$SAMPLE_RATE bufferSize=$bufferSize")

        recordJob = scope.launch {
            val buffer = ByteArray(bufferSize)
            while (isActive && started.get()) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val audioBytes = if (read == buffer.size) buffer else buffer.copyOf(read)
                    if (audioChunksSent.getAndIncrement() == 0) {
                        onStatus("Sending Audio")
                    }
                    Log.d(TAG, "Sending audio chunk bytes=$read")
                    val chunk = JSONObject().apply {
                        put("type", "input_audio_buffer.append")
                        put("audio", Base64.encodeToString(audioBytes, Base64.NO_WRAP))
                    }
                    webSocket.send(chunk.toString())
                } else {
                    delay(10)
                }
            }
        }
    }

    private fun handleServerMessage(text: String) {
        try {
            val root = JSONObject(text)
            val type = root.optString("type")

            when (type) {
                "error" -> {
                    val error = root.optJSONObject("error")
                    val message = error?.optString("message", "Realtime API returned an error.") ?: "Realtime API returned an error."
                    val code = error?.opt("code")?.toString()?.let { " code=$it" }.orEmpty()
                    Log.e(TAG, "Realtime API error payload:$code message=$message raw=$text")
                    onError("Connection Failed: $message")
                    onStatus("Connection Failed: $message")
                }
                "session.created" -> {
                    Log.d(TAG, "Session created")
                    onStatus("OpenAI Connected")
                }
                "session.updated" -> {
                    Log.d(TAG, "Session updated")
                    onStatus("OpenAI Connected")
                }
                "input_audio_buffer.speech_started" -> {
                    Log.d(TAG, "Speech started")
                    onSpeakingStateChanged(true)
                    onStatus("Listening...")
                }
                "input_audio_buffer.speech_finished" -> {
                    Log.d(TAG, "Speech finished")
                    onStatus("Processing...")
                }
                "response.created" -> {
                    Log.d(TAG, "Response created")
                    onSpeakingStateChanged(true)
                }
                "response.text.delta" -> {
                    val delta = root.optString("delta", "")
                    if (delta.isNotBlank()) {
                        onAssistantTranscript(delta)
                    }
                }
                "response.text.done" -> {
                    val text = root.optString("text", "")
                    if (text.isNotBlank()) {
                        onAssistantTranscript(text)
                    }
                }
                "response.audio.delta" -> {
                    val delta = root.optString("delta", "")
                    if (delta.isNotBlank()) {
                        onStatus("Receiving Audio Response")
                        Log.d(TAG, "Received audio response chunk base64Length=${delta.length}")
                        playAudio(Base64.decode(delta, Base64.DEFAULT))
                    }
                }
                "response.audio.done" -> {
                    Log.d(TAG, "Audio response done")
                }
                "response.done" -> {
                    Log.d(TAG, "Response done")
                    onSpeakingStateChanged(false)
                    onStatus("OpenAI Connected")
                }
                "conversation.item.created" -> {
                    root.optJSONObject("item")?.let { item ->
                        if (item.optString("role") == "user") {
                            item.optJSONArray("content")?.let { content ->
                                for (i in 0 until content.length()) {
                                    val contentItem = content.optJSONObject(i)
                                    if (contentItem != null) {
                                        val transcript = contentItem.optString("transcript").trim()
                                        if (transcript.isNotBlank()) {
                                            onUserTranscript(transcript)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Realtime API message: $text", e)
        }
    }

    private fun playAudio(audioBytes: ByteArray) {
        if (audioBytes.isEmpty()) return
        if (!playbackPrimed) {
            audioTrack = createAudioTrack()
            playbackPrimed = true
        }
        val track = audioTrack ?: return
        onSpeakingStateChanged(true)
        Log.d(TAG, "Playing audio response bytes=${audioBytes.size}")
        track.play()
        track.write(audioBytes, 0, audioBytes.size)
    }

    private fun createAudioTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuffer, SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE / 5)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun stopAudio() {
        try {
            recordJob?.cancel()
            recordJob = null
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord stop failed", e)
        } finally {
            try {
                audioRecord?.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord release failed", e)
            }
            audioRecord = null
        }

        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack stop failed", e)
        } finally {
            try {
                audioTrack?.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack release failed", e)
            }
            audioTrack = null
            playbackPrimed = false
        }
    }

    companion object {
        private const val TAG = "OpenAIRealtimeVoiceSession"
        private const val SAMPLE_RATE = 24_000
        private const val OUTPUT_SAMPLE_RATE = 24_000
        private const val CHANNEL_COUNT = 1
        private const val BYTES_PER_SAMPLE = 2
    }
}

