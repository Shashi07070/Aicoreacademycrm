package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduledClass
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledClassDao {
    @Query("SELECT * FROM scheduled_classes ORDER BY date ASC, startTime ASC")
    fun getAllClasses(): Flow<List<ScheduledClass>>

    @Query("SELECT * FROM scheduled_classes WHERE date = :date ORDER BY startTime ASC")
    fun getClassesForDate(date: String): Flow<List<ScheduledClass>>

    @Query("SELECT * FROM scheduled_classes WHERE courseId = :courseId ORDER BY date ASC")
    fun getClassesForCourse(courseId: Long): Flow<List<ScheduledClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(scheduledClass: ScheduledClass): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ScheduledClass>)

    @Update
    suspend fun updateClass(scheduledClass: ScheduledClass)

    @Delete
    suspend fun deleteClass(scheduledClass: ScheduledClass)

    @Query("SELECT COUNT(*) FROM scheduled_classes")
    suspend fun getCount(): Int
}
