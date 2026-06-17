package com.example.masterenglishfluency.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeakingSessionDao {
    @Query("SELECT * FROM speaking_sessions ORDER BY timestamp DESC")
    fun getSessions(): Flow<List<SpeakingSessionEntity>>

    @Query("SELECT * FROM speaking_sessions ORDER BY timestamp DESC")
    suspend fun getSessionsOnce(): List<SpeakingSessionEntity>

    @Query("SELECT COUNT(*) FROM speaking_sessions")
    suspend fun getSessionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SpeakingSessionEntity)

    @Query("DELETE FROM speaking_sessions")
    suspend fun clearAll()
}
