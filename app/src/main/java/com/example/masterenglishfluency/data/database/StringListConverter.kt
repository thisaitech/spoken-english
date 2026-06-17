package com.example.masterenglishfluency.data.database

import androidx.room.TypeConverter

object StringListConverter {
    @TypeConverter
    fun fromStringList(values: List<String>): String = values.joinToString("||")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isBlank()) emptyList() else value.split("||")
}
