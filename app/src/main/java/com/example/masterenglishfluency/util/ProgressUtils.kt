package com.example.masterenglishfluency.util

import com.example.masterenglishfluency.data.model.GrammarLesson
import com.example.masterenglishfluency.data.model.QuizAttempt

private const val MILLIS_PER_DAY = 86_400_000L

fun calculateDailyStreak(
    completedLessons: List<GrammarLesson>,
    quizAttempts: List<QuizAttempt>
): Int {
    val today = System.currentTimeMillis() / MILLIS_PER_DAY
    val activityDays = (completedLessons.map { it.updatedAt / MILLIS_PER_DAY } + quizAttempts.map { it.completedAt / MILLIS_PER_DAY })
        .distinct()
        .sortedDescending()

    var streak = 0
    var expectedDay = today
    for (day in activityDays) {
        when {
            day == expectedDay -> {
                streak++
                expectedDay--
            }
            day < expectedDay -> break
        }
    }
    return streak
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}m ${remainingSeconds.toString().padStart(2, '0')}s" else "${seconds}s"
}
