package com.example.karhebti_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "translations",
    indices = [
        Index("languageCode", "key", unique = true),
        Index("languageCode"),
        Index("updatedAt")
    ]
)
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val languageCode: String,
    val originalText: String,
    val translatedText: String,
    val updatedAt: Long = System.currentTimeMillis()
)
