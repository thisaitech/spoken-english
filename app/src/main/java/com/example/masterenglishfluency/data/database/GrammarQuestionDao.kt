package com.example.masterenglishfluency.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masterenglishfluency.data.model.GrammarQuestion

@Dao
interface GrammarQuestionDao {
    @Query("SELECT * FROM grammar_questions WHERE lesson_id = :lessonId ORDER BY id ASC")
    suspend fun getQuestionsForLesson(lessonId: Long): List<GrammarQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<GrammarQuestion>)
}
