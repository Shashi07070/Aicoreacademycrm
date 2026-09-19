package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long?,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val enrollmentDate: Long = System.currentTimeMillis(),
    val totalFee: Double = 0.0,
    val paidFee: Double = 0.0,
    val feeStatus: String = "PENDING", // PENDING, PARTIAL, COMPLETED
    val notes: String = "",
    val isActive: Boolean = true
) {
    val pendingFee: Double
        get() = (totalFee - paidFee).coerceAtLeast(0.0)
}
