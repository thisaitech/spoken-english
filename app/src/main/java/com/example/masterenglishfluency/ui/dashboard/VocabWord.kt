package com.example.masterenglishfluency.ui.dashboard

data class VocabWord(
    val id: Int,
    val word: String,
    val type: String,
    val definition: String,
    val isBookmarked: Boolean = false
)
