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

class GeminiLiveVoiceSession(
    context: Context,
    private val apiKey: String,
    private val model: String,
    private val systemInstruction: String,
    private val voiceName: String = "Kore",
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
    private val normalizedModel = if (model.startsWith("models/")) model else "models/$model"

    private var socket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private var playbackPrimed = false
    private var audioChunksSent = AtomicInteger(0)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        audioChunksSent.set(0)
        Log.d(TAG, "Starting Gemini Live session: model=$normalizedModel voice=$voiceName")
        onStatus("Connecting to Gemini...")

        val request = Request.Builder()
            .url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket onOpen: code=${response.code} message=${response.message}")
                onConnectionStateChanged(true)
                onStatus("Gemini Connected")
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
                Log.e(TAG, "Live API websocket failure ($responseSummary)", t)
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
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", trimmed)
                            })
                        })
                    })
                })
                put("turnComplete", true)
            })
        }
        socket?.send(payload.toString())
    }

    private fun sendSetup(webSocket: WebSocket) {
        val payload = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", normalizedModel)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }
        Log.d(TAG, "Sending setup payload")
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
                        put("realtimeInput", JSONObject().apply {
                            put("mediaChunks", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("mimeType", "audio/pcm;rate=$SAMPLE_RATE")
                                    put("data", Base64.encodeToString(audioBytes, Base64.NO_WRAP))
                                })
                            })
                        })
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

            root.optJSONObject("error")?.let { error ->
                val message = error.optString("message", "Gemini Live returned an error.")
                val code = error.opt("code")?.toString()?.let { " code=$it" }.orEmpty()
                Log.e(TAG, "Gemini Live error payload:$code message=$message raw=$text")
                onError("Connection Failed: $message")
                onStatus("Connection Failed: $message")
                return
            }

            root.optJSONObject("serverContent")?.let { serverContent ->
                val turnComplete = serverContent.optBoolean("turnComplete", false)
                val modelTurn = serverContent.optJSONObject("modelTurn")
                if (modelTurn != null) {
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.optJSONObject(i) ?: continue
                            part.optString("text").takeIf { it.isNotBlank() }?.let { textPart ->
                                onAssistantTranscript(textPart)
                            }
                            val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                            val audioData = inlineData?.optString("data").orEmpty()
                            if (audioData.isNotBlank()) {
                                onStatus("Receiving Audio Response")
                                Log.d(TAG, "Received audio response chunk base64Length=${audioData.length}")
                                playAudio(Base64.decode(audioData, Base64.DEFAULT))
                            }
                        }
                    }
                }

                if (turnComplete) {
                    Log.d(TAG, "Server turn complete")
                    onSpeakingStateChanged(false)
                    onStatus("Gemini Connected")
                }
                return
            }

            root.optJSONObject("setupComplete")?.let {
                Log.d(TAG, "Setup complete acknowledged by Gemini")
                onStatus("Gemini Connected")
                return
            }

            root.optJSONObject("clientContent")?.let { clientContent ->
                val turns = clientContent.optJSONArray("turns")
                if (turns != null) {
                    for (i in 0 until turns.length()) {
                        val turn = turns.optJSONObject(i) ?: continue
                        val parts = turn.optJSONArray("parts") ?: continue
                        for (j in 0 until parts.length()) {
                            val part = parts.optJSONObject(j) ?: continue
                            val textPart = part.optString("text").trim()
                            if (textPart.isNotBlank()) {
                                onUserTranscript(textPart)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Live API message: $text", e)
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
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuffer, OUTPUT_SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE / 5)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
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
        private const val TAG = "GeminiLiveVoiceSession"
        private const val SAMPLE_RATE = 16_000
        private const val OUTPUT_SAMPLE_RATE = 24_000
        private const val CHANNEL_COUNT = 1
        private const val BYTES_PER_SAMPLE = 2
    }
}
