package com.example.masterenglishfluency.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.masterenglishfluency.ui.components.AppIcons
import com.example.masterenglishfluency.ui.theme.MasterEnglishFluencyTheme
import java.util.Locale

class TalkWithAiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MasterEnglishFluencyTheme {
                ChatScreen(
                    onBack = { finish() },
                    viewModel = ChatViewModel()
                )
            }
        }
    }
}

// Main Composable Screen for TalkWithAi
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel
) {
    val context = LocalContext.current
    // Initialize SpeechRecognizer
    val speechRecognizer = androidx.compose.runtime.remember {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    // Stop listening on error
                    // You may want to handle specific error codes
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // TODO: Send the recognized text to the ViewModel
                        Log.d("ChatScreen", "Recognized: ${matches[0]}")
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }
    
    val messages by viewModel.messages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val isConversationActive by viewModel.isConversationActive.collectAsState()
    val voiceStatus by viewModel.voiceStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()



    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening(speechRecognizer) { listening ->
                viewModel.setListening(listening)
            }
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AppIcons.INSTANCE.Back,
                        contentDescription = "Back",
                        tint = Color(0xFF2C3E50)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Talk with AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "Voice in, voice out",
                        fontSize = 12.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { "${it.isUser}-${it.text.hashCode()}" }) { message ->
                ChatBubble(message = message)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val scale = if (isListening) 1.12f else 1f
            
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        startListening(speechRecognizer) { listening -> 
                            viewModel.setListening(listening) 
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(88.dp)
                    .scale(scale),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFEB5757) else Color(0xFF2F80ED)
                ),
                enabled = !isThinking && speechRecognizer != null
            ) {
                Icon(
                    imageVector = AppIcons.INSTANCE.Mic,
                    contentDescription = "Talk with AI",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isThinking -> "Thinking..."
                    isListening -> "Listening..."
                    else -> "Tap mic to speak"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
        }
    }
}

// Individual chat message bubble
@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) Color(0xFF2F80ED) else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) "You" else "AI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) Color(0xFFD6E8FF) else Color(0xFF7F8C8D)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) Color.White else Color(0xFF2C3E50)
                )
            }
        }
    }
}

// Start listening with SpeechRecognizer
private fun startListening(
    recognizer: SpeechRecognizer?,
    onListeningStateChange: (Boolean) -> Unit
) {
    val recognizerInstance = recognizer ?: return
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    }
    onListeningStateChange(true)
    recognizerInstance.startListening(intent)
}

// Placeholder for Gemini API integration - replace with actual implementation
fun fetchGeminiResponse(
    userText: String,
    callback: (String) -> Unit
) {
    Thread {
        try {
            // TODO: Integrate with Google Gemini API here
            // Example using OkHttp/Retrofit:
            // val request = GeminiRequest(
            //     contents = listOf(Content(
            //         parts = listOf(Part(text = userText))
            //     ))
            // )
            // val call = geminiService.generateContent(request)
            // val response = call.execute().body()
            // callback(response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Sorry, I didn't understand.")

            // Placeholder response for testing
            val response = when {
                userText.contains("hello", ignoreCase = true) ->
                    "Hello! How can I help you today?"
                userText.contains("how are you", ignoreCase = true) ->
                    "I'm doing well, thank you for asking!"
                userText.contains("bye", ignoreCase = true) ->
                    "Goodbye! Have a great day!"
                else ->
                    "I heard you say: $userText. How can I assist you further?"
            }
            callback(response)
        } catch (e: Exception) {
            Log.e("ChatScreen", "Gemini API error", e)
            callback("Sorry, I encountered an error. Please try again.")
        }
    }.start()
}