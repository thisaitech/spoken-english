package com.example.masterenglishfluency.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.masterenglishfluency.AppViewModel
import com.example.masterenglishfluency.data.model.GrammarLesson
import com.example.masterenglishfluency.data.model.PracticeSession
import com.example.masterenglishfluency.data.model.QuizAttempt
import com.example.masterenglishfluency.ui.components.AppCard
import com.example.masterenglishfluency.ui.components.StatPill
import com.example.masterenglishfluency.ui.components.StatRow
import com.example.masterenglishfluency.util.calculateDailyStreak
import com.example.masterenglishfluency.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    val completedLessons = state.lessons.filter { it.isCompleted }
    val streak = calculateDailyStreak(completedLessons, state.recentQuizAttempts)
    val lessonProgress = if (state.lessons.isEmpty()) 0f else completedLessons.size.toFloat() / state.lessons.size.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Progress") })
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
                ProgressOverviewCard(
                    completedLessons = completedLessons.size,
                    totalLessons = state.lessons.size,
                    quizCount = state.recentQuizAttempts.size,
                    streak = streak,
                    practiceMinutes = state.recentPracticeSessions.sumOf { it.durationSeconds } / 60
                )
            }
            item {
                LessonProgressCard(
                    progress = lessonProgress,
                    completedLessons = completedLessons
                )
            }
            item {
                QuizScoresCard(attempts = state.recentQuizAttempts)
            }
            item {
                PracticeSessionsCard(sessions = state.recentPracticeSessions)
            }
        }
    }
}

@Composable
private fun ProgressOverviewCard(
    completedLessons: Int,
    totalLessons: Int,
    quizCount: Int,
    streak: Int,
    practiceMinutes: Int
) {
    AppCard(title = "Overall Progress") {
        StatRow(
            stats = listOf(
                "Completed" to "$completedLessons/$totalLessons",
                "Quizzes" to quizCount.toString(),
                "Streak" to "$streak days",
                "Practice" to "${practiceMinutes}m"
            )
        )
    }
}

@Composable
private fun LessonProgressCard(
    progress: Float,
    completedLessons: List<GrammarLesson>
) {
    AppCard(title = "Completed Lessons") {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (completedLessons.isEmpty()) {
            Text("Complete grammar quizzes to build your lesson history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            completedLessons.forEach { lesson ->
                Text("• ${lesson.title}", modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun QuizScoresCard(attempts: List<QuizAttempt>) {
    AppCard(title = "Quiz Scores") {
        if (attempts.isEmpty()) {
            Text("Submit grammar quizzes to see your scores here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            attempts.forEach { attempt ->
                QuizScoreItem(attempt)
            }
        }
    }
}

@Composable
private fun QuizScoreItem(attempt: QuizAttempt) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = attempt.lessonTitle, fontWeight = FontWeight.Bold)
                Text(
                    text = "${attempt.score} correct out of ${attempt.totalQuestions}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${attempt.score}/${attempt.totalQuestions}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PracticeSessionsCard(sessions: List<PracticeSession>) {
    AppCard(title = "Saved Speaking Sessions") {
        if (sessions.isEmpty()) {
            Text("Record and save speaking practice to track your fluency routine.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sessions.forEach { session ->
                Text(
                    text = "• ${session.prompt} — ${formatDuration(session.durationSeconds)}",
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
