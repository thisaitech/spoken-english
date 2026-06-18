package com.example.masterenglishfluency.ui.speaking

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterenglishfluency.data.SpeakingSession
import com.example.masterenglishfluency.data.UserProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class SpeakingViewModel(
    private val repository: UserProgressRepository = UserProgressRepository.getInstance()
) : ViewModel() {

    data class AssistantMessage(
        val speaker: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _secondsElapsed = MutableStateFlow(0)
    val secondsElapsed: StateFlow<Int> = _secondsElapsed.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _showFeedback = MutableStateFlow(false)
    val showFeedback: StateFlow<Boolean> = _showFeedback.asStateFlow()

    private val _showSuccess = MutableStateFlow(false)
    val showSuccess: StateFlow<Boolean> = _showSuccess.asStateFlow()

    private val _showQuickSummary = MutableStateFlow(false)
    val showQuickSummary: StateFlow<Boolean> = _showQuickSummary.asStateFlow()

    private val _topicIndex = MutableStateFlow(0)
    val topicIndex: StateFlow<Int> = _topicIndex.asStateFlow()

    private val _currentSession = MutableStateFlow<SpeakingSession?>(null)
    val currentSession: StateFlow<SpeakingSession?> = _currentSession.asStateFlow()

    private val _currentCategory = MutableStateFlow("Core Modules")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

    private val _practiceLanguage = MutableStateFlow("English")
    val practiceLanguage: StateFlow<String> = _practiceLanguage.asStateFlow()

    private val _userRealSpeech = MutableStateFlow("")
    val userRealSpeech: StateFlow<String> = _userRealSpeech.asStateFlow()

    private val _isAssistantEnabled = MutableStateFlow(false)
    val isAssistantEnabled: StateFlow<Boolean> = _isAssistantEnabled.asStateFlow()

    private val _assistantStatus = MutableStateFlow("")
    val assistantStatus: StateFlow<String> = _assistantStatus.asStateFlow()

    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()

    private val _assistantReplyText = MutableStateFlow("")
    val assistantReplyText: StateFlow<String> = _assistantReplyText.asStateFlow()

    private val _isAssistantListening = MutableStateFlow(false)

    val isSessionValid: StateFlow<Boolean> = _currentSession
        .map { it != null && it.transcript.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // AtomicBoolean guarantees that exactly one caller (SpeechRecognizer callback
    // OR the 1500 ms fallback) wins the compareAndSet race and runs completeAnalysis().
    private val isAnalyzing = AtomicBoolean(false)

    val paragraphsMap = mapOf(
        "Casual Chat" to listOf(
            "Engaging in hobbies is vital for a balanced life. Recreation provides an avenue to escape daily stressors and explore new passions. Over time, a casual pastime can evolve into a lifelong dedication, enriching our personal experiences and bringing immense joy.",
            "This weekend, I plan to embark on a short trip to the countryside to unwind. I will visit a historical site and hike along the nature trails. If the weather remains pleasant, I hope to capture some scenic photographs before returning home on Sunday evening."
        ),
        "Job Interview" to listOf(
            "Over the past few years, I have dedicated myself to mastering software engineering. I have successfully designed, built, and deployed several interactive mobile applications. I thrive in collaborative team environments where we can solve complex challenges and deliver high-quality results.",
            "When faced with a difficult challenge at work, I focus on analyzing the root cause. I systematically break down the problem into smaller tasks, collaborate with my peers, and implement a structured solution. This methodical approach ensures we achieve our targets efficiently."
        ),
        "Public Speaking" to listOf(
            "Protecting our planet is a collective responsibility that we can no longer ignore. By making minor changes in our daily routines—like reducing waste and conserving energy—we can mitigate environmental damage. Let us take action today to secure a cleaner, healthier future for the next generation.",
            "Good morning, everyone. It is a pleasure to welcome you to this year's annual conference. Today, we will explore the fascinating future of artificial intelligence in education. To start, let me share a surprising statistic that might change the way you view digital classrooms."
        ),
        "Academic" to listOf(
            "The rapid proliferation of social media platforms has fundamentally altered modern communication styles. While it fosters global connectivity, it can also lead to superficial interactions and screen fatigue. Mitigating these negative effects requires us to balance virtual messaging with face-to-face dialogue.",
            "Online learning offers unparalleled flexibility and access to diverse resources, whereas traditional classrooms provide invaluable personal interaction and hands-on guidance. Conversely, virtual study demands high self-discipline, while structured schools foster organic collaboration among peers."
        ),
        "Core Modules" to listOf(
            "Hello! My name is Alex, and I am from a vibrant city known for its history. My main objective is to enhance my English communication skills so I can connect with people globally. I am excited to meet everyone and embark on this learning journey together.",
            "I would like to order a fresh garden salad and a grilled chicken sandwich. For my drink, I prefer a cup of iced coffee with a splash of milk. Please make sure the sandwich is served warm, and let me know if there are any seasonal specials available today.",
            "Excuse me, could you please tell me how to get to the nearest subway station? I seem to have lost my way. Do I need to walk straight past the intersection, or should I take a right turn at the signal? Any guidance would be greatly appreciated.",
            "In my opinion, maintaining a healthy work-life balance is essential for long-term productivity and happiness. Consequently, companies should encourage flexible working hours. On the other hand, employees must also learn to disconnect from work devices during their personal time.",
            "I am highly motivated to join your organization because of your innovative projects and supportive culture. My primary strength is adaptability, which helps me learn new tools quickly. While my public speaking skills are developing, I am actively practicing to speak more confidently.",
            "Hi team, I wanted to share a quick update on our project status. We have completed the core features and successfully resolved the critical database bugs. Next, we will run user testing to prepare for the final deployment. Please share your feedback by Friday afternoon.",
            "Could you let me know the price of this handmade wool sweater? It looks very comfortable, but it is a bit above my budget. I was wondering if you could offer a small discount, or if there is a store sale coming up that I should wait for.",
            "It is so wonderful to meet you at this event. I noticed you were also interested in photography, which is one of my favorite hobbies. We should plan to visit the local art gallery together sometime next week if you are free. Here is my contact info.",
            "Please help! There has been a minor accident on the main road, and someone appears to be injured. We need an ambulance immediately. The location is near the central park entrance. I will stay with the person and follow your instructions until help arrives."
        )
    )

    val topicsMap = mapOf(
        "Casual Chat" to listOf(
            "Discussing Hobbies: Reading Challenge",
            "Weekend Plans: Reading Challenge"
        ),
        "Job Interview" to listOf(
            "Tell Me About Yourself: Reading Challenge",
            "Handling Challenges: Reading Challenge"
        ),
        "Public Speaking" to listOf(
            "Persuasive Pitch: Reading Challenge",
            "Welcoming an Audience: Reading Challenge"
        ),
        "Academic" to listOf(
            "Analyzing a Trend: Reading Challenge",
            "Comparing Viewpoints: Reading Challenge"
        ),
        "Core Modules" to listOf(
            "Introductory Conversation: Reading Challenge",
            "Ordering Food: Reading Challenge",
            "Asking for Directions: Reading Challenge",
            "Sharing Opinions: Reading Challenge",
            "Job Interview Preparation: Reading Challenge",
            "Professional Networking: Reading Challenge",
            "Shopping & Bargaining: Reading Challenge",
            "Socializing & Making Friends: Reading Challenge",
            "Emergency Situations: Reading Challenge"
        )
    )

    private val _topics = MutableStateFlow<List<String>>(topicsMap["Core Modules"]!!)
    val topics: StateFlow<List<String>> = _topics.asStateFlow()

    private val _currentParagraph = MutableStateFlow("")
    val currentParagraph: StateFlow<String> = _currentParagraph.asStateFlow()

    init {
        val cat = repository.getSelectedSpeakingCategory()
        _currentCategory.value = cat
        val loadedTopics = topicsMap[cat] ?: topicsMap["Core Modules"]!!
        _topics.value = loadedTopics
        _topicIndex.value = repository.getSelectedSpeakingTopicIndex().coerceIn(0, loadedTopics.size - 1)
        updateCurrentParagraph()
    }

    fun updateCurrentParagraph() {
        val cat = _currentCategory.value
        val index = _topicIndex.value
        val paragraphs = paragraphsMap[cat] ?: paragraphsMap["Core Modules"]!!
        _currentParagraph.value = paragraphs.getOrNull(index) ?: paragraphs.first()
    }

    fun nextTopic() {
        val currentTopics = _topics.value
        if (currentTopics.isNotEmpty()) {
            _topicIndex.value = (_topicIndex.value + 1) % currentTopics.size
            repository.setSelectedSpeakingTopicIndex(_topicIndex.value)
            updateCurrentParagraph()
            _userRealSpeech.value = ""
        }
    }

    fun setPracticeLanguage(language: String) {
        _practiceLanguage.value = language
    }

    fun startListening(context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}

                            override fun onError(error: Int) {
                                Log.e("SpeakingViewModel", "SpeechRecognizer Error: $error")
                                if (_isAssistantListening.value) {
                                    _assistantStatus.value = "Voice assistant could not hear you. Try again."
                                    _isAssistantListening.value = false
                                    return
                                }
                                if (_isProcessing.value) completeAnalysis()
                            }

                            override fun onResults(results: Bundle?) {
                                val text = results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull() ?: ""
                                if (text.isBlank()) return

                                if (_isAssistantListening.value) {
                                    handleAssistantTranscript(context, text)
                                    return
                                }

                                if (text.isNotBlank()) _userRealSpeech.value = text
                                if (_isProcessing.value) completeAnalysis()
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                if (_isAssistantListening.value) return
                                val text = partialResults
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull() ?: ""
                                if (text.isNotBlank()) _userRealSpeech.value = text
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                }

                _userRealSpeech.value = ""

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        if (_practiceLanguage.value == "Tamil") "ta-IN" else "en-US"
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeakingViewModel", "Failed to start speech recognizer", e)
                if (_isAssistantListening.value) {
                    _assistantStatus.value = "Speech recognition is not available."
                    _isAssistantListening.value = false
                }
            }
        }
    }

    fun startSpeaking(context: Context) {
        _isSpeaking.value = true
        _secondsElapsed.value = 0
        _userRealSpeech.value = ""
        _isAssistantListening.value = false
        isAnalyzing.set(false)

        startListening(context)

        try {
            val file = File(context.filesDir, "speaking_${System.currentTimeMillis()}.m4a")
            recordingFile = file
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("SpeakingViewModel", "Start recording failed", e)
        }
    }

    fun tickTimer() {
        _secondsElapsed.value = _secondsElapsed.value + 1
    }

    fun stopSpeakingAndProcess() {
        _isSpeaking.value = false
        _isProcessing.value = true
        _isAssistantListening.value = false

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("SpeakingViewModel", "Stop recording failed", e)
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("SpeakingViewModel", "Stop listening failed", e)
            }
        }

        // Fallback: if SpeechRecognizer callback hasn't fired within 1500 ms, trigger analysis.
        // AtomicBoolean ensures only one of (callback, fallback) ever enters completeAnalysis().
        viewModelScope.launch {
            delay(1500L)
            if (_isProcessing.value) completeAnalysis()
        }
    }

    // ── Offline evaluation ────────────────────────────────────────────────────

    fun performOfflineEvaluation(original: String, spoken: String): String {
        // ── word diff ────────────────────────────────────────────────────────
        val diff         = computeWordDiff(original, spoken)
        val accuracyScore  = diff["accuracyScore"] as Int
        val wordsCorrect   = diff["wordsCorrect"]  as Int
        val wordsMissed    = diff["wordsMissed"]   as Int
        @Suppress("UNCHECKED_CAST")
        val missedWords    = diff["missedWords"]   as List<String>
        @Suppress("UNCHECKED_CAST")
        val extraWords     = diff["extraWords"]    as List<String>
        @Suppress("UNCHECKED_CAST")
        val incorrectWords = diff["incorrectWords"] as List<String>

        // ── pause detection ──────────────────────────────────────────────────
        val pauseCount = detectPauses(spoken)

        // ── derived scores ───────────────────────────────────────────────────
        val spokenSet      = cleanRegex.replace(spoken, "").lowercase()
            .split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val skippedSentences = computeSkippedSentences(original, spokenSet)
        val sentenceMistakes = buildSentenceMistakes(original, skippedSentences)
        val paragraphContentWords = extractContentWords(original)
        val vocabularyMatchedCount = computeParagraphVocabSpoken(original, spokenSet).size
        val vocabularyCoverage = if (paragraphContentWords.isNotEmpty()) {
            (vocabularyMatchedCount.toFloat() / paragraphContentWords.size.toFloat() * 100f).toInt()
        } else 0
        val fluencyPenalty = (pauseCount * 3 + skippedSentences.size * 10).coerceAtMost(40)
        val fluencyScore   = if (wordsCorrect > 0) (95 - fluencyPenalty).coerceIn(0, 100) else 0
        val pronunciationScore = if (wordsCorrect > 0) ((accuracyScore * 2 + fluencyScore) / 3).coerceIn(0, 100) else 0
        val vocabularyScore    = if (wordsCorrect > 0) (accuracyScore + 10).coerceIn(0, 100) else 0
        val grammarScore       = if (wordsCorrect > 0) (accuracyScore + 5 - incorrectWords.size * 2).coerceIn(0, 100) else 0
        val overallScore       = (accuracyScore + fluencyScore + pronunciationScore + vocabularyScore + grammarScore) / 5

        // ── feedback text ────────────────────────────────────────────────────
        val mispronounced    = (missedWords + incorrectWords).distinct().take(5)
        val paragraphVocab   = computeParagraphVocabSpoken(original, spokenSet)

        val strengths = when {
            accuracyScore >= 90 -> "Excellent reading accuracy. Nearly every word was pronounced correctly."
            accuracyScore >= 75 -> "Strong reading accuracy with a steady, natural pace. Most words were pronounced correctly."
            else                -> "Good attempt. Focus on reading each word clearly before moving to the next."
        }

        val vocabularyFeedback = when {
            vocabularyCoverage >= 85 -> "Strong vocabulary coverage. You included most of the paragraph's key words."
            vocabularyCoverage >= 60 -> "Good vocabulary coverage. Review the missing key words and repeat the paragraph slowly."
            vocabularyCoverage >= 35 -> "Partial vocabulary coverage. Focus on the important words highlighted in the paragraph."
            else -> "Low vocabulary coverage. Re-read the paragraph, then practice one sentence at a time."
        }

        val overallResultMessage = when {
            overallScore >= 90 -> "Excellent reading assessment. Your pronunciation, fluency, and accuracy are ready for the next challenge."
            overallScore >= 75 -> "Good reading performance. Keep improving sentence completeness and smooth pacing."
            overallScore >= 60 -> "Fair reading performance. Review the sentence mistakes and vocabulary gaps before trying again."
            else -> "Needs more practice. Read the paragraph in short sections and record again."
        }

        val mistakes = buildString {
            if (wordsMissed > 0) append("Missed $wordsMissed word(s): ${missedWords.take(5).joinToString(", ")}.")
            else append("No omissions detected.")
            if (incorrectWords.isNotEmpty()) append(" Substituted: ${incorrectWords.take(3).joinToString(", ")}.")
            if (extraWords.isNotEmpty()) append(" Extra words added: ${extraWords.take(3).joinToString(", ")}.")
            if (pauseCount > 0) append(" Detected $pauseCount filler/pause(s).")
            if (skippedSentences.isNotEmpty()) append(" ${skippedSentences.size} sentence(s) largely skipped.")
        }

        val corrections = if (mispronounced.isNotEmpty())
            "Practice these words slowly: ${mispronounced.joinToString(", ") { "'$it'" }}. Record yourself and compare."
        else "Great accuracy! Now focus on smoother pacing and natural intonation."

        val aiFeedbackTips = buildString {
            if (pauseCount >= 3) append("Reduce filler words (uh, um, er). Pause silently instead. ")
            if (skippedSentences.isNotEmpty()) append("Re-read skipped sentences aloud 3 times each. ")
            if (accuracyScore < 75) append("Slow down and read word-by-word to improve accuracy. ")
            if (extraWords.isNotEmpty()) append("Avoid inserting words not in the original text. ")
            if (this.isEmpty()) append("Great performance! Maintain your pace and work on natural stress patterns.")
        }.trim()

        return JSONObject().apply {
            put("sessionType",         "Reading Assessment")
            put("overallScore",        overallScore)
            put("pronunciationScore",  pronunciationScore)
            put("fluencyScore",        fluencyScore)
            put("accuracyScore",       accuracyScore)
            put("vocabularyScore",     vocabularyScore)
            put("grammarScore",        grammarScore)
            put("wordsCorrect",        wordsCorrect)
            put("wordsMissed",         wordsMissed)
            put("pauseCount",          pauseCount)
            put("extraWords",          extraWords.size)
            put("mispronouncedWords",  org.json.JSONArray(mispronounced))
            put("missedWords",         org.json.JSONArray(missedWords))
            put("extraWordsList",      org.json.JSONArray(extraWords))
            put("skippedSentences",    org.json.JSONArray(skippedSentences))
            put("sentenceMistakes",    org.json.JSONArray(sentenceMistakes))
            put("vocabularyFeedback",  vocabularyFeedback)
            put("overallResultMessage",overallResultMessage)
            put("strengths",           strengths)
            put("mistakes",            mistakes)
            put("suggestedCorrections",corrections)
            put("improvedVersion",     original)
            put("paragraph",           original)
        }.toString()
    }

    // ── Analysis helpers ──────────────────────────────────────────────────────

    private val stopWords = setOf(
        "i", "a", "an", "the", "is", "it", "to", "of", "and", "in",
        "my", "me", "we", "you", "he", "she", "they", "am", "are",
        "was", "be", "at", "on", "or", "do", "so", "if", "as",
        "by", "up", "us", "no", "go"
    )

    private val cleanRegex = Regex("[^a-zA-Z0-9\\s]")

    // ── Word-level diff ───────────────────────────────────────────────────────

    enum class WordLabel { CORRECT, MISSED, INCORRECT, EXTRA }
    data class WordTag(val word: String, val label: WordLabel)

    /**
     * Aligns [originalText] against [spokenText] token-by-token.
     *
     * Returns a map with:
     *   "accuracyScore"    Int     0–100
     *   "missedWords"      List<String>
     *   "extraWords"       List<String>
     *   "highlightedWords" List<WordTag>  (original words tagged CORRECT/MISSED/INCORRECT
     *                                      + extra spoken words tagged EXTRA)
     */
    fun computeWordDiff(originalText: String, spokenText: String): Map<String, Any> {
        val origTokens = cleanRegex.replace(originalText, "").lowercase()
            .split(Regex("\\s+")).filter { it.isNotBlank() }
        val spokenTokens = cleanRegex.replace(spokenText, "").lowercase()
            .split(Regex("\\s+")).filter { it.isNotBlank() }

        // Build LCS table so we can trace which original words were spoken in order.
        val n = origTokens.size
        val m = spokenTokens.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) for (j in 1..m) {
            lcs[i][j] = if (origTokens[i - 1] == spokenTokens[j - 1]) lcs[i - 1][j - 1] + 1
                        else maxOf(lcs[i - 1][j], lcs[i][j - 1])
        }

        // Trace back to find matched (orig-index, spoken-index) pairs.
        val matchedOrigIdx = mutableSetOf<Int>()
        val matchedSpokenIdx = mutableSetOf<Int>()
        var i = n; var j = m
        while (i > 0 && j > 0) {
            when {
                origTokens[i - 1] == spokenTokens[j - 1] -> {
                    matchedOrigIdx.add(i - 1); matchedSpokenIdx.add(j - 1); i--; j--
                }
                lcs[i - 1][j] >= lcs[i][j - 1] -> i--
                else -> j--
            }
        }

        // Tag every original token.
        val highlighted = origTokens.mapIndexed { idx, word ->
            when {
                idx in matchedOrigIdx -> WordTag(word, WordLabel.CORRECT)
                spokenTokens.contains(word) -> WordTag(word, WordLabel.INCORRECT) // present but out-of-order / substituted context
                else -> WordTag(word, WordLabel.MISSED)
            }
        }.toMutableList()

        // Spoken tokens not matched to any original word → EXTRA.
        val extraWords = spokenTokens.filterIndexed { idx, _ -> idx !in matchedSpokenIdx }
            .filter { it !in origTokens }           // skip out-of-order originals already tagged INCORRECT
            .distinct()
        extraWords.forEach { highlighted.add(WordTag(it, WordLabel.EXTRA)) }

        val missedWords = highlighted.filter { it.label == WordLabel.MISSED }.map { it.word }
        val incorrectWords = highlighted.filter { it.label == WordLabel.INCORRECT }.map { it.word }
        val correctCount = matchedOrigIdx.size
        val accuracyScore = ((correctCount.toFloat() / n.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)

        return mapOf(
            "accuracyScore"    to accuracyScore,
            "missedWords"      to missedWords,
            "extraWords"       to extraWords,
            "incorrectWords"   to incorrectWords,
            "highlightedWords" to highlighted,
            "wordsCorrect"     to correctCount,
            "wordsMissed"      to missedWords.size
        )
    }

    // ── Pause / hesitation detection ──────────────────────────────────────────

    /** Counts filler tokens in [transcript]: uh, um, er, hmm, like, you know. */
    fun detectPauses(transcript: String): Int {
        val fillers = setOf("uh", "um", "er", "hmm", "like", "you know")
        val lower = transcript.lowercase()
        // "you know" is a bigram — count it first, then count single-word fillers.
        val bigram = Regex("\\byou know\\b").findAll(lower).count()
        val tokens = cleanRegex.replace(lower, "").split(Regex("\\s+"))
        val single = tokens.count { it in fillers - "you know" }
        return bigram + single
    }

    private fun extractContentWords(text: String): List<String> {
        return cleanRegex.replace(text, "")
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
    }

    private fun buildSentenceMistakes(paragraph: String, skippedSentences: List<String>): List<String> {
        if (skippedSentences.isEmpty()) return emptyList()
        return skippedSentences.map { sentence ->
            val contentWords = extractContentWords(sentence)
            val missing = contentWords.take(6).joinToString(" ")
            "Missed or incomplete sentence: \"$sentence\". Practice these key words: $missing."
        }
    }

    /**
     * Returns the list of sentences from [paragraph] that are considered "skipped" —
     * i.e. fewer than 30 % of their meaningful content words appear in [spokenSet].
     */
    fun computeSkippedSentences(paragraph: String, spokenSet: Set<String>): List<String> {
        val sentences = paragraph.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        return sentences.filter { sentence ->
            val contentWords = cleanRegex.replace(sentence, "")
                .lowercase().split(Regex("\\s+"))
                .filter { it.isNotBlank() && it !in stopWords }
            if (contentWords.isEmpty()) return@filter false
            val matched = contentWords.count { it in spokenSet }
            (matched.toFloat() / contentWords.size) < 0.30f
        }
    }

    /**
     * Returns unique content words from the original [paragraph] that the user
     * actually spoke (intersection of paragraph vocab and spoken words),
     * excluding stop words and single-character tokens.
     */
    fun computeParagraphVocabSpoken(paragraph: String, spokenSet: Set<String>): List<String> {
        val paragraphWords = cleanRegex.replace(paragraph, "")
            .lowercase().split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
        return paragraphWords.filter { it in spokenSet }.sorted()
    }

    private fun normalizeReadingEvaluationJson(
        originalText: String,
        userSpeechText: String,
        evaluationJson: String
    ): String {
        val fallback = JSONObject(performOfflineEvaluation(originalText, userSpeechText))
        val merged = try {
            JSONObject(evaluationJson)
        } catch (e: Exception) {
            Log.w("SpeakingViewModel", "Invalid reading evaluation JSON, using offline evaluation", e)
            return fallback.toString()
        }

        listOf(
            "sessionType",
            "overallScore",
            "pronunciationScore",
            "fluencyScore",
            "accuracyScore",
            "vocabularyScore",
            "grammarScore",
            "wordsCorrect",
            "wordsMissed",
            "pauseCount",
            "extraWords",
            "mispronouncedWords",
            "missedWords",
            "extraWordsList",
            "skippedSentences",
            "sentenceMistakes",
            "vocabularyFeedback",
            "overallResultMessage",
            "strengths",
            "mistakes",
            "suggestedCorrections",
            "improvedVersion",
            "paragraph"
        ).forEach { key ->
            if (!merged.has(key) || merged.isNull(key)) {
                if (fallback.has(key)) merged.put(key, fallback.get(key))
            }
        }
        if (!merged.has("paragraph") || merged.isNull("paragraph")) merged.put("paragraph", originalText)
        if (!merged.has("sessionType") || merged.isNull("sessionType")) merged.put("sessionType", "Reading Assessment")
        return merged.toString()
    }

    fun enableAssistant(context: Context) {
        _isAssistantEnabled.value = true
        speakFeedback(context)
    }

    fun disableAssistant() {
        _isAssistantEnabled.value = false
        _isAssistantListening.value = false
        _assistantStatus.value = "AI Voice Assistant is off."
    }

    fun speakFeedback(context: Context) {
        val session = _currentSession.value ?: return
        val text = buildAssistantFeedbackText(session)
        _assistantReplyText.value = text
        _assistantStatus.value = text
        addAssistantMessage("AI", text)
    }

    fun startAssistantListening(context: Context) {
        if (!_isAssistantEnabled.value) {
            _isAssistantEnabled.value = true
        }
        _isAssistantListening.value = true
        _assistantStatus.value = "Listening to your question..."
        if (speechRecognizer == null) {
            speechRecognizer = createSpeechRecognizer(context)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopAssistantListening() {
        _isAssistantListening.value = false
        _assistantStatus.value = "Voice assistant stopped."
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w("SpeakingViewModel", "Failed to stop assistant listening", e)
        }
    }

    private fun handleAssistantTranscript(context: Context, userText: String) {
        _isAssistantListening.value = false
        val trimmed = userText.trim()
        if (trimmed.isBlank()) {
            _assistantStatus.value = "I did not catch that. Please try again."
            return
        }
        addAssistantMessage("You", trimmed)
        _assistantStatus.value = "Thinking..."
        val reply = buildAssistantReply(trimmed)
        _assistantStatus.value = "Speaking: $reply"
        _assistantReplyText.value = reply
        addAssistantMessage("AI", reply)
    }

    private fun addAssistantMessage(speaker: String, text: String) {
        _assistantMessages.value = (_assistantMessages.value + AssistantMessage(speaker, text))
            .takeLast(20)
    }

    private fun buildAssistantFeedbackText(session: SpeakingSession): String {
        val sentencePart = if (session.sentenceMistakesList.isEmpty()) {
            "No sentence mistakes were detected."
        } else {
            "Sentence mistakes: ${session.sentenceMistakesList.take(2).joinToString(" ") }"
        }
        return "Your reading score is ${session.overallScore} out of 100. Pronunciation is ${session.pronunciationScore}, fluency is ${session.fluencyScore}, and vocabulary feedback is ${session.vocabularyFeedbackText.ifBlank { "review the highlighted key words" }}. $sentencePart ${session.overallResultMessage.ifBlank { "Keep practicing one sentence at a time." }}"
    }

    private fun buildAssistantReply(userText: String): String {
        val lower = userText.lowercase()
        val session = _currentSession.value
        if (session == null) return "Please finish the reading analysis first, then ask me a question."
        return when {
            lower.contains("sentence") || lower.contains("mistake") || lower.contains("error") -> {
                if (session.sentenceMistakesList.isEmpty()) "No sentence mistakes were detected. Your sentence coverage was good."
                else "The main sentence mistakes are: ${session.sentenceMistakesList.take(2).joinToString(" ")}"
            }
            lower.contains("pronunciation") || lower.contains("pronounce") -> {
                if (session.mispronouncedWordsList.isEmpty()) "Your pronunciation was strong. Keep reading at a steady pace."
                else "Work on these words: ${session.mispronouncedWordsList.take(4).joinToString(", ")}. Say them slowly, then repeat the sentence."
            }
            lower.contains("vocab") || lower.contains("word") -> {
                session.vocabularyFeedbackText.ifBlank { "You matched ${session.wordsCorrectCount} words. Focus on the missed words shown in the paragraph." }
            }
            lower.contains("fluency") || lower.contains("speed") || lower.contains("pace") -> {
                "Your fluency score is ${session.fluencyScore}. ${if (session.pauseCount > 0) "Try to reduce pauses and filler words." else "Your pace was smooth. Keep that rhythm."}"
            }
            lower.contains("save") || lower.contains("firebase") || lower.contains("history") -> {
                "Tap Save to Firebase & History to store this assessment. Your conversation history is also included in the saved result."
            }
            else -> "You can ask about sentence mistakes, pronunciation, vocabulary, fluency, or how to save this assessment."
        }
    }

    private fun createSpeechRecognizer(context: Context): SpeechRecognizer {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val message = "Speech recognition is not available on this device."
            if (_isAssistantListening.value) _assistantStatus.value = message
            _userRealSpeech.value = message
            throw IllegalStateException(message)
        }
        return SpeechRecognizer.createSpeechRecognizer(context)
    }

    // ── Core analysis flow ────────────────────────────────────────────────────

    fun completeAnalysis() {
        // AtomicBoolean: only the first caller (callback OR fallback) gets through.
        // compareAndSet(false, true) returns true only once; every subsequent call returns false.
        if (!isAnalyzing.compareAndSet(false, true)) return

        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val userSpeechText = _userRealSpeech.value.trim()

                if (userSpeechText.isBlank()) {
                    // Nothing was spoken – reset cleanly so the screen returns to ready state.
                    _isProcessing.value  = false
                    _showQuickSummary.value = false
                    isAnalyzing.set(false)
                    return@launch
                }

                val topic        = _topics.value.getOrNull(_topicIndex.value) ?: "Speaking Challenge"
                val originalText = _currentParagraph.value

                val responseJsonString: String
                val audioFile = recordingFile
                if (audioFile != null && audioFile.exists()) {
                    var result = ""
                    try {
                        result = com.example.masterenglishfluency.api.GeminiVoiceService
                            .evaluateReading(originalText, userSpeechText)
                    } catch (e: Exception) {
                        Log.w("SpeakingViewModel", "Gemini API failed, using offline evaluation", e)
                        result = performOfflineEvaluation(originalText, userSpeechText)
                    } finally {
                        try { audioFile.delete() } catch (e: Exception) {
                            Log.e("SpeakingViewModel", "Failed to delete audio file", e)
                        }
                    }
                    responseJsonString = normalizeReadingEvaluationJson(
                        originalText,
                        userSpeechText,
                        result
                    )
                } else {
                    responseJsonString = performOfflineEvaluation(originalText, userSpeechText)
                }

                val eval = JSONObject(responseJsonString)
                val session = SpeakingSession(
                    id                = UUID.randomUUID().toString(),
                    topic             = topic.split(":").firstOrNull()?.trim() ?: topic,
                    transcript        = userSpeechText,
                    fluencyScore      = eval.optInt("fluencyScore",       85),
                    pronunciationScore = eval.optInt("pronunciationScore", 85),
                    grammarScore      = eval.optInt("grammarScore",        85),
                    vocabularyScore   = eval.optInt("vocabularyScore",     85),
                    durationSeconds   = _secondsElapsed.value.coerceAtLeast(3),
                    timestamp         = System.currentTimeMillis(),
                    aiResponse        = responseJsonString,
                    audioPath         = null
                )

                _currentSession.value   = session
                _showQuickSummary.value = false
                _isAssistantEnabled.value = true
                val feedbackText = buildAssistantFeedbackText(session)
                val mistakesDetected = hasMistakes(session, originalText)
                if (mistakesDetected) {
                    _showFeedback.value = true
                    _showSuccess.value = false
                } else {
                    _showFeedback.value = false
                    _showSuccess.value = true
                }
                _assistantReplyText.value = feedbackText
                _assistantStatus.value = feedbackText
                _assistantMessages.value = listOf(
                    AssistantMessage("AI", feedbackText)
                )

            } catch (e: Exception) {
                Log.e("SpeakingViewModel", "Error during completeAnalysis", e)
                // On unexpected error ensure processing spinner is cleared
                _showQuickSummary.value = false
            } finally {
                isAnalyzing.set(false)
                _isProcessing.value = false
            }
        }
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    fun confirmQuickSummary() {
        _showQuickSummary.value = false
        _showFeedback.value = true
    }

    fun dismissQuickSummary() {
        _showQuickSummary.value = false
        _showFeedback.value = false
        _currentSession.value = null
        _userRealSpeech.value = ""
    }

    fun saveSession() {
        val session = _currentSession.value ?: return
        if (session.transcript.isBlank()) return
        viewModelScope.launch {
            val enrichedSession = session.copy(
                aiResponse = enrichSessionWithAssistantHistory(session.aiResponse)
            )
            repository.addSpeakingSession(enrichedSession)
        }
        _showFeedback.value = false
        _currentSession.value = null
        _userRealSpeech.value = ""
        _assistantMessages.value = emptyList()
        _assistantStatus.value = ""
        _assistantReplyText.value = ""
        _isAssistantEnabled.value = false
    }

    private fun enrichSessionWithAssistantHistory(aiResponse: String): String {
        if (_assistantMessages.value.isEmpty()) return aiResponse
        return try {
            val json = if (aiResponse.startsWith("{")) JSONObject(aiResponse) else JSONObject()
            json.put("assistantConversationHistory", JSONArray(_assistantMessages.value.map { message ->
                JSONObject().apply {
                    put("speaker", message.speaker)
                    put("text", message.text)
                    put("timestamp", message.timestamp)
                }
            }))
            json.toString()
        } catch (e: Exception) {
            Log.w("SpeakingViewModel", "Failed to enrich session with assistant history", e)
            aiResponse
        }
    }

    fun discardSession() {
        _currentSession.value = null
        _showFeedback.value = false
        _showQuickSummary.value = false
        _userRealSpeech.value = ""
    }

    fun hasMistakes(session: SpeakingSession, originalText: String): Boolean {
        val transcript = session.transcript.trim()
        if (transcript.isBlank() || originalText.isBlank()) return false

        val spokenSet = cleanRegex.replace(transcript, "").lowercase()
            .split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val diff = computeWordDiff(originalText, transcript)
        @Suppress("UNCHECKED_CAST")
        val missedWords = diff["missedWords"] as List<String>
        @Suppress("UNCHECKED_CAST")
        val incorrectWords = diff["incorrectWords"] as List<String>
        @Suppress("UNCHECKED_CAST")
        val extraWords = diff["extraWords"] as List<String>
        val pauseCount = detectPauses(transcript)
        val skippedSentences = computeSkippedSentences(originalText, spokenSet)

        return missedWords.isNotEmpty()
                || incorrectWords.isNotEmpty()
                || extraWords.isNotEmpty()
                || skippedSentences.isNotEmpty()
                || pauseCount >= 3
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaRecorder = null
        viewModelScope.launch(Dispatchers.Main) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
