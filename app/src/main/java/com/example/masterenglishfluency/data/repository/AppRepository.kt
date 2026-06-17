package com.example.masterenglishfluency.data.repository

import com.example.masterenglishfluency.data.database.AppDatabase
import com.example.masterenglishfluency.data.model.AppSettings
import com.example.masterenglishfluency.data.model.GrammarLesson
import com.example.masterenglishfluency.data.model.GrammarQuestion
import com.example.masterenglishfluency.data.model.PracticeSession
import com.example.masterenglishfluency.data.model.QuizAttempt
import com.example.masterenglishfluency.data.model.WordOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrammarQuizState(
    val lessonId: Long = 0,
    val lessonTitle: String = "",
    val totalQuestions: Int = 0,
    val answers: List<Int?> = emptyList(),
    val isSubmitted: Boolean = false,
    val score: Int = 0
)

data class AppUiState(
    val wordOfDay: WordOfDay = SampleData.words.first(),
    val lessons: List<GrammarLesson> = emptyList(),
    val currentQuestions: List<GrammarQuestion> = emptyList(),
    val recentQuizAttempts: List<QuizAttempt> = emptyList(),
    val recentPracticeSessions: List<PracticeSession> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val selectedLessonId: Long? = null,
    val quizState: GrammarQuizState = GrammarQuizState(),
    val selectedPracticePromptIndex: Int = 0
) {
    companion object {
        fun initial(): AppUiState = AppUiState()
    }
}

class AppRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectedLessonId = MutableStateFlow<Long?>(null)
    private val selectedPracticePromptIndex = MutableStateFlow(0)
    private val currentQuestions = MutableStateFlow<List<GrammarQuestion>>(emptyList())
    private val quizState = MutableStateFlow(GrammarQuizState())

    val uiState: StateFlow<AppUiState> = combine(
        combine(
            database.wordDao().observeWordOfDay(SampleData.todayKey()),
            database.grammarLessonDao().observeLessons(),
            currentQuestions,
            database.quizAttemptDao().observeRecentAttempts(8),
            database.practiceSessionDao().observeRecentSessions(6)
        ) { wordOfDay, lessons, questions, attempts, sessions ->
            listOf<Any?>(wordOfDay, lessons, questions, attempts, sessions)
        },
        combine(
            settingsRepository.settingsFlow,
            selectedLessonId,
            quizState,
            selectedPracticePromptIndex
        ) { settings, lessonId, quizState, promptIndex ->
            listOf<Any?>(settings, lessonId, quizState, promptIndex)
        }
    ) { firstList, second ->
        @Suppress("UNCHECKED_CAST")
        AppUiState(
            wordOfDay = (firstList[0] as? WordOfDay) ?: SampleData.words.first(),
            lessons = firstList[1] as List<GrammarLesson>,
            currentQuestions = firstList[2] as List<GrammarQuestion>,
            recentQuizAttempts = firstList[3] as List<QuizAttempt>,
            recentPracticeSessions = firstList[4] as List<PracticeSession>,
            settings = second[0] as AppSettings,
            selectedLessonId = second[1] as Long?,
            quizState = second[2] as GrammarQuizState,
            selectedPracticePromptIndex = second[3] as Int
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), AppUiState.initial())

    fun initializeSampleData() {
        scope.launch {
            val wordDao = database.wordDao()
            val lessonDao = database.grammarLessonDao()
            val questionDao = database.grammarQuestionDao()

            if (wordDao.getWordOfDay(SampleData.todayKey()) == null) {
                wordDao.insertAll(SampleData.words)
            }

            if (lessonDao.observeLessons().first().isEmpty()) {
                lessonDao.insertAll(SampleData.lessons)
                SampleData.questions.forEach { (lessonId, questions) ->
                    questionDao.insertAll(questions)
                }
            }
        }
    }

    suspend fun markWordCompleted(id: Long) {
        database.wordDao().updateCompleted(id, true)
    }

    fun selectGrammarLesson(lessonId: Long) {
        scope.launch {
            val lesson = database.grammarLessonDao().getLessonById(lessonId) ?: return@launch
            val questions = database.grammarQuestionDao().getQuestionsForLesson(lessonId)
            selectedLessonId.value = lessonId
            currentQuestions.value = questions
            quizState.value = GrammarQuizState(
                lessonId = lessonId,
                lessonTitle = lesson.title,
                totalQuestions = questions.size,
                answers = List(questions.size) { null }
            )
        }
    }

    fun answerQuestion(questionIndex: Int, optionIndex: Int) {
        val current = quizState.value
        val answers = current.answers.toMutableList()
        if (answers.indices.contains(questionIndex)) {
            answers[questionIndex] = optionIndex
            quizState.value = current.copy(answers = answers, isSubmitted = false)
        }
    }

    fun submitQuiz() {
        scope.launch {
            val current = quizState.value
            if (current.lessonId == 0L) return@launch

            val questions = database.grammarQuestionDao().getQuestionsForLesson(current.lessonId)
            val score = questions.indices.count { index ->
                current.answers.getOrNull(index) == questions[index].correctOptionIndex
            }
            val attempt = QuizAttempt(
                lessonId = current.lessonId,
                lessonTitle = current.lessonTitle,
                score = score,
                totalQuestions = questions.size
            )
            database.quizAttemptDao().insert(attempt)

            if (questions.isNotEmpty() && score.toFloat() / questions.size >= 0.7f) {
                database.grammarLessonDao().updateCompleted(
                    id = current.lessonId,
                    isCompleted = true,
                    updatedAt = System.currentTimeMillis()
                )
            }

            quizState.value = current.copy(isSubmitted = true, score = score)
        }
    }

    fun resetQuiz() {
        selectedLessonId.value = null
        currentQuestions.value = emptyList()
        quizState.value = GrammarQuizState()
    }

    fun selectPracticePrompt(index: Int) {
        selectedPracticePromptIndex.value = index.coerceIn(0, SampleData.practicePrompts.lastIndex)
    }

    suspend fun addPracticeSession(prompt: String, durationSeconds: Int, filePath: String) {
        database.practiceSessionDao().insert(
            PracticeSession(
                prompt = prompt,
                durationSeconds = durationSeconds,
                filePath = filePath
            )
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
    }

    suspend fun setNotifications(enabled: Boolean) {
        settingsRepository.setNotifications(enabled)
    }

    suspend fun setProfileName(name: String) {
        settingsRepository.setProfileName(name)
    }
}
