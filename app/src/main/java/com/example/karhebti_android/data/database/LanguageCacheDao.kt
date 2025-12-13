package com.example.karhebti_android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface LanguageCacheDao {
    @Query("SELECT * FROM language_cache")
    fun getAllLanguagesSync(): List<LanguageCacheEntity>

    @Query("DELETE FROM language_cache")
    fun deleteAllLanguages()

    @Insert
    fun insertLanguages(languages: List<LanguageCacheEntity>)
}

