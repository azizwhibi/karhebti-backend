package com.example.karhebti_android.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UpcomingMaintenanceEntity::class,
        TranslationEntity::class,
        LanguageCacheEntity::class,
        LanguageListCacheEntity::class
    ],
    version = 2, // Incremented version number
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun upcomingMaintenanceDao(): UpcomingMaintenanceDao
    abstract fun translationDao(): TranslationDao
    abstract fun languageCacheDao(): LanguageCacheDao
    abstract fun languageListCacheDao(): LanguageListCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "karhebti_database"
                )
                .fallbackToDestructiveMigration() // Added for dev safety
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
