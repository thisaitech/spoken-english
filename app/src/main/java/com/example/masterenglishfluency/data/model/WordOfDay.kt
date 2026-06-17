package com.example.masterenglishfluency.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordOfDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val meaning: String,
    val example: String,
    val pronunciation: String,
    @ColumnInfo(name = "date_key") val dateKey: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false
)
