package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val subject: String,
    val feeAmount: Double,
    val timingSchedule: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
