package com.example.masterenglishfluency.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_words ORDER BY id ASC")
    fun getWords(): Flow<List<VocabularyWordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<VocabularyWordEntity>)

    @Query("UPDATE vocabulary_words SET isBookmarked = NOT isBookmarked WHERE id = :wordId")
    suspend fun toggleBookmark(wordId: Int)

    @Query("SELECT COUNT(*) FROM vocabulary_words")
    suspend fun getCount(): Int

    @Query("DELETE FROM vocabulary_words")
    suspend fun clearAll()

    @Query("SELECT id FROM vocabulary_words WHERE isBookmarked = 1")
    suspend fun getBookmarkedWordIds(): List<Int>

    @Query("UPDATE vocabulary_words SET isBookmarked = 0")
    suspend fun clearAllBookmarks()

    @Query("UPDATE vocabulary_words SET isBookmarked = 1 WHERE id IN (:bookmarkedIds)")
    suspend fun setBookmarks(bookmarkedIds: List<Int>)
}
