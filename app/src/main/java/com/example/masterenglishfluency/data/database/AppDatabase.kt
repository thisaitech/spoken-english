package com.example.masterenglishfluency.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.masterenglishfluency.data.model.GrammarLesson
import com.example.masterenglishfluency.data.model.GrammarQuestion
import com.example.masterenglishfluency.data.model.PracticeSession
import com.example.masterenglishfluency.data.model.QuizAttempt
import com.example.masterenglishfluency.data.model.WordOfDay

@Database(
    entities = [
        WordOfDay::class,
        GrammarLesson::class,
        GrammarQuestion::class,
        QuizAttempt::class,
        PracticeSession::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun grammarLessonDao(): GrammarLessonDao
    abstract fun grammarQuestionDao(): GrammarQuestionDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun practiceSessionDao(): PracticeSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "master_english_fluency.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
