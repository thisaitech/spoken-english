package com.example.masterenglishfluency.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterenglishfluency.data.SpeakingSession
import com.example.masterenglishfluency.data.UserProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MILLIS_PER_DAY = 86_400_000L

class DashboardViewModel(
    private val repository: UserProgressRepository = UserProgressRepository.getInstance()
) : ViewModel() {

    val isPremiumUser: StateFlow<Boolean> = repository.isPremiumUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName: StateFlow<String> = repository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex Mercer")

    val profilePicPath: StateFlow<String> = repository.profilePicPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedGoal: StateFlow<String> = repository.selectedGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Casual Chat")

    val vocabularyWords: StateFlow<List<VocabWord>> = repository.vocabularyWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speakingHistory: StateFlow<List<SpeakingSession>> = repository.speakingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPracticeCount: StateFlow<Int> = speakingHistory
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val practiceSeconds: StateFlow<Int> = speakingHistory
        .map { sessions -> sessions.sumOf { it.durationSeconds } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakDays: StateFlow<Int> = speakingHistory
        .map { sessions -> repository.calculateStreakFromSessions(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentStreak: StateFlow<Int> = streakDays

    val longestStreak: StateFlow<Int> = speakingHistory
        .map { sessions -> calculateLongestStreak(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageScore: StateFlow<Int> = speakingHistory
        .map { sessions ->
            if (sessions.isEmpty()) 0 else sessions.map { it.overallScore }.average().toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overallFluencyScore: StateFlow<Int> = speakingHistory
        .map { sessions ->
            if (sessions.isEmpty()) 0 else sessions.map { it.fluencyScore }.average().toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val vocabRangeScore: StateFlow<Int> = speakingHistory
        .map { sessions ->
            if (sessions.isEmpty()) 0 else sessions.map { it.vocabularyScore }.average().toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pronunciationScore: StateFlow<Int> = speakingHistory
        .map { sessions ->
            if (sessions.isEmpty()) 0 else sessions.map { it.pronunciationScore }.average().toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val grammarScore: StateFlow<Int> = speakingHistory
        .map { sessions ->
            if (sessions.isEmpty()) 0 else sessions.map { it.grammarScore }.average().toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val vocabularyGrowth: StateFlow<Int> = speakingHistory
        .map { sessions -> repository.getUniqueWordsSpoken(sessions).size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wordOfDay: StateFlow<com.example.masterenglishfluency.ui.dashboard.VocabWord?> = repository.wordOfDay
        .map { vocab ->
            vocab?.let { VocabWord(it.id, it.word, it.type, it.definition, it.isBookmarked) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectGoal(goal: String) {
        viewModelScope.launch {
            repository.setGoal(goal)
        }
    }

    fun toggleBookmark(wordId: Int) {
        viewModelScope.launch {
            repository.toggleBookmark(wordId)
        }
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            repository.setPremiumUser(true)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetProgress()
        }
    }

    fun selectSpeakingTopic(category: String, index: Int) {
        repository.setSelectedSpeakingCategory(category)
        repository.setSelectedSpeakingTopicIndex(index)
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.setUserName(name)
        }
    }

    fun updateProfilePic(path: String) {
        viewModelScope.launch {
            repository.setProfilePicPath(path)
        }
    }
}

fun calculateLongestStreak(sessions: List<SpeakingSession>): Int {
    if (sessions.isEmpty()) return 0
    val sessionDays = sessions.map { it.timestamp / MILLIS_PER_DAY }.distinct().sorted()
    var longest = 1
    var current = 1
    for (i in 1 until sessionDays.size) {
        current = if (sessionDays[i] == sessionDays[i - 1] + 1) current + 1 else 1
        if (current > longest) longest = current
    }
    return longest
}
