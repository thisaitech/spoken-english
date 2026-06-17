package com.example.masterenglishfluency.data

import org.json.JSONObject

data class SpeakingSession(
    val id: String,
    val topic: String,
    val transcript: String,
    val fluencyScore: Int,
    val pronunciationScore: Int,
    val grammarScore: Int,
    val vocabularyScore: Int,
    val durationSeconds: Int,
    val timestamp: Long,
    val aiResponse: String = "",
    val audioPath: String? = null
) {
    private val json: JSONObject? by lazy {
        try { if (aiResponse.startsWith("{")) JSONObject(aiResponse) else null }
        catch (e: Exception) { null }
    }

    private fun jsonStringList(key: String): List<String> =
        json?.optJSONArray(key)?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()

    val sessionType: String get() = json?.optString("sessionType", "Reading Assessment") ?: "Reading Assessment"
    val paragraphText: String get() = json?.optString("paragraph") ?: ""
    val overallScore: Int get() = json?.optInt("overallScore") ?: ((fluencyScore + pronunciationScore + grammarScore + vocabularyScore) / 4)
    val accuracyScore: Int get() = json?.optInt("accuracyScore") ?: pronunciationScore
    val wordsCorrectCount: Int get() = json?.optInt("wordsCorrect") ?: 0
    val wordsMissedCount: Int get() = json?.optInt("wordsMissed") ?: 0
    val extraWordsCount: Int get() = json?.optInt("extraWords") ?: 0
    val pauseCount: Int get() = json?.optInt("pauseCount") ?: 0
    val sentenceMistakesList: List<String> get() = jsonStringList("sentenceMistakes")
    val vocabularyFeedbackText: String get() = json?.optString("vocabularyFeedback") ?: ""
    val overallResultMessage: String get() = json?.optString("overallResultMessage") ?: ""
    val mispronouncedWordsList: List<String> get() = jsonStringList("mispronouncedWords")
    val missedWordsList: List<String> get() = jsonStringList("missedWords")
    val extraWordsList: List<String> get() = jsonStringList("extraWordsList")
    val strengthsText: String get() = json?.optString("strengths") ?: ""
    val mistakesText: String get() = json?.optString("mistakes") ?: ""
    val suggestedCorrectionsText: String get() = json?.optString("suggestedCorrections") ?: ""
    val improvedParagraphText: String get() = json?.optString("improvedVersion") ?: ""
    val aiFeedbackTips: String get() = json?.optString("aiFeedbackTips") ?: ""
}
