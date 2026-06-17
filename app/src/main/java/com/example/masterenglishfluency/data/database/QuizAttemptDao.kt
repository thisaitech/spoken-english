package com.example.masterenglishfluency.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masterenglishfluency.data.model.QuizAttempt
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizAttemptDao {
    @Query("SELECT * FROM quiz_attempts ORDER BY completed_at DESC, id DESC LIMIT :limit")
    fun observeRecentAttempts(limit: Int): Flow<List<QuizAttempt>>

    @Query("SELECT COUNT(*) FROM quiz_attempts")
    fun observeAttemptCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: QuizAttempt)
}
