package com.example.masterenglishfluency.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val streakDays: Int,
    val practiceSeconds: Int
)
