package com.example.masterenglishfluency.api

import com.example.masterenglishfluency.BuildConfig
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiVoiceService {
    private const val TAG = "GeminiVoiceService"

    private val geminiApiKey: String = BuildConfig.GEMINI_API_KEY

    suspend fun processAudio(
        audioFile: File,
        role: String,
        languageMode: String,
        historyContext: String,
        userRealSpeech: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        if (geminiApiKey.isBlank()) {
            throw IllegalStateException("Gemini API Key is not configured.")
        }

        val urlConnection = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey")
            .openConnection() as HttpURLConnection

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val promptText = "You are an AI English speaking tutor. Your role context is: '$role'.\n" +
                    "Here is the conversation history so far:\n" +
                    historyContext + "\n" +
                    "The user has just spoken: \"$userRealSpeech\".\n" +
                    "Respond to the user's input. Evaluate their grammar, pronunciation, and flow if appropriate, and provide a helpful, natural, and engaging response as your role.\n" +
                    "Keep the output format strictly as a JSON object, e.g., {\"transcript\": \"...\", \"aiResponse\": \"...\"}.\n" +
                    "In the JSON response, set the 'transcript' key to the exact text: \"$userRealSpeech\", and the 'aiResponse' key to your conversational tutor reply.\n" +
                    if (languageMode == "Tamil") {
                        "IMPORTANT: The user wants bilingual practice. Understand their input (which may be in Tamil or Tanglish), " +
                        "but respond as your role using code-switching (mix of Tamil and English / Tanglish) or Tamil to guide and encourage them to practice English."
                    } else {
                        "Respond entirely in fluent, natural English with corrections if they make mistakes."
                    }

            // Construct Gemini request JSON payload
            val root = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.doOutput = true
            urlConnection.connectTimeout = 15000
            urlConnection.readTimeout = 15000

            OutputStreamWriter(urlConnection.outputStream).use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = urlConnection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Gemini Response: $responseText")
                val responseJson = JSONObject(responseText)

                // Parse generated content JSON
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val textResponse = parts.getJSONObject(0).getString("text")

                // Extract transcript and aiResponse from the inner JSON string returned by Gemini
                val innerJson = JSONObject(textResponse.trim())
                val transcript = innerJson.optString("transcript", "Unable to transcribe audio.")
                val aiResponse = innerJson.optString("aiResponse", "Good job speaking!")
                Pair(transcript, aiResponse)
            } else {
                val errorText = urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "HTTP Error Code $responseCode: $errorText")
                throw Exception("API call failed with HTTP $responseCode")
            }
        } finally {
            urlConnection.disconnect()
        }
    }

    suspend fun transcribeAudio(audioFile: File) = withContext(Dispatchers.IO) {
        if (geminiApiKey.isBlank()) {
            throw IllegalStateException("Gemini API Key is not configured.")
        }

        val urlConnection = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey")
            .openConnection() as HttpURLConnection

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val promptText = "Transcribe the spoken audio exactly as the user said it. Return only the transcript text."

            val root = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
            }

            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.doOutput = true
            urlConnection.connectTimeout = 15000
            urlConnection.readTimeout = 15000

            OutputStreamWriter(urlConnection.outputStream).use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = urlConnection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).optString("text").trim()
            } else {
                val errorText = urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "HTTP Error Code $responseCode: $errorText")
                throw Exception("API call failed with HTTP $responseCode")
            }
        } finally {
            urlConnection.disconnect()
        }
    }

    suspend fun evaluateReading(
        originalParagraph: String,
        userSpeechText: String
    ): String = withContext(Dispatchers.IO) {
        if (geminiApiKey.isBlank()) {
            throw IllegalStateException("Gemini API Key is not configured.")
        }

        val urlConnection = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey")
            .openConnection() as HttpURLConnection

        try {
            val promptText = "You are an IELTS Speaking examiner and pronunciation specialist.\n" +
                    "Compare the original paragraph read by the student with the student's spoken transcript.\n\n" +
                    "Original Paragraph:\n\"$originalParagraph\"\n\n" +
                    "Student's Spoken Transcript:\n\"$userSpeechText\"\n\n" +
                    "Task:\n" +
                    "Analyze the differences between the original paragraph and the student's transcript. Compute accurate scores from 0 to 100 for pronunciation, fluency, accuracy, vocabulary, and grammar. The overall score should be the average of these scores.\n" +
                    "Count how many words are correct, how many are missed (omitted), and list any mispronounced/substituted words.\n" +
                    "Identify sentence-level mistakes, including omitted, incomplete, or incorrect sentences. Provide vocabulary feedback based on how many key words from the paragraph were spoken.\n" +
                    "Provide a clear qualitative feedback mapping strengths, mistakes, suggested corrections, and an overall result message.\n\n" +
                    "Output format:\n" +
                    "Return ONLY a valid JSON object matching the following structure (do not include markdown wrapping or backticks):\n" +
                    "{\n" +
                    "  \"sessionType\": \"Reading Assessment\",\n" +
                    "  \"overallScore\": 85,\n" +
                    "  \"pronunciationScore\": 80,\n" +
                    "  \"fluencyScore\": 90,\n" +
                    "  \"accuracyScore\": 85,\n" +
                    "  \"vocabularyScore\": 88,\n" +
                    "  \"grammarScore\": 82,\n" +
                    "  \"wordsCorrect\": 40,\n" +
                    "  \"wordsMissed\": 5,\n" +
                    "  \"mispronouncedWords\": [\"word1\", \"word2\"],\n" +
                    "  \"sentenceMistakes\": [\"Missed or incomplete sentence: ...\"],\n" +
                    "  \"vocabularyFeedback\": \"Feedback about key vocabulary coverage.\",\n" +
                    "  \"overallResultMessage\": \"Overall result message.\",\n" +
                    "  \"strengths\": \"Strengths description\",\n" +
                    "  \"mistakes\": \"Mistakes description\",\n" +
                    "  \"suggestedCorrections\": \"Suggested corrections description\",\n" +
                    "  \"improvedVersion\": \"Improved version paragraph text\",\n" +
                    "  \"paragraph\": \"Original paragraph text\"\n" +
                    "}"

            val root = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.doOutput = true
            urlConnection.connectTimeout = 15000
            urlConnection.readTimeout = 15000

            OutputStreamWriter(urlConnection.outputStream).use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = urlConnection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Gemini Reading Evaluation Response: $responseText")
                val responseJson = JSONObject(responseText)

                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text").trim()
            } else {
                val errorText = urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "HTTP Error Code $responseCode: $errorText")
                throw Exception("API call failed with HTTP $responseCode")
            }
        } finally {
            urlConnection.disconnect()
        }
    }
}
