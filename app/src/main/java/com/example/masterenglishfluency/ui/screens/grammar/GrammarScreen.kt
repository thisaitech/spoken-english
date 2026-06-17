package com.example.masterenglishfluency.ui.screens.grammar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.example.masterenglishfluency.data.repository.GrammarQuizState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Grammar") })
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
                LessonsList(
                    lessons = state.lessons,
                    selectedLessonId = state.selectedLessonId,
                    onSelectLesson = viewModel::selectGrammarLesson
                )
            }
            if (state.selectedLessonId != null) {
                item {
                    QuizCard(
                        quizState = state.quizState,
                        questions = state.currentQuestions,
                        onAnswer = viewModel::answerQuestion,
                        onSubmit = viewModel::submitQuiz,
                        onReset = viewModel::resetQuiz
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonsList(
    lessons: List<GrammarLesson>,
    selectedLessonId: Long?,
    onSelectLesson: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Grammar Lessons",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        lessons.forEach { lesson ->
            LessonCard(
                lesson = lesson,
                selected = lesson.id == selectedLessonId,
                onClick = { onSelectLesson(lesson.id) }
            )
        }
    }
}

@Composable
private fun LessonCard(
    lesson: GrammarLesson,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = lesson.topic, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (lesson.isCompleted) {
                    Text("Completed", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = lesson.explanation, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClick) {
                Text(if (selected) "Quiz Open" else "Start Lesson")
            }
        }
    }
}

@Composable
private fun QuizCard(
    quizState: GrammarQuizState,
    questions: List<com.example.masterenglishfluency.data.model.GrammarQuestion>,
    onAnswer: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quiz: ${quizState.lessonTitle}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (questions.isEmpty()) {
                Text("No quiz questions are available for this lesson.")
            } else {
                questions.forEachIndexed { questionIndex, question ->
                    QuestionRow(
                        question = question,
                        questionIndex = questionIndex,
                        selectedOption = quizState.answers.getOrNull(questionIndex),
                        isSubmitted = quizState.isSubmitted,
                        onAnswer = onAnswer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            if (quizState.isSubmitted) {
                Text(
                    text = "Score: ${quizState.score} / ${quizState.totalQuestions}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSubmit,
                    enabled = !quizState.isSubmitted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit Quiz")
                }
                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close Quiz")
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(
    question: com.example.masterenglishfluency.data.model.GrammarQuestion,
    questionIndex: Int,
    selectedOption: Int?,
    isSubmitted: Boolean,
    onAnswer: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${questionIndex + 1}. ${question.question}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            question.options.forEachIndexed { optionIndex, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == optionIndex,
                        onClick = {
                            if (!isSubmitted) {
                                onAnswer(questionIndex, optionIndex)
                            }
                        }
                    )
                    Text(text = option, modifier = Modifier.weight(1f))
                    if (isSubmitted) {
                        val correct = optionIndex == question.correctOptionIndex
                        val chosenWrong = selectedOption == optionIndex && optionIndex != question.correctOptionIndex
                        Text(
                            text = when {
                                correct -> "Correct"
                                chosenWrong -> "Your answer"
                                else -> ""
                            },
                            color = if (correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
