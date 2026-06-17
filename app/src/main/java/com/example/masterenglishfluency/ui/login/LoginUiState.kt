package com.example.masterenglishfluency.ui.login

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val email: String) : LoginUiState
    data class Error(val message: String) : LoginUiState
}
