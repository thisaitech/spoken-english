package com.example.masterenglishfluency.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterenglishfluency.data.UserProgressRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.masterenglishfluency.data.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: UserProgressRepository = UserProgressRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    fun setSignUpMode(signUp: Boolean) {
        _isSignUpMode.value = signUp
        _uiState.value = LoginUiState.Idle
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }
        if (!email.contains("@")) {
            _uiState.value = LoginUiState.Error("Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _uiState.value = LoginUiState.Error("Password must be at least 6 characters")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
                repository.setLoginState(email, true)
                _uiState.value = LoginUiState.Success(email)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    "Firebase sign-in failed. Please check your network and try again."
                )
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }
        if (!email.contains("@")) {
            _uiState.value = LoginUiState.Error("Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _uiState.value = LoginUiState.Error("Password must be at least 6 characters")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
                repository.setLoginState(email, true)
                _uiState.value = LoginUiState.Success(email)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    "Firebase sign-up failed. Please check your network and try again."
                )
            }
        }
    }

    fun authenticateWithGoogle() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            _uiState.value = LoginUiState.Error("Google Auth is not configured on this device")
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    fun loginSucceeded(email: String) {
        viewModelScope.launch {
            repository.setLoginState(email, true)
            _uiState.value = LoginUiState.Success(email)
        }
    }
}
