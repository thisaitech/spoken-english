package com.example.masterenglishfluency.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masterenglishfluency.data.model.GrammarLesson
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarLessonDao {
    @Query("SELECT * FROM grammar_lessons ORDER BY id ASC")
    fun observeLessons(): Flow<List<GrammarLesson>>

    @Query("SELECT * FROM grammar_lessons WHERE is_completed = 1 ORDER BY id ASC")
    fun observeCompletedLessons(): Flow<List<GrammarLesson>>

    @Query("SELECT * FROM grammar_lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: Long): GrammarLesson?

    @Query("SELECT COUNT(*) FROM grammar_lessons WHERE is_completed = 1")
    fun observeCompletedLessonCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<GrammarLesson>)

    @Query("UPDATE grammar_lessons SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCompleted(id: Long, isCompleted: Boolean, updatedAt: Long)
}
