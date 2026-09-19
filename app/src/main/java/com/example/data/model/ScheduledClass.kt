package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_classes")
data class ScheduledClass(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long = 0,
    val courseName: String,
    val date: String, // "YYYY-MM-DD"
    val dayOfWeek: String = "",
    val startTime: String,
    val endTime: String,
    val topic: String = "",
    val room: String = "Hall A",
    val instructor: String = "Prof. Alan Vance",
    val classType: String = "Lecture", // Lecture, Lab, Workshop, Test
    val createdAt: Long = System.currentTimeMillis()
)
