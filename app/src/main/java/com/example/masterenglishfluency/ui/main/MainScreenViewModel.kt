package com.example.masterenglishfluency.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterenglishfluency.data.DataRepository
import kotlinx.coroutines.flow.*

class MainScreenViewModel(dataRepository: DataRepository) : ViewModel() {
    val uiState: StateFlow<MainScreenUiState> = dataRepository.getData()
        .map { MainScreenUiState.Success(it) as MainScreenUiState }
        .catch { emit(MainScreenUiState.Error(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainScreenUiState.Loading
        )
}
