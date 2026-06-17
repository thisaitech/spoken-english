package com.example.masterenglishfluency.ui.speaking

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.masterenglishfluency.data.SpeakingSession
import com.example.masterenglishfluency.ui.components.AppIcons
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SpeakingPracticeScreen(
    onBack: () -> Unit,
    viewModel: SpeakingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val secondsElapsed by viewModel.secondsElapsed.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val showFeedback by viewModel.showFeedback.collectAsState()
    val topicIndex by viewModel.topicIndex.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val originalParagraph by viewModel.currentParagraph.collectAsState()
    val userRealSpeech by viewModel.userRealSpeech.collectAsState()
    val isSessionValid by viewModel.isSessionValid.collectAsState()
    val isAssistantEnabled by viewModel.isAssistantEnabled.collectAsState()
    val assistantStatus by viewModel.assistantStatus.collectAsState()
    val assistantMessages by viewModel.assistantMessages.collectAsState()
    val assistantReplyText by viewModel.assistantReplyText.collectAsState()

    val topics = viewModel.topics.collectAsState().value
    val rawTopic = topics.getOrNull(topicIndex) ?: "Reading Practice"
    val topicTitle = rawTopic.split(":").firstOrNull()?.trim() ?: rawTopic
    val scrollState = rememberScrollState()

    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }
    DisposableEffect(context) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) textToSpeech?.language = Locale.US
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    LaunchedEffect(assistantReplyText) {
        if (assistantReplyText.isNotBlank()) {
            tts?.speak(assistantReplyText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startSpeaking(context)
        else Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            while (true) {
                delay(1000L)
                viewModel.tickTimer()
            }
        }
    }

    fun speak(text: String) {
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun startReading() {
        val ok = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (ok) viewModel.startSpeaking(context) else permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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
                            text = "Topic: $topicTitle",
                            fontSize = 13.sp,
                            color = Color(0xFF2F80ED),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!showFeedback && !isProcessing) {
                    TextButton(onClick = { viewModel.nextTopic() }) {
                        Text(
                            text = "Next Topic",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2F80ED),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isProcessing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF2F80ED),
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Converting voice to text and analyzing your reading...",
                                color = Color(0xFF2C3E50),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                !showFeedback -> {
                    ReadingInputSection(
                        originalParagraph = originalParagraph,
                        userRealSpeech = userRealSpeech,
                        isSpeaking = isSpeaking,
                        secondsElapsed = secondsElapsed,
                        onStart = ::startReading,
                        onStop = { viewModel.stopSpeakingAndProcess() },
                        onSpeakParagraph = { speak(originalParagraph) },
                        tts = tts
                    )
                }

                else -> {
                    val session = currentSession
                    if (session != null) {
                        AssessmentReportSection(
                            session = session,
                            originalParagraph = originalParagraph,
                            viewModel = viewModel,
                            isAssistantEnabled = isAssistantEnabled,
                            assistantMessages = assistantMessages,
                            assistantStatus = assistantStatus,
                            onSpeakFeedback = { viewModel.speakFeedback(context) },
                            onEnableAssistant = { viewModel.enableAssistant(context) },
                            onAskVoiceQuestion = { viewModel.startAssistantListening(context) },
                            onStopAssistant = { viewModel.stopAssistantListening() },
                            onSave = { viewModel.saveSession() },
                            isSessionValid = isSessionValid,
                            onDiscard = { viewModel.discardSession() },
                            onTryAgain = { viewModel.discardSession() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingInputSection(
    originalParagraph: String,
    userRealSpeech: String,
    isSpeaking: Boolean,
    secondsElapsed: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSpeakParagraph: () -> Unit,
    tts: TextToSpeech?
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read the paragraph below aloud:",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { onSpeakParagraph() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.INSTANCE.Volume,
                            contentDescription = "Listen",
                            tint = Color(0xFF2F80ED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = originalParagraph,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = Color(0xFF2C3E50)
                )
            }
        }

        if (isSpeaking || userRealSpeech.isNotBlank()) {
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
                        text = if (userRealSpeech.isNotBlank()) userRealSpeech else "Listening to your voice...",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = if (userRealSpeech.isNotBlank()) Color(0xFF2F80ED) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (isSpeaking) {
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
                    .clickable { onStop() },
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
                    .clickable { onStart() },
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
private fun AssessmentReportSection(
    session: SpeakingSession,
    originalParagraph: String,
    viewModel: SpeakingViewModel,
    isAssistantEnabled: Boolean,
    assistantMessages: List<SpeakingViewModel.AssistantMessage>,
    assistantStatus: String,
    onSpeakFeedback: () -> Unit,
    onEnableAssistant: () -> Unit,
    onAskVoiceQuestion: () -> Unit,
    onStopAssistant: () -> Unit,
    onSave: () -> Unit,
    isSessionValid: Boolean,
    onDiscard: () -> Unit,
    onTryAgain: () -> Unit
) {
    val scrollState = rememberScrollState()
    val cleanRegex = Regex("[^a-zA-Z0-9\\s]")
    val spokenSet = remember(session.id, session.transcript) {
        cleanRegex.replace(session.transcript, "").lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    }
    val skippedSentences = remember(session.id, originalParagraph, session.transcript) {
        viewModel.computeSkippedSentences(originalParagraph, spokenSet)
    }
    val sentenceMistakes = remember(session.id, session.sentenceMistakesList, skippedSentences) {
        session.sentenceMistakesList.takeIf { it.isNotEmpty() } ?: skippedSentences.map { sentence ->
            "Missed or incomplete sentence: \"$sentence\"."
        }
    }
    val vocabularyFeedbackText = remember(session.id, session.vocabularyFeedbackText, session.vocabularyScore) {
        session.vocabularyFeedbackText.ifBlank {
            when {
                session.vocabularyScore >= 85 -> "Strong vocabulary coverage. You included most key words from the paragraph."
                session.vocabularyScore >= 60 -> "Good vocabulary coverage. Review the missing key words and repeat the paragraph slowly."
                session.vocabularyScore >= 35 -> "Partial vocabulary coverage. Focus on the important words highlighted in the paragraph."
                else -> "Low vocabulary coverage. Re-read the paragraph, then practice one sentence at a time."
            }
        }
    }
    val overallResultMessage = remember(session.id, session.overallResultMessage, session.overallScore) {
        session.overallResultMessage.ifBlank {
            when {
                session.overallScore >= 90 -> "Excellent reading assessment. Your pronunciation, fluency, and accuracy are ready for the next challenge."
                session.overallScore >= 75 -> "Good reading performance. Keep improving sentence completeness and smooth pacing."
                session.overallScore >= 60 -> "Fair reading performance. Review the sentence mistakes and vocabulary gaps before trying again."
                else -> "Needs more practice. Read the paragraph in short sections and record again."
            }
        }
    }
    val hasCleanReading = session.mispronouncedWordsList.isEmpty() &&
        session.missedWordsList.isEmpty() &&
        sentenceMistakes.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Assessment Results",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF27AE60)
        )
        Spacer(modifier = Modifier.height(10.dp))

        OverallResultCard(message = overallResultMessage, score = session.overallScore)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard("2. Pronunciation", "${session.pronunciationScore}", Color(0xFFE28743), Modifier.weight(1f))
            StatItemCard("4. Fluency", "${session.fluencyScore}", Color(0xFF2F80ED), Modifier.weight(1f))
            StatItemCard("Vocabulary", "${session.vocabularyScore}", Color(0xFF27AE60), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))

        HighlightedParagraphCard(
            paragraph = originalParagraph,
            transcript = session.transcript,
            extraWords = session.extraWordsList,
            missedWords = session.missedWordsList
        )
        Spacer(modifier = Modifier.height(16.dp))

        SentenceMistakesCard(mistakes = sentenceMistakes, skippedSentences = skippedSentences)
        Spacer(modifier = Modifier.height(16.dp))

        VocabularyFeedbackCard(
            feedback = vocabularyFeedbackText,
            score = session.vocabularyScore,
            matchedCount = session.paragraphText
                .takeIf { it.isNotBlank() }
                ?.let { viewModel.computeParagraphVocabSpoken(it, spokenSet).size }
                ?: 0
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard("Accuracy", "${session.accuracyScore}", Color(0xFF27AE60), Modifier.weight(1f))
            StatItemCard("Correct Words", "${session.wordsCorrectCount}", Color(0xFF27AE60), Modifier.weight(1f))
            StatItemCard("Missed Words", "${session.wordsMissedCount}", Color(0xFFEB5757), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (session.mispronouncedWordsList.isNotEmpty()) {
            WordChipsCard("Mispronounced / Substituted Words", session.mispronouncedWordsList, Color(0xFFFDEDEC), Color(0xFFC0392B))
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (session.extraWordsList.isNotEmpty()) {
            WordChipsCard("Extra Words", session.extraWordsList, Color(0xFFFFF2E8), Color(0xFFE28743))
            Spacer(modifier = Modifier.height(16.dp))
        }

        InsightCard("Strengths", session.strengthsText, Color(0xFF27AE60))
        Spacer(modifier = Modifier.height(12.dp))
        InsightCard("Mistakes", session.mistakesText, Color(0xFFEB5757))
        Spacer(modifier = Modifier.height(12.dp))
        InsightCard("Suggested Corrections", session.suggestedCorrectionsText.ifBlank {
            if (hasCleanReading) "Okay - no corrections needed." else "Review the missed words and sentences above."
        }, Color(0xFFE28743))
        Spacer(modifier = Modifier.height(16.dp))

        VoiceAssistantCard(
            isAssistantEnabled = isAssistantEnabled,
            status = assistantStatus,
            messages = assistantMessages,
            onSpeakFeedback = onSpeakFeedback,
            onEnableAssistant = onEnableAssistant,
            onAskVoiceQuestion = onAskVoiceQuestion,
            onStopAssistant = onStopAssistant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            enabled = isSessionValid,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60))
        ) {
            Text("Save to Firebase & History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Discard", color = Color(0xFFEB5757), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTryAgain,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun VoiceAssistantCard(
    isAssistantEnabled: Boolean,
    status: String,
    messages: List<SpeakingViewModel.AssistantMessage>,
    onSpeakFeedback: () -> Unit,
    onEnableAssistant: () -> Unit,
    onAskVoiceQuestion: () -> Unit,
    onStopAssistant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Voice Assistant",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = if (isAssistantEnabled) "On" else "Off",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAssistantEnabled) Color(0xFF27AE60) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAssistantEnabled) status.ifBlank { "AI feedback is ready. Ask a question by voice." } else "Enable the assistant to hear feedback aloud and ask voice questions.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isAssistantEnabled) {
                    Button(
                        onClick = onSpeakFeedback,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        Text("Speak Feedback", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onAskVoiceQuestion,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60))
                    ) {
                        Text("Ask by Voice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onStopAssistant,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Stop", color = Color(0xFFEB5757), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onEnableAssistant,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                    ) {
                        Text("Enable AI Voice Assistant", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            if (messages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    messages.takeLast(6).forEach { message ->
                        Text(
                            text = "${message.speaker}: ${message.text}",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (message.speaker == "You") Color(0xFF2F80ED) else Color(0xFF2C3E50),
                            fontWeight = if (message.speaker == "You") FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SentenceMistakesCard(
    mistakes: List<String>,
    skippedSentences: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (mistakes.isEmpty()) Color(0xFFE8F8F0) else Color(0xFFFDEDEC),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = if (mistakes.isEmpty()) Color(0xFFF4FBF7) else Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "1. Sentence Mistakes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (mistakes.isEmpty()) Color(0xFF27AE60) else Color(0xFFEB5757)
                )
                Text(
                    "${mistakes.size} found",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (mistakes.isEmpty()) Color(0xFF27AE60) else Color(0xFFEB5757)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (mistakes.isEmpty()) {
                Text(
                    "No sentence mistakes detected. You covered the paragraph's sentences well.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF2C3E50)
                )
            } else {
                mistakes.take(5).forEach { mistake ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•  ", color = Color(0xFFEB5757), fontSize = 12.sp)
                        Text(
                            text = mistake,
                            fontSize = 12.sp,
                            color = Color(0xFF7F8C8D),
                            lineHeight = 18.sp
                        )
                    }
                }
                if (skippedSentences.isNotEmpty()) {
                    Text(
                        "Tip: Re-read the highlighted sentences aloud 3 times before your next attempt.",
                        fontSize = 12.sp,
                        color = Color(0xFFE28743),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabularyFeedbackCard(
    feedback: String,
    score: Int,
    matchedCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "3. Vocabulary Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2C3E50)
                )
                Text("$score / 100", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2F80ED))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                feedback,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("$matchedCount key vocabulary words matched", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun OverallResultCard(message: String, score: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    score >= 80 -> Color(0xFFE8F8F0)
                    score >= 60 -> Color(0xFFFFF2E8)
                    else -> Color(0xFFFDEDEC)
                },
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = when {
            score >= 80 -> Color(0xFFF4FBF7)
            score >= 60 -> Color(0xFFFFFBFA)
            else -> Color(0xFFFFFBFA)
        }),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "5. Overall Result",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = when {
                        score >= 80 -> Color(0xFF27AE60)
                        score >= 60 -> Color(0xFFE28743)
                        else -> Color(0xFFEB5757)
                    }
                )
                Text(
                    "$score / 100",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = when {
                        score >= 80 -> Color(0xFF27AE60)
                        score >= 60 -> Color(0xFFE28743)
                        else -> Color(0xFFEB5757)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF2C3E50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HighlightedParagraphCard(
    paragraph: String,
    transcript: String,
    extraWords: List<String>,
    missedWords: List<String>
) {
    val highlightedParagraph = remember(paragraph, transcript) {
        buildHighlightedParagraph(paragraph, transcript)
    }
    val extraWordChips = remember(paragraph, transcript, extraWords) {
        extraWords.takeIf { it.isNotEmpty() } ?: deriveExtraWords(paragraph, transcript)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Highlighted Paragraph",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2C3E50)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HighlightLegendDot("Correct", Color(0xFF27AE60))
                    HighlightLegendDot("Missed", Color(0xFFEB5757))
                    HighlightLegendDot("Extra", Color(0xFFE28743))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = highlightedParagraph,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = Color(0xFF2C3E50)
            )
            if (extraWordChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Incorrect / extra words", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE28743))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extraWordChips.take(10).forEach { word ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFF2E8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = word, color = Color(0xFFE28743), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (missedWords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Missed words", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEB5757))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    missedWords.take(10).forEach { word ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFDEDEC), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = word, color = Color(0xFFC0392B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
private fun HighlightLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun StatItemCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
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

private fun normalizeWordToken(word: String): String =
    word.lowercase().trim('\'', '"', '`', ',', '.', '!', '?', ';', ':', '(', ')', '[', ']', '{', '}', '-')

private fun extractWords(text: String): List<String> =
    Regex("[A-Za-z']+").findAll(text).map { normalizeWordToken(it.value) }.filter { it.isNotBlank() }.toList()

private fun buildHighlightedParagraph(paragraph: String, transcript: String) = buildAnnotatedString {
    val transcriptCounts = extractWords(transcript).groupingBy { it }.eachCount().toMutableMap()
    val wordRegex = Regex("[A-Za-z']+")
    var lastIndex = 0

    wordRegex.findAll(paragraph).forEach { match ->
        append(paragraph.substring(lastIndex, match.range.first))
        val normalized = normalizeWordToken(match.value)
        val matched = (transcriptCounts[normalized] ?: 0) > 0
        if (matched) {
            transcriptCounts[normalized] = (transcriptCounts[normalized] ?: 0) - 1
        }
        withStyle(
            SpanStyle(
                color = if (matched) Color(0xFF27AE60) else Color(0xFFEB5757),
                fontWeight = FontWeight.Bold
            )
        ) {
            append(match.value)
        }
        lastIndex = match.range.last + 1
    }

    append(paragraph.substring(lastIndex))
}

private fun deriveExtraWords(paragraph: String, transcript: String): List<String> {
    val paragraphCounts = extractWords(paragraph).groupingBy { it }.eachCount().toMutableMap()
    val extraWords = mutableListOf<String>()

    extractWords(transcript).forEach { word ->
        val remaining = paragraphCounts[word] ?: 0
        if (remaining > 0) {
            paragraphCounts[word] = remaining - 1
        } else {
            extraWords.add(word)
        }
    }

    return extraWords.distinct()
}
