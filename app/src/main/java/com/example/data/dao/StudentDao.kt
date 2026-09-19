package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE courseId = :courseId ORDER BY name ASC")
    fun getStudentsByCourse(courseId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE courseId = :courseId ORDER BY name ASC")
    suspend fun getStudentsByCourseImmediate(courseId: Long): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentById(id: Long): Flow<Student?>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentByIdImmediate(id: Long): Student?

    @Query("SELECT * FROM students WHERE feeStatus = :status ORDER BY name ASC")
    fun getStudentsByFeeStatus(status: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("UPDATE students SET paidFee = :paidFee, feeStatus = :feeStatus WHERE id = :studentId")
    suspend fun updateStudentPaymentState(studentId: Long, paidFee: Double, feeStatus: String)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getCount(): Int
}
