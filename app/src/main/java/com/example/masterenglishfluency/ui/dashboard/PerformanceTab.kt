package com.example.masterenglishfluency.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.masterenglishfluency.data.SpeakingSession
import com.example.masterenglishfluency.ui.chat.ChatViewModel
import com.example.masterenglishfluency.ui.components.AppIcons

@Composable
fun PerformanceTab(
    viewModel: DashboardViewModel,
    chatViewModel: ChatViewModel
) {
    val fluencyScore by viewModel.overallFluencyScore.collectAsState()
    val vocabScore by viewModel.vocabRangeScore.collectAsState()
    val pronunciationScore by viewModel.pronunciationScore.collectAsState()
    val grammarScore by viewModel.grammarScore.collectAsState()
    val speakingHistory by viewModel.speakingHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp)
    ) {
        Text(
            text = "Performance",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Fluency gauge card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overall Fluency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Gauge Drawing
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(150.dp)
                    ) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            drawCircle(
                                color = Color(0xFFF1F5F9),
                                style = Stroke(width = 12.dp.toPx())
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF2F80ED),
                                        Color(0xFF00C9FF),
                                        Color(0xFF2F80ED)
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = (fluencyScore.toFloat() / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(
                                    width = 12.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$fluencyScore%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2F80ED)
                            )
                            Text(
                                text = "Grade A2",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Under stats
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccuracyProgressBar(label = "Vocabulary Range", progressValue = vocabScore / 100f)
                        AccuracyProgressBar(label = "Pronunciation Accuracy", progressValue = pronunciationScore / 100f)
                        AccuracyProgressBar(label = "Grammatical Precision", progressValue = grammarScore / 100f)
                    }
                }
            }
            }

            // Tips section
            item {
                Text(
                    text = "Personalized Insights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }

            item {
                ExpandableTipCard(
                    title = "Enhance Vocabulary Range",
                    body = "You frequently use base words. Try introducing advanced synonyms like 'luminous' instead of 'bright', or 'fastidious' instead of 'careful' to express yourself more effectively."
                )
            }

            item {
                ExpandableTipCard(
                    title = "Improve Pronunciation Accuracy",
                    body = "Focus on the pronunciation of consonant clusters and silent letters. Speaking at a slightly slower pace will help maintain sound clarity."
                )
            }

            // Speaking History section
            item {
                Text(
                    text = "Speaking History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            }

            if (speakingHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No speaking sessions recorded yet. Start practicing from the Home screen!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    }
                }
            } else {
                items(speakingHistory) { session ->
                    HistoryRow(session = session)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        PerformanceComposer(chatViewModel = chatViewModel)
    }
}

@Composable
private fun PerformanceComposer(chatViewModel: ChatViewModel) {
    var inputText by remember { mutableStateOf("") }
    val isLoading by chatViewModel.isLoading.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ask the AI Assistant",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type a question or practice sentence...") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2F80ED),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        chatViewModel.sendMessage(inputText)
                        inputText = ""
                    }
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    chatViewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text("Send", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AccuracyProgressBar(label: String, progressValue: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Text(
                text = "${(progressValue * 100).toInt()}%",
                color = Color(0xFF2F80ED),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progressValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF2F80ED),
            trackColor = Color(0xFFF1F5F9)
        )
    }
}

@Composable
fun ExpandableTipCard(title: String, body: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Icon(
                    imageVector = if (isExpanded) AppIcons.INSTANCE.EyeOff else AppIcons.INSTANCE.Eye,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = body, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun HistoryRow(session: SpeakingSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.topic,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = session.sessionType,
                        fontSize = 11.sp,
                        color = Color(0xFF2F80ED),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${session.durationSeconds}s",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreIndicator(label = "Fluency", score = session.fluencyScore)
                ScoreIndicator(label = "Pron", score = session.pronunciationScore)
                ScoreIndicator(label = "Grammar", score = session.grammarScore)
                ScoreIndicator(label = "Vocab", score = session.vocabularyScore)
            }
        }
    }
}

@Composable
fun ScoreIndicator(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = "$score", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
    }
}
