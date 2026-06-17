package com.example.masterenglishfluency.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lesson_id") val lessonId: Long,
    @ColumnInfo(name = "lesson_title") val lessonTitle: String,
    val score: Int,
    @ColumnInfo(name = "total_questions") val totalQuestions: Int,
    @ColumnInfo(name = "completed_at") val completedAt: Long = System.currentTimeMillis()
)
