package com.example.masterenglishfluency.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masterenglishfluency.data.model.PracticeSession
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_sessions ORDER BY created_at DESC, id DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<PracticeSession>>

    @Query("SELECT COUNT(*) FROM practice_sessions")
    fun observeSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PracticeSession)
}
