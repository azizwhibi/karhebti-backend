package com.example.karhebti_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language_list_cache")
data class LanguageListCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lastFetchedAt: Long,
    val cacheExpiryAt: Long
)

