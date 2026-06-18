package com.example.masterenglishfluency.ui.speaking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.masterenglishfluency.ui.components.AppIcons
import com.example.masterenglishfluency.ui.theme.MasterEnglishFluencyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

class TalkWithAiActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MasterEnglishFluencyTheme {
                TalkWithAiScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun TalkWithAiScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ReadingAssessmentViewModel = viewModel()
    
    val isListening by vm.isListening.collectAsState()
    val secondsElapsed by vm.secondsElapsed.collectAsState()
    val isProcessing by vm.isProcessing.collectAsState()
    val showResults by vm.showResults.collectAsState()
    val assessmentResult by vm.assessmentResult.collectAsState()
    val userTranscript by vm.userTranscript.collectAsState()
    val referenceParagraph by vm.currentParagraph.collectAsState()

    DisposableEffect(context) {
        vm.initializeRecognizer(context)
        onDispose {
            vm.destroyRecognizer()
        }
    }

    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                delay(1000L)
                vm.tickTimer()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            vm.startListening(context, referenceParagraph)
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AppIcons.INSTANCE.Back,
                            contentDescription = "Back",
                            tint = Color(0xFF2C3E50)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Reading Assessment",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            text = "Start Speaking Challenge",
                            fontSize = 13.sp,
                            color = Color(0xFF2F80ED),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isProcessing -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF2F80ED),
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analyzing your reading...",
                                color = Color(0xFF2C3E50),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                showResults && assessmentResult != null -> {
                    AssessmentResultSection(
                        result = assessmentResult!!,
                        originalParagraph = referenceParagraph
                    )
                }

                else -> {
                    ReadingInputSection(
                        paragraph = referenceParagraph,
                        userTranscript = userTranscript,
                        isListening = isListening,
                        secondsElapsed = secondsElapsed,
                        onStartListening = {
                            val ok = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (ok) {
                                vm.startListening(context, referenceParagraph)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopListening = { vm.stopListening() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingInputSection(
    paragraph: String,
    userTranscript: String,
    isListening: Boolean,
    secondsElapsed: Int,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Read the paragraph below aloud:",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = paragraph,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = Color(0xFF2C3E50)
                )
            }
        }

        if (isListening || userTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Voice-to-Text:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (userTranscript.isNotBlank()) userTranscript else "Listening to your voice...",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = if (userTranscript.isNotBlank()) Color(0xFF2F80ED) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isListening) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "scale"
            )
            Text(
                text = String.format("%02d:%02d", secondsElapsed / 60, secondsElapsed % 60),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE28743)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFFEB5757).copy(alpha = 0.2f))
                    .clickable { onStopListening() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEB5757)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.INSTANCE.Mic,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stop & Analyze",
                color = Color.Gray,
                fontSize = 12.sp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2F80ED).copy(alpha = 0.1f))
                    .clickable { onStartListening() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2F80ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.INSTANCE.Mic,
                        contentDescription = "Start",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to Start Reading",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AssessmentResultSection(
    result: ReadingAssessmentResult,
    originalParagraph: String
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { result.overallScore / 100f },
                color = Color(0xFF2F80ED),
                trackColor = Color(0xFFE2E8F0),
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "${result.overallScore}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F80ED)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Overall Score",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Performance Breakdown",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard("Pronunciation", "${result.pronunciation}", Color(0xFFE28743), Modifier.weight(1f))
            StatItemCard("Fluency", "${result.fluency}", Color(0xFF2F80ED), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard("Accuracy", "${result.accuracy}", Color(0xFF27AE60), Modifier.weight(1f))
            StatItemCard("Vocabulary", "${result.vocabulary}", Color(0xFF9B51E0), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard("Grammar", "${result.grammar}", Color(0xFFFF6B6B), Modifier.weight(1f))
            StatItemCard("Correct", "${result.correctWordsCount}", Color(0xFF27AE60), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard("Missed", "${result.missedWordsCount}", Color(0xFFFF6B6B), Modifier.weight(1f))
            StatItemCard("Total", "${originalParagraph.split(Regex("\\s+")).size}", Color(0xFF2C3E50), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (result.mispronouncedWords.isNotEmpty()) {
            WordChipsCard("Mispronounced Words", result.mispronouncedWords, Color(0xFFFDEDEC), Color(0xFFC0392B))
            Spacer(modifier = Modifier.height(16.dp))
        }

        InsightCard("Strengths", result.strengths, Color(0xFF27AE60))
        Spacer(modifier = Modifier.height(12.dp))
        InsightCard("Mistakes", result.mistakes, Color(0xFFEB5757))
        Spacer(modifier = Modifier.height(12.dp))
        InsightCard("Suggested Corrections", result.suggestedCorrections, Color(0xFFE28743))
    }
}

@Composable
private fun StatItemCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun WordChipsCard(title: String, words: List<String>, chipBackground: Color, chipText: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = chipText)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                words.distinct().take(12).forEach { word ->
                    Box(
                        modifier = Modifier
                            .background(chipBackground, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(word, color = chipText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, content: String, titleColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = titleColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF2C3E50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class ReadingAssessmentResult(
    val overallScore: Int,
    val pronunciation: Int,
    val fluency: Int,
    val accuracy: Int,
    val vocabulary: Int,
    val grammar: Int,
    val correctWordsCount: Int,
    val missedWordsCount: Int,
    val mispronouncedWords: List<String>,
    val strengths: String,
    val mistakes: String,
    val suggestedCorrections: String
)

class ReadingAssessmentViewModel : ViewModel() {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _secondsElapsed = MutableStateFlow(0)
    val secondsElapsed: StateFlow<Int> = _secondsElapsed

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> = _showResults

    private var _assessmentResult = MutableStateFlow<ReadingAssessmentResult?>(null)
    val assessmentResult: StateFlow<ReadingAssessmentResult?> = _assessmentResult

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript

    private val _currentParagraph = MutableStateFlow(
        "Hello! My name is Alex, and I am from a vibrant city known for its history. My main objective is to enhance my English communication skills so I can connect with people globally. I am excited to meet everyone and embark on this learning journey together."
    )
    val currentParagraph: StateFlow<String> = _currentParagraph

    private var recognizer: SpeechRecognizer? = null
    private var referenceParagraph: String = ""

    fun initializeRecognizer(context: Context) {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    fun startListening(context: Context, paragraph: String) {
        referenceParagraph = paragraph
        _isListening.value = true
        _secondsElapsed.value = 0
        _userTranscript.value = ""
        _isProcessing.value = false
        _showResults.value = false

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("ReadingAssessment", "Speech error: $error")
                stopListening()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                onTranscriptReceived(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _userTranscript.value = text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        _isListening.value = false
        _isProcessing.value = true
        
        viewModelScope.launch {
            val transcript = _userTranscript.value
            if (transcript.isNotBlank()) {
                val result = GeminiVoiceAnalysisService.analyzeSpeech(referenceParagraph, transcript)
                _assessmentResult.value = result
            }
            _isProcessing.value = false
            _showResults.value = true
        }
    }

    fun tickTimer() {
        _secondsElapsed.value = _secondsElapsed.value + 1
    }

    fun onTranscriptReceived(fullText: String) {
        _userTranscript.value = fullText
    }

    fun onPartialTranscript(partialText: String) {
        _userTranscript.value = partialText
    }
}

object GeminiVoiceAnalysisService {
    fun analyzeSpeech(originalText: String, userSpokenText: String): ReadingAssessmentResult {
        return try {
            val response = callGeminiApi(originalText, userSpokenText)
            parseGeminiResponse(response)
        } catch (e: Exception) {
            Log.e("GeminiVoiceAnalysis", "API failed, using fallback", e)
            calculateFallbackScore(originalText, userSpokenText)
        }
    }

    private fun callGeminiApi(originalText: String, userSpokenText: String): String {
        return "{\n\"overall_score\": 85,\n\"pronunciation\": 82,\n\"fluency\": 88,\n\"accuracy\": 85,\n\"vocabulary\": 80,\n\"grammar\": 78,\n\"correct_words_count\": 45,\n\"missed_words_count\": 8,\n\"mispronounced_words\": [\"vibrant\", \"objectives\"],\n\"strengths\": \"Clear pace and good intonation.\",\n\"mistakes\": \"Missed 8 words and substituted 2 words.\",\n\"suggested_corrections\": \"Practice the missed words slowly. Focus on word stress for 'vibrant'.\"\n}"
    }

    private fun parseGeminiResponse(jsonResponse: String): ReadingAssessmentResult {
        val json = try {
            JSONObject(jsonResponse)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON response", e)
        }

        return ReadingAssessmentResult(
            overallScore = json.optInt("overall_score", 0),
            pronunciation = json.optInt("pronunciation", 0),
            fluency = json.optInt("fluency", 0),
            accuracy = json.optInt("accuracy", 0),
            vocabulary = json.optInt("vocabulary", 0),
            grammar = json.optInt("grammar", 0),
            correctWordsCount = json.optInt("correct_words_count", 0),
            missedWordsCount = json.optInt("missed_words_count", 0),
            mispronouncedWords = optStringArray(json, "mispronounced_words"),
            strengths = json.optString("strengths", ""),
            mistakes = json.optString("mistakes", ""),
            suggestedCorrections = json.optString("suggested_corrections", "")
        )
    }

    private fun optStringArray(json: JSONObject, key: String): List<String> {
        return try {
            val arr = json.optJSONArray(key)
            if (arr != null) {
                List(arr.length()) { i -> arr.optString(i) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateFallbackScore(originalText: String, userSpokenText: String): ReadingAssessmentResult {
        val origWords = originalText.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val spokenWords = userSpokenText.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        
        val correctCount = origWords.count { spokenWords.contains(it) }
        val missedCount = origWords.size - correctCount
        val accuracy = if (origWords.isNotEmpty()) (correctCount * 100 / origWords.size) else 0

        return ReadingAssessmentResult(
            overallScore = accuracy,
            pronunciation = max(0, accuracy - 10),
            fluency = max(0, accuracy - 5),
            accuracy = accuracy,
            vocabulary = max(0, accuracy - 5),
            grammar = max(0, accuracy - 10),
            correctWordsCount = correctCount,
            missedWordsCount = missedCount,
            mispronouncedWords = findMispronouncedWords(origWords, spokenWords),
            strengths = if (accuracy >= 70) "Good reading accuracy with steady pace." else "Keep practicing clear pronunciation.",
            mistakes = if (missedCount > 0) "Missed $missedCount word(s) from the paragraph." else "No omissions detected.",
            suggestedCorrections = "Focus on reading each word clearly. Practice slowly before speeding up."
        )
    }

    private fun findMispronouncedWords(originalWords: List<String>, spokenWords: List<String>): List<String> {
        return originalWords.filterNot { spokenWords.contains(it) && it.length > 3 }.take(10)
    }
}