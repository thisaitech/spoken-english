package com.example.masterenglishfluency.data

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.masterenglishfluency.data.local.AppDatabase
import com.example.masterenglishfluency.data.local.SpeakingSessionEntity
import com.example.masterenglishfluency.data.local.UserStatsEntity
import com.example.masterenglishfluency.data.local.VocabularyWordEntity
import com.example.masterenglishfluency.ui.dashboard.VocabWord
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserProgressRepository private constructor(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dataStore = context.dataStore
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var userListener: ListenerRegistration? = null
    private var sessionsListener: ListenerRegistration? = null
    private var pendingSyncJob: Job? = null
    private var realtimeSyncUid: String? = null

    val baseWords = listOf(
        VocabWord(1, "Eloquence", "noun", "Fluent or persuasive speaking or writing.", false),
        VocabWord(2, "Resilient", "adj", "Able to withstand or recover quickly from difficult conditions.", false),
        VocabWord(3, "Pragmatic", "adj", "Dealing with things sensibly and realistically in a practical way.", false),
        VocabWord(4, "Incentive", "noun", "A thing that motivates or encourages someone to do something.", false),
        VocabWord(5, "Mitigate", "verb", "Make less severe, serious, or painful.", false)
    )

    val premiumWords = listOf(
        VocabWord(6, "Acumen", "noun", "The ability to make good judgments and quick decisions, typically in a particular domain.", false),
        VocabWord(7, "Cognizant", "adj", "Having knowledge or being aware of.", false),
        VocabWord(8, "Elucidate", "verb", "Make something clear; explain.", false),
        VocabWord(9, "Fastidious", "adj", "Very attentive to and concerned about accuracy and detail.", false),
        VocabWord(10, "Gregarious", "adj", "Fond of company; sociable.", false),
        VocabWord(11, "Ineffable", "adj", "Too great or extreme to be expressed or described in words.", false),
        VocabWord(12, "Juxtapose", "verb", "Place or deal with close together for contrasting effect.", false),
        VocabWord(13, "Nefarious", "adj", "Wicked or criminal.", false),
        VocabWord(14, "Ostentatious", "adj", "Designed to impress or attract notice.", false),
        VocabWord(15, "Sycophant", "noun", "A person who acts obsequiously toward someone important in order to gain advantage.", false)
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (db.vocabularyDao().getCount() == 0) {
                    val entities = (baseWords + premiumWords).map {
                        VocabularyWordEntity(it.id, it.word, it.type, it.definition, it.isBookmarked)
                    }
                    db.vocabularyDao().insertWords(entities)
                }
                val existingStats = db.userStatsDao().getStatsOnce()
                if (existingStats == null) {
                    db.userStatsDao().saveStats(UserStatsEntity(id = 1, streakDays = 0, practiceSeconds = 0))
                }
            } catch (e: Exception) {
                Log.e("MEFRepository", "Database initialization failed", e)
                crashlytics.recordException(e)
            }
        }

        repoScope.launch {
            val uid = getUserId()
            if (uid != null) {
                syncFromFirestore()
            }
        }
    }

    val isPremiumUser: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_PREMIUM_KEY] ?: false
    }

    val selectedGoal: Flow<String> = dataStore.data.map { preferences ->
        preferences[SELECTED_GOAL_KEY] ?: "Casual Chat"
    }

    val userEmail: Flow<String> = dataStore.data.map { preferences ->
        preferences[EMAIL_KEY] ?: ""
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    val userName: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: "Alex Mercer"
    }

    val profilePicPath: Flow<String> = dataStore.data.map { preferences ->
        preferences[PROFILE_PIC_KEY] ?: ""
    }

    val streakDays: Flow<Int> = db.userStatsDao().getStats().map { it?.streakDays ?: 0 }

    val practiceSeconds: Flow<Int> = db.userStatsDao().getStats().map { it?.practiceSeconds ?: 0 }

    val speakingHistory: Flow<List<SpeakingSession>> = db.speakingSessionDao().getSessions().map { entities ->
        entities.map {
            SpeakingSession(
                it.id, it.topic, it.transcript,
                it.fluencyScore, it.pronunciationScore, it.grammarScore, it.vocabularyScore,
                it.durationSeconds, it.timestamp, it.aiResponse, it.audioPath
            )
        }
    }

    val vocabularyWords: Flow<List<VocabWord>> = db.vocabularyDao().getWords().map { entities ->
        entities.map { VocabWord(it.id, it.word, it.type, it.definition, it.isBookmarked) }
    }

    val totalPracticeCount: StateFlow<Int> = speakingHistory
        .map { it.size }
        .stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(5000), 0)

    fun getSpeakingHistoryFlow(): Flow<List<SpeakingSession>> = speakingHistory

    val wordOfDay: Flow<VocabWord?> = kotlinx.coroutines.flow.flow {
        val allWords = (baseWords + premiumWords)
        val todayIndex = (System.currentTimeMillis() / 86_400_000L).toInt() % allWords.size
        emit(allWords.getOrNull(todayIndex))
    }

    fun calculateStreakFromSessions(sessions: List<SpeakingSession>): Int {
        if (sessions.isEmpty()) return 0
        val dayMillis = 86_400_000L
        val today = System.currentTimeMillis() / dayMillis
        val sessionDays = sessions.map { it.timestamp / dayMillis }.distinct().sortedDescending()
        var streak = 0
        var expectedDay = today
        for (day in sessionDays) {
            when {
                day == expectedDay -> {
                    streak++
                    expectedDay--
                }
                day < expectedDay -> break
            }
        }
        return streak
    }

    fun getUniqueWordsSpoken(sessions: List<SpeakingSession>): Set<String> {
        val wordRegex = Regex("[a-zA-Z]+")
        return sessions.flatMap { session ->
            wordRegex.findAll(session.transcript).map { it.value.lowercase() }.toList()
        }.toSet()
    }

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun scheduleRealtimeSync(uid: String) {
        pendingSyncJob?.cancel()
        pendingSyncJob = repoScope.launch {
            delay(200)
            if (getUserId() == uid) {
                syncFromFirestore()
            }
        }
    }

    private fun startRealtimeSync(uid: String) {
        if (realtimeSyncUid == uid && userListener != null && sessionsListener != null) {
            return
        }
        stopRealtimeSync()
        realtimeSyncUid = uid

        var skipInitialUserEvent = true
        var skipInitialSessionEvent = true

        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { _, error ->
                if (error != null) {
                    Log.e("MEFRepository", "User doc listener failed", error)
                    return@addSnapshotListener
                }
                if (skipInitialUserEvent) {
                    skipInitialUserEvent = false
                    return@addSnapshotListener
                }
                scheduleRealtimeSync(uid)
            }

        sessionsListener = firestore.collection("users").document(uid)
            .collection("sessions")
            .addSnapshotListener { _, error ->
                if (error != null) {
                    Log.e("MEFRepository", "Session listener failed", error)
                    return@addSnapshotListener
                }
                if (skipInitialSessionEvent) {
                    skipInitialSessionEvent = false
                    return@addSnapshotListener
                }
                scheduleRealtimeSync(uid)
            }
    }

    private fun stopRealtimeSync() {
        pendingSyncJob?.cancel()
        pendingSyncJob = null
        userListener?.remove()
        userListener = null
        sessionsListener?.remove()
        sessionsListener = null
        realtimeSyncUid = null
    }

    suspend fun setPremiumUser(premium: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = premium
        }
        val bundle = Bundle().apply {
            putBoolean("is_premium", premium)
        }
        analytics.logEvent("set_premium_user", bundle)
        val uid = getUserId()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(uid)
                        .set(mapOf("isPremiumUser" to premium), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("MEFRepository", "Failed to upload premium status", e)
                }
            }
        }
    }

    suspend fun setGoal(goal: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_GOAL_KEY] = goal
        }
        val bundle = Bundle().apply {
            putString("goal", goal)
        }
        analytics.logEvent("select_learning_goal", bundle)
        val uid = getUserId()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(uid)
                        .set(mapOf("selectedGoal" to goal), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("MEFRepository", "Failed to upload goal", e)
                }
            }
        }
    }

    suspend fun setLoginState(email: String, loggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[EMAIL_KEY] = email
            preferences[IS_LOGGED_IN_KEY] = loggedIn
        }
        if (loggedIn) {
            val bundle = Bundle().apply {
                putString("email", email)
            }
            analytics.logEvent("login", bundle)
            val uid = getUserId()
            if (uid != null) {
                crashlytics.setUserId(uid)
                crashlytics.log("Logged in: $email")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        firestore.collection("users").document(uid)
                            .set(mapOf(
                                "email" to email,
                                "name" to "Alex Mercer",
                                "userId" to uid
                            ), SetOptions.merge())
                            .await()
                    } catch (e: Exception) {
                        Log.e("MEFRepository", "Failed to upload user profile to Firestore", e)
                    }
                    syncFromFirestore()
                }
            }
        } else {
            analytics.logEvent("user_logout", null)
            auth.signOut()
            crashlytics.log("Logged out user")
            stopRealtimeSync()
            CoroutineScope(Dispatchers.IO).launch {
                clearLocalData()
            }
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
        val uid = getUserId()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(uid)
                        .set(mapOf("name" to name), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("MEFRepository", "Failed to update user name", e)
                }
            }
        }
    }

    suspend fun setProfilePicPath(path: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_PIC_KEY] = path
        }
        val uid = getUserId()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(uid)
                        .set(mapOf("profilePicPath" to path), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("MEFRepository", "Failed to update profile pic path", e)
                }
            }
        }
    }

    suspend fun toggleBookmark(wordId: Int) {
        db.vocabularyDao().toggleBookmark(wordId)
        val bundle = Bundle().apply {
            putInt("word_id", wordId)
        }
        analytics.logEvent("vocab_bookmark_toggled", bundle)
        val uid = getUserId()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val bookmarks = db.vocabularyDao().getBookmarkedWordIds()
                    firestore.collection("users").document(uid)
                        .set(mapOf("bookmarkedWordIds" to bookmarks), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("MEFRepository", "Failed to upload bookmarks", e)
                }
            }
        }
    }

    suspend fun addSpeakingSession(session: SpeakingSession) {
        val entity = SpeakingSessionEntity(
            id = session.id, topic = session.topic, transcript = session.transcript,
            fluencyScore = session.fluencyScore, pronunciationScore = session.pronunciationScore,
            grammarScore = session.grammarScore, vocabularyScore = session.vocabularyScore,
            accuracyScore = session.accuracyScore,
            durationSeconds = session.durationSeconds, timestamp = session.timestamp,
            aiResponse = session.aiResponse, audioPath = session.audioPath
        )
        db.speakingSessionDao().insertSession(entity)

        val allSessions = db.speakingSessionDao().getSessionsOnce()
        val totalPracticeSeconds = allSessions.sumOf { it.durationSeconds }
        val calculatedStreak = calculateStreakFromSessions(
            allSessions.map { s -> SpeakingSession(s.id, s.topic, s.transcript, s.fluencyScore, s.pronunciationScore, s.grammarScore, s.vocabularyScore, s.durationSeconds, s.timestamp, s.aiResponse, s.audioPath) }
        )
        db.userStatsDao().saveStats(UserStatsEntity(id = 1, streakDays = calculatedStreak, practiceSeconds = totalPracticeSeconds))

        val bundle = Bundle().apply {
            putString("session_id", session.id)
            putInt("fluency", session.fluencyScore)
            putInt("pronunciation", session.pronunciationScore)
        }
        analytics.logEvent("speaking_session_added", bundle)

        val uid = getUserId()
        if (uid != null) {
            try {
                val sessionData = mapOf(
                    "id" to session.id,
                    "sessionType" to session.sessionType,
                    "topic" to session.topic,
                    "transcript" to session.transcript,
                    "overallScore" to session.overallScore,
                    "accuracyScore" to session.accuracyScore,
                    "fluencyScore" to session.fluencyScore,
                    "pronunciationScore" to session.pronunciationScore,
                    "grammarScore" to session.grammarScore,
                    "vocabularyScore" to session.vocabularyScore,
                    "wordsCorrect" to session.wordsCorrectCount,
                    "wordsMissed" to session.wordsMissedCount,
                    "extraWordsCount" to session.extraWordsCount,
                    "pauseCount" to session.pauseCount,
                    "paragraphText" to session.paragraphText,
                    "sentenceMistakes" to session.sentenceMistakesList,
                    "vocabularyFeedback" to session.vocabularyFeedbackText,
                    "overallResultMessage" to session.overallResultMessage,
                    "mispronouncedWords" to session.mispronouncedWordsList,
                    "missedWords" to session.missedWordsList,
                    "extraWordsList" to session.extraWordsList,
                    "strengths" to session.strengthsText,
                    "mistakes" to session.mistakesText,
                    "suggestedCorrections" to session.suggestedCorrectionsText,
                    "improvedVersion" to session.improvedParagraphText,
                    "durationSeconds" to session.durationSeconds,
                    "timestamp" to session.timestamp,
                    "aiResponse" to session.aiResponse,
                    "audioPath" to session.audioPath
                )
                firestore.collection("users").document(uid)
                    .collection("sessions").document(session.id)
                    .set(sessionData)
                    .await()

                firestore.collection("users").document(uid)
                    .set(
                        mapOf(
                            "streakDays" to calculatedStreak,
                            "practiceSeconds" to totalPracticeSeconds
                        ), SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                Log.e("MEFRepository", "Failed to sync speaking session to Firestore", e)
            }
        }
    }

    private suspend fun clearLocalData() {
        db.speakingSessionDao().clearAll()
        db.userStatsDao().clearAll()
        db.vocabularyDao().clearAllBookmarks()
    }

    suspend fun resetProgress() {
        clearLocalData()
        analytics.logEvent("reset_progress_event", null)
        val uid = getUserId()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(uid)
                        .set(mapOf(
                            "streakDays" to 0,
                            "practiceSeconds" to 0,
                            "bookmarkedWordIds" to emptyList<Int>(),
                            "sessionsCleared" to true
                        ), SetOptions.merge())
                        .await()

                    val sessions = firestore.collection("users").document(uid).collection("sessions").get().await()
                    for (doc in sessions.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    Log.e("MEFRepository", "Failed to reset progress in Firestore", e)
                }
            }
        }
    }

    suspend fun syncFromFirestore() {
        val uid = getUserId() ?: return
        Log.i("MEFSync", "Starting Firestore sync for user: $uid")
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val isPremium = doc.getBoolean("isPremiumUser") ?: false
                val goal = doc.getString("selectedGoal") ?: "Casual Chat"
                val streak = doc.getLong("streakDays")?.toInt() ?: 0
                val practice = doc.getLong("practiceSeconds")?.toInt() ?: 0
                val bookmarkedIds = doc.get("bookmarkedWordIds") as? List<*>
                val intBookmarkedIds = bookmarkedIds?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

                val name = doc.getString("name") ?: "Alex Mercer"
                val picPath = doc.getString("profilePicPath") ?: ""
                dataStore.edit { preferences ->
                    preferences[IS_PREMIUM_KEY] = isPremium
                    preferences[SELECTED_GOAL_KEY] = goal
                    preferences[USER_NAME_KEY] = name
                    preferences[PROFILE_PIC_KEY] = picPath
                }

                db.userStatsDao().saveStats(UserStatsEntity(id = 1, streakDays = streak, practiceSeconds = practice))

                db.vocabularyDao().clearAllBookmarks()
                if (intBookmarkedIds.isNotEmpty()) {
                    db.vocabularyDao().setBookmarks(intBookmarkedIds)
                }
            } else {
                val initialData = mapOf(
                    "isPremiumUser" to false,
                    "selectedGoal" to "Casual Chat",
                    "streakDays" to 0,
                    "practiceSeconds" to 0,
                    "bookmarkedWordIds" to emptyList<Int>()
                )
                firestore.collection("users").document(uid).set(initialData).await()
            }

            db.speakingSessionDao().clearAll()

            val querySnapshot = firestore.collection("users").document(uid)
                .collection("sessions").get().await()
            for (sessionDoc in querySnapshot.documents) {
                val sId = sessionDoc.id
                val topic = sessionDoc.getString("topic") ?: ""
                val transcript = sessionDoc.getString("transcript") ?: ""
                val fluency = sessionDoc.getLong("fluencyScore")?.toInt() ?: 0
                val pronunciation = sessionDoc.getLong("pronunciationScore")?.toInt() ?: 0
                val grammar = sessionDoc.getLong("grammarScore")?.toInt() ?: 0
                val vocab = sessionDoc.getLong("vocabularyScore")?.toInt() ?: 0
                val duration = sessionDoc.getLong("durationSeconds")?.toInt() ?: 0
                val ts = sessionDoc.getLong("timestamp") ?: System.currentTimeMillis()

                db.speakingSessionDao().insertSession(
                    SpeakingSessionEntity(
                        id = sId,
                        topic = topic,
                        transcript = transcript,
                        fluencyScore = fluency,
                        pronunciationScore = pronunciation,
                        grammarScore = grammar,
                        vocabularyScore = vocab,
                        durationSeconds = duration,
                        timestamp = ts,
                        aiResponse = sessionDoc.getString("aiResponse") ?: "",
                        audioPath = sessionDoc.getString("audioPath")
                    )
                )
            }
            startRealtimeSync(uid)
        } catch (e: Exception) {
            Log.e("MEFSync", "Sync failed", e)
            crashlytics.recordException(e)
        }
    }

    private var _selectedSpeakingTopicIndex = 0
    private var _selectedSpeakingCategory = "Core Modules"

    fun getSelectedSpeakingTopicIndex(): Int {
        return _selectedSpeakingTopicIndex
    }

    fun setSelectedSpeakingTopicIndex(index: Int) {
        _selectedSpeakingTopicIndex = index
    }

    fun getSelectedSpeakingCategory(): String {
        return _selectedSpeakingCategory
    }

    fun setSelectedSpeakingCategory(category: String) {
        _selectedSpeakingCategory = category
    }

    companion object {
        private val EMAIL_KEY = stringPreferencesKey("user_email")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium_user")
        private val SELECTED_GOAL_KEY = stringPreferencesKey("selected_goal")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val PROFILE_PIC_KEY = stringPreferencesKey("profile_pic_path")

        @Volatile
        private var INSTANCE: UserProgressRepository? = null

        fun initialize(context: Context): UserProgressRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserProgressRepository(context)
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): UserProgressRepository {
            return INSTANCE ?: throw IllegalStateException("UserProgressRepository not initialized. Call initialize(context) first.")
        }
    }
}

suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        Log.e("MEFRepository", "Task execution failed: ${exception.message}", exception)
        continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        Log.w("MEFRepository", "Task execution cancelled")
        continuation.resumeWithException(java.util.concurrent.CancellationException("Task was cancelled"))
    }
}
