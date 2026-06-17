package com.example.masterenglishfluency.data.repository

import com.example.masterenglishfluency.data.model.GrammarLesson
import com.example.masterenglishfluency.data.model.GrammarQuestion
import com.example.masterenglishfluency.data.model.WordOfDay

object SampleData {
    private val now = System.currentTimeMillis()

    fun todayKey(): String = "word-${System.currentTimeMillis() / 86_400_000}"

    val words: List<WordOfDay>
        get() = listOf(
            WordOfDay(
                word = "Fluent",
                meaning = "Able to speak or write smoothly, accurately, and with confidence.",
                example = "After daily practice, she became more fluent in business conversations.",
                pronunciation = "FLOO-ent",
                dateKey = todayKey()
            ),
            WordOfDay(
                word = "Articulate",
                meaning = "Able to express ideas clearly and effectively.",
                example = "The candidate gave an articulate answer during the interview.",
                pronunciation = "ar-TIK-yoo-lit",
                dateKey = "word-previous-1"
            ),
            WordOfDay(
                word = "Confidence",
                meaning = "A feeling of trust in your abilities and preparation.",
                example = "Confidence grows when you practice speaking every day.",
                pronunciation = "KON-fi-dens",
                dateKey = "word-previous-2"
            ),
            WordOfDay(
                word = "Vocabulary",
                meaning = "The set of words you know and use in a language.",
                example = "Reading articles helps you build a stronger vocabulary.",
                pronunciation = "vo-KAB-yoo-ler-ee",
                dateKey = "word-previous-3"
            )
        )

    val lessons: List<GrammarLesson>
        get() = listOf(
            GrammarLesson(
                title = "Present Perfect for Experiences",
                topic = "Grammar",
                explanation = "Use the present perfect to talk about life experiences when the exact time is not important. The structure is have/has + past participle.",
                examples = "I have visited London. She has learned three new phrases this week.",
                updatedAt = now
            ),
            GrammarLesson(
                title = "Past Simple vs Present Perfect",
                topic = "Tenses",
                explanation = "Use the past simple for finished time, such as yesterday or in 2020. Use the present perfect for unfinished time or experiences connected to now.",
                examples = "I watched a movie yesterday. I have watched two movies this week.",
                updatedAt = now
            ),
            GrammarLesson(
                title = "Modal Verbs for Polite Requests",
                topic = "Speaking",
                explanation = "Use could, would, and may to make requests sound polite and natural in professional or formal situations.",
                examples = "Could you repeat that, please? Would you mind speaking more slowly?",
                updatedAt = now
            ),
            GrammarLesson(
                title = "Conditionals for Real Situations",
                topic = "Conditionals",
                explanation = "Use the first conditional for real future possibilities: If + present simple, will + base verb.",
                examples = "If I practice daily, I will improve my fluency. If you join the call, we will discuss pronunciation.",
                updatedAt = now
            )
        )

    val questions: Map<Long, List<GrammarQuestion>>
        get() = mapOf(
            1L to listOf(
                GrammarQuestion(
                    lessonId = 1L,
                    question = "Choose the correct sentence for an unfinished life experience.",
                    options = listOf("I have visited Canada.", "I visited Canada in 2010.", "I am visiting Canada yesterday.", "I will have visited Canada."),
                    correctOptionIndex = 0
                )
            ),
            2L to listOf(
                GrammarQuestion(
                    lessonId = 2L,
                    question = "Which sentence correctly uses past simple?",
                    options = listOf("I have seen him yesterday.", "I saw him yesterday.", "I see him yesterday.", "I was see him yesterday."),
                    correctOptionIndex = 1
                )
            ),
            3L to listOf(
                GrammarQuestion(
                    lessonId = 3L,
                    question = "Which request sounds the most polite?",
                    options = listOf("Give me the file now.", "Could you send me the file, please?", "You send the file.", "Send the file."),
                    correctOptionIndex = 1
                )
            ),
            4L to listOf(
                GrammarQuestion(
                    lessonId = 4L,
                    question = "Choose the correct first conditional sentence.",
                    options = listOf("If I will practice, I improve.", "If I practice, I will improve.", "If I practiced, I will improve.", "If I practice, I improved."),
                    correctOptionIndex = 1
                )
            )
        )

    val practicePrompts: List<String> = listOf(
        "Introduce yourself professionally in 60 seconds. Mention your goals, strengths, and one recent achievement.",
        "Describe a book, movie, or podcast that helped your English. Explain what you learned and why you recommend it.",
        "Talk about a challenge you overcame. Use past tense details and explain what the experience taught you.",
        "Explain your opinion about online learning. Give two reasons and one example from your life.",
        "Describe your ideal English practice routine for the next month. Include speaking, listening, reading, and vocabulary."
    )
}
