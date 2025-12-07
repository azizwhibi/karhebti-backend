package com.example.karhebti_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language_cache")
data class LanguageCacheEntity(
    @PrimaryKey val languageCode: String,
    val name: String,
    val nativeName: String,
    val cachedAt: Long
)

