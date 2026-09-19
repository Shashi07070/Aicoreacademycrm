package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FeePayment
import kotlinx.coroutines.flow.Flow

@Dao
interface FeePaymentDao {
    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId ORDER BY paymentDate DESC")
    fun getPaymentsForStudent(studentId: Long): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForStudentImmediate(studentId: Long): List<FeePayment>

    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<FeePayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: FeePayment): Long

    @Delete
    suspend fun deletePayment(payment: FeePayment)

    @Query("SELECT SUM(amount) FROM fee_payments WHERE studentId = :studentId")
    suspend fun getTotalPaidForStudent(studentId: Long): Double?
}
