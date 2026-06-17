package com.example.masterenglishfluency.ui.main

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Success(val data: List<String>) : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
}
