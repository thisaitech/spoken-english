package com.example.masterenglishfluency.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.masterenglishfluency.data.database.StringListConverter

@Entity(tableName = "grammar_questions")
@TypeConverters(StringListConverter::class)
data class GrammarQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lesson_id") val lessonId: Long,
    val question: String,
    val options: List<String>,
    @ColumnInfo(name = "correct_option_index") val correctOptionIndex: Int
)
