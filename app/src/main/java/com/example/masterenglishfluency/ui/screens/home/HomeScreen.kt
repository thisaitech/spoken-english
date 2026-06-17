package com.example.masterenglishfluency.ui.screens.home

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.masterenglishfluency.AppViewModel
import com.example.masterenglishfluency.data.model.WordOfDay
import com.example.masterenglishfluency.ui.components.AppCard
import com.example.masterenglishfluency.ui.components.SectionTitle
import com.example.masterenglishfluency.ui.components.StatRow
import com.example.masterenglishfluency.ui.navigation.Screen
import com.example.masterenglishfluency.util.calculateDailyStreak
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val completedLessons = state.lessons.count { it.isCompleted }
    val quizCount = state.recentQuizAttempts.size
    val streak = calculateDailyStreak(state.lessons, state.recentQuizAttempts)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Master English Fluency") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WelcomeCard(profileName = state.settings.profileName)
            }
            item {
                DailyChallengeCard(
                    word = state.wordOfDay,
                    onMarkComplete = { viewModel.markWordCompleted(state.wordOfDay.id) },
                    onPronounce = { pronounce(context, state.wordOfDay.word) },
                    onVocabularyClick = { onNavigate(Screen.Vocabulary.route) }
                )
            }
            item {
                ProgressCard(
                    completedLessons = completedLessons,
                    quizCount = quizCount,
                    streak = streak,
                    onProgressClick = { onNavigate(Screen.Progress.route) }
                )
            }
            item {
                ContinueLearningCard(onNavigate)
            }
        }
    }
}

@Composable
private fun WelcomeCard(profileName: String) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Welcome back, $profileName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Complete one challenge today to keep your English streak alive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyChallengeCard(
    word: WordOfDay,
    onMarkComplete: () -> Unit,
    onPronounce: () -> Unit,
    onVocabularyClick: () -> Unit
) {
    AppCard(title = "Daily English Challenge") {
        Text(
            text = word.word,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = word.meaning, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Example: ${word.example}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPronounce) {
                Icon(imageVector = Icons.Default.Speaker, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Pronounce")
            }
            Button(onClick = onVocabularyClick) {
                Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Vocabulary")
            }
            Button(onClick = onMarkComplete) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Mark Complete")
            }
        }
    }
}

@Composable
private fun ProgressCard(
    completedLessons: Int,
    quizCount: Int,
    streak: Int,
    onProgressClick: () -> Unit
) {
    AppCard(title = "Progress") {
        StatRow(
            stats = listOf(
                "Lessons" to completedLessons.toString(),
                "Quizzes" to quizCount.toString(),
                "Streak" to "$streak days"
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onProgressClick, modifier = Modifier.fillMaxWidth()) {
            Text("View Full Progress")
        }
    }
}

@Composable
private fun ContinueLearningCard(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Continue Learning")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Speaking", Icons.Default.PlayArrow) { onNavigate(Screen.Speaking.route) }
            QuickAction("Grammar", Icons.Default.School) { onNavigate(Screen.Grammar.route) }
        }
    }
}

@Composable
private fun RowScope.QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.padding(4.dp))
        Text(label)
    }
}

private fun pronounce(context: Context, text: String) {
    lateinit var textToSpeech: TextToSpeech
    textToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US)
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Handler(Looper.getMainLooper()).postDelayed({ textToSpeech.shutdown() }, 3_000)
        }
    }
}
