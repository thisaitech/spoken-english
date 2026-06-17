package com.example.masterenglishfluency.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_words")
data class VocabularyWordEntity(
    @PrimaryKey val id: Int,
    val word: String,
    val type: String,
    val definition: String,
    val isBookmarked: Boolean
)
