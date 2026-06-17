package com.example.masterenglishfluency

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterenglishfluency.data.repository.AppRepository
import com.example.masterenglishfluency.data.repository.AppUiState
import com.example.masterenglishfluency.data.repository.SampleData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class RecordingState(
    val isRecording: Boolean = false,
    val currentFilePath: String? = null,
    val elapsedSeconds: Int = 0,
    val message: String = ""
)

class AppViewModel(
    private val repository: AppRepository
) : ViewModel() {
    val uiState: StateFlow<AppUiState> = repository.uiState
    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordingStartedAt = 0L
    private var recordingTimerJob: Job? = null

    fun markWordCompleted(id: Long) {
        viewModelScope.launch { repository.markWordCompleted(id) }
    }

    fun selectGrammarLesson(lessonId: Long) {
        repository.selectGrammarLesson(lessonId)
    }

    fun answerQuestion(questionIndex: Int, optionIndex: Int) {
        repository.answerQuestion(questionIndex, optionIndex)
    }

    fun submitQuiz() {
        repository.submitQuiz()
    }

    fun resetQuiz() {
        repository.resetQuiz()
    }

    fun selectPracticePrompt(index: Int) {
        repository.selectPracticePrompt(index)
    }

    fun startRecording(context: Context) {
        val file = File(context.filesDir, "practice_${System.currentTimeMillis()}.m4a")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recordingStartedAt = SystemClock.elapsedRealtime()
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_recordingState.value.isRecording) {
                delay(1_000)
                _recordingState.value = _recordingState.value.copy(
                    elapsedSeconds = ((SystemClock.elapsedRealtime() - recordingStartedAt) / 1_000).toInt()
                )
            }
        }
        _recordingState.value = RecordingState(
            isRecording = true,
            currentFilePath = file.absolutePath,
            elapsedSeconds = 0
        )
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        mediaRecorder?.release()
        mediaRecorder = null
        recordingTimerJob?.cancel()
        val duration = ((SystemClock.elapsedRealtime() - recordingStartedAt) / 1_000).toInt().coerceAtLeast(1)
        val path = _recordingState.value.currentFilePath
        _recordingState.value = if (path != null) {
            RecordingState(isRecording = false, currentFilePath = path, elapsedSeconds = duration)
        } else {
            RecordingState(message = "Start a recording before stopping.")
        }
    }

    fun playRecording(context: Context) {
        val path = _recordingState.value.currentFilePath ?: return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            setOnCompletionListener {
                release()
                mediaPlayer = null
            }
            start()
        }
    }

    fun saveRecording(context: Context) {
        val state = _recordingState.value
        val path = state.currentFilePath ?: return
        val prompt = SampleData.practicePrompts[uiState.value.selectedPracticePromptIndex]
        viewModelScope.launch {
            repository.addPracticeSession(prompt, state.elapsedSeconds, path)
            _recordingState.value = state.copy(message = "Practice session saved.")
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setNotifications(enabled) }
    }

    fun setProfileName(name: String) {
        viewModelScope.launch { repository.setProfileName(name) }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
        recordingTimerJob?.cancel()
    }
}

class AppViewModelFactory(
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
