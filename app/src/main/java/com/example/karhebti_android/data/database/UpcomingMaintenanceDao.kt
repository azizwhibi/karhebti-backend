package com.example.karhebti_android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UpcomingMaintenanceDao {

    @Query("SELECT * FROM upcoming_maintenances ORDER BY dueAt ASC LIMIT :limit")
    fun getUpcomingMaintenances(limit: Int = 5): Flow<List<UpcomingMaintenanceEntity>>

    @Query("SELECT * FROM upcoming_maintenances ORDER BY dueAt ASC LIMIT :limit")
    suspend fun getUpcomingMaintenancesList(limit: Int = 5): List<UpcomingMaintenanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(maintenances: List<UpcomingMaintenanceEntity>)

    @Query("DELETE FROM upcoming_maintenances")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM upcoming_maintenances")
    suspend fun getCount(): Int
}

