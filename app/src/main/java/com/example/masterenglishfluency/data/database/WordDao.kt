package com.example.masterenglishfluency.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.masterenglishfluency.data.model.WordOfDay
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE date_key = :dateKey ORDER BY id DESC LIMIT 1")
    fun observeWordOfDay(dateKey: String): Flow<WordOfDay?>

    @Query("SELECT * FROM words WHERE date_key = :dateKey ORDER BY id DESC LIMIT 1")
    suspend fun getWordOfDay(dateKey: String): WordOfDay?

    @Query("SELECT * FROM words ORDER BY date_key DESC, id DESC")
    fun observeAllWords(): Flow<List<WordOfDay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordOfDay>)

    @Query("UPDATE words SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateCompleted(id: Long, isCompleted: Boolean)
}
