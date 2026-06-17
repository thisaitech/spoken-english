package com.example.masterenglishfluency.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speaking_sessions")
data class SpeakingSessionEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val transcript: String,
    val fluencyScore: Int,
    val pronunciationScore: Int,
    val grammarScore: Int,
    val vocabularyScore: Int,
    val accuracyScore: Int = 0,
    val durationSeconds: Int,
    val timestamp: Long,
    val aiResponse: String,
    val audioPath: String?
)
