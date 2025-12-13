package com.example.karhebti_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "upcoming_maintenances")
data class UpcomingMaintenanceEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val voitureId: String,
    val plate: String?,
    val dueAt: Date,
    val status: String,
    val lastUpdated: Date = Date()
)

