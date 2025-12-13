package com.example.karhebti_android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LanguageListCacheDao {
    @Query("SELECT * FROM language_list_cache LIMIT 1")
    fun getCache(): LanguageListCacheEntity?

    @Insert
    fun insertCache(cache: LanguageListCacheEntity)
}

