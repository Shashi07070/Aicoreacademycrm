package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AttendanceDao
import com.example.data.dao.CourseDao
import com.example.data.dao.FeePaymentDao
import com.example.data.dao.ScheduledClassDao
import com.example.data.dao.StudentDao
import com.example.data.model.Attendance
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.data.model.ScheduledClass
import com.example.data.model.Student

@Database(
    entities = [
        Course::class,
        Student::class,
        FeePayment::class,
        Attendance::class,
        ScheduledClass::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun studentDao(): StudentDao
    abstract fun feePaymentDao(): FeePaymentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun scheduledClassDao(): ScheduledClassDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tuition_crm_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
