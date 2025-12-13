package com.example.karhebti_android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface TranslationDao {
    @Query("SELECT translatedText FROM translations WHERE `key` = :key AND languageCode = :languageCode LIMIT 1")
    suspend fun getTranslatedText(key: String, languageCode: String): String?

    @Query("SELECT * FROM translations WHERE languageCode = :languageCode LIMIT :limit OFFSET :offset")
    suspend fun getTranslationsByLanguagePaginated(languageCode: String, limit: Int, offset: Int): List<TranslationEntity>

    @Insert
    suspend fun insertTranslations(entities: List<TranslationEntity>)

    @Query("DELETE FROM translations WHERE languageCode = :languageCode")
    suspend fun deleteTranslationsByLanguage(languageCode: String)

    @Query("DELETE FROM translations")
    suspend fun deleteAllTranslations()
}

