package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.data.model.Attendance
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.data.model.Student
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

sealed class CloudSyncState {
    object Idle : CloudSyncState()
    object Syncing : CloudSyncState()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : CloudSyncState()
    data class Error(val message: String) : CloudSyncState()
    data class NotConfigured(val message: String) : CloudSyncState()
}

class FirestoreSyncService(private val context: Context) {

    private val tag = "FirestoreSyncService"

    /**
     * Helper extension to await Google Play Tasks in coroutines safely
     */
    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result ->
                cont.resume(result) { }
            }
            addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
        }

    fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (isFirebaseConfigured()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firestore not initialized: ${e.message}")
            null
        }
    }

    // ==========================================
    // Course Persistence
    // ==========================================

    suspend fun saveCourse(course: Course): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured. Add google-services.json to enable cloud sync.")
        )
        try {
            val docData = hashMapOf(
                "id" to course.id,
                "name" to course.name,
                "subject" to course.subject,
                "feeAmount" to course.feeAmount,
                "timingSchedule" to course.timingSchedule,
                "description" to course.description,
                "createdAt" to course.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("courses")
                .document(course.id.toString())
                .set(docData, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving course to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured.")
        )
        try {
            firestore.collection("courses")
                .document(courseId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting course from Firestore", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Student Persistence
    // ==========================================

    suspend fun saveStudent(student: Student): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured. Add google-services.json to enable cloud sync.")
        )
        try {
            val docData = hashMapOf(
                "id" to student.id,
                "courseId" to student.courseId,
                "name" to student.name,
                "phone" to student.phone,
                "email" to student.email,
                "address" to student.address,
                "enrollmentDate" to student.enrollmentDate,
                "totalFee" to student.totalFee,
                "paidFee" to student.paidFee,
                "pendingFee" to student.pendingFee,
                "feeStatus" to student.feeStatus,
                "notes" to student.notes,
                "isActive" to student.isActive,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("students")
                .document(student.id.toString())
                .set(docData, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving student to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(studentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured.")
        )
        try {
            firestore.collection("students")
                .document(studentId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting student from Firestore", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Attendance Logs Persistence
    // ==========================================

    suspend fun saveAttendance(attendance: Attendance): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured. Add google-services.json to enable cloud sync.")
        )
        try {
            val docId = if (attendance.id > 0) attendance.id.toString() else "${attendance.courseId}_${attendance.studentId}_${attendance.date}"
            val docData = hashMapOf(
                "id" to attendance.id,
                "studentId" to attendance.studentId,
                "courseId" to attendance.courseId,
                "date" to attendance.date,
                "status" to attendance.status,
                "remark" to attendance.remark,
                "recordedAt" to attendance.recordedAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("attendance_logs")
                .document(docId)
                .set(docData, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving attendance to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun saveAttendanceBatch(attendances: List<Attendance>): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured. Add google-services.json to enable cloud sync.")
        )
        try {
            val batch = firestore.batch()
            for (att in attendances) {
                val docId = if (att.id > 0) att.id.toString() else "${att.courseId}_${att.studentId}_${att.date}"
                val docRef = firestore.collection("attendance_logs").document(docId)
                val docData = hashMapOf(
                    "id" to att.id,
                    "studentId" to att.studentId,
                    "courseId" to att.courseId,
                    "date" to att.date,
                    "status" to att.status,
                    "remark" to att.remark,
                    "recordedAt" to att.recordedAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, docData, SetOptions.merge())
            }
            batch.commit().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving attendance batch to Firestore", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Fee Payment Persistence
    // ==========================================

    suspend fun saveFeePayment(payment: FeePayment): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured. Add google-services.json to enable cloud sync.")
        )
        try {
            val docData = hashMapOf(
                "id" to payment.id,
                "studentId" to payment.studentId,
                "amount" to payment.amount,
                "paymentDate" to payment.paymentDate,
                "paymentMode" to payment.paymentMode,
                "receiptNo" to payment.receiptNo,
                "notes" to payment.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("fee_payments")
                .document(payment.id.toString())
                .set(docData, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving fee payment to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFeePayment(paymentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not configured.")
        )
        try {
            firestore.collection("fee_payments")
                .document(paymentId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting fee payment from Firestore", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // Full Bidirectional Sync
    // ==========================================

    suspend fun syncAllToCloud(
        students: List<Student>,
        courses: List<Course>,
        attendances: List<Attendance>,
        payments: List<FeePayment>
    ): CloudSyncState = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext CloudSyncState.NotConfigured(
            "Firestore is not configured. Attach google-services.json to activate real-time cloud sync."
        )

        try {
            // 1. Sync Courses
            val courseBatch = firestore.batch()
            for (c in courses) {
                val ref = firestore.collection("courses").document(c.id.toString())
                courseBatch.set(
                    ref,
                    hashMapOf(
                        "id" to c.id,
                        "name" to c.name,
                        "subject" to c.subject,
                        "feeAmount" to c.feeAmount,
                        "timingSchedule" to c.timingSchedule,
                        "description" to c.description,
                        "createdAt" to c.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }
            if (courses.isNotEmpty()) {
                courseBatch.commit().awaitTask()
            }

            // 2. Sync Students
            val studentBatch = firestore.batch()
            for (s in students) {
                val ref = firestore.collection("students").document(s.id.toString())
                studentBatch.set(
                    ref,
                    hashMapOf(
                        "id" to s.id,
                        "courseId" to s.courseId,
                        "name" to s.name,
                        "phone" to s.phone,
                        "email" to s.email,
                        "address" to s.address,
                        "enrollmentDate" to s.enrollmentDate,
                        "totalFee" to s.totalFee,
                        "paidFee" to s.paidFee,
                        "pendingFee" to s.pendingFee,
                        "feeStatus" to s.feeStatus,
                        "notes" to s.notes,
                        "isActive" to s.isActive,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }
            if (students.isNotEmpty()) {
                studentBatch.commit().awaitTask()
            }

            // 3. Sync Attendance
            val attendanceBatch = firestore.batch()
            for (att in attendances) {
                val docId = if (att.id > 0) att.id.toString() else "${att.courseId}_${att.studentId}_${att.date}"
                val ref = firestore.collection("attendance_logs").document(docId)
                attendanceBatch.set(
                    ref,
                    hashMapOf(
                        "id" to att.id,
                        "studentId" to att.studentId,
                        "courseId" to att.courseId,
                        "date" to att.date,
                        "status" to att.status,
                        "remark" to att.remark,
                        "recordedAt" to att.recordedAt,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }
            if (attendances.isNotEmpty()) {
                attendanceBatch.commit().awaitTask()
            }

            // 4. Sync Payments
            val paymentBatch = firestore.batch()
            for (p in payments) {
                val ref = firestore.collection("fee_payments").document(p.id.toString())
                paymentBatch.set(
                    ref,
                    hashMapOf(
                        "id" to p.id,
                        "studentId" to p.studentId,
                        "amount" to p.amount,
                        "paymentDate" to p.paymentDate,
                        "paymentMode" to p.paymentMode,
                        "receiptNo" to p.receiptNo,
                        "notes" to p.notes,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }
            if (payments.isNotEmpty()) {
                paymentBatch.commit().awaitTask()
            }

            CloudSyncState.Success(
                "Successfully synchronized ${students.size} students, ${courses.size} courses, ${attendances.size} attendance logs, and ${payments.size} fee payments to Firestore cloud."
            )
        } catch (e: Exception) {
            Log.e(tag, "Full sync to Firestore failed", e)
            CloudSyncState.Error(e.localizedMessage ?: "Sync to cloud failed: ${e.message}")
        }
    }

    /**
     * Fetch remote students from Firestore
     */
    suspend fun fetchStudentsFromCloud(): List<Student> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection("students").get().awaitTask()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val phone = doc.getString("phone") ?: ""
                    val email = doc.getString("email") ?: ""
                    val address = doc.getString("address") ?: ""
                    val courseId = doc.getLong("courseId")
                    val totalFee = doc.getDouble("totalFee") ?: 0.0
                    val paidFee = doc.getDouble("paidFee") ?: 0.0
                    val feeStatus = doc.getString("feeStatus") ?: "PENDING"
                    val notes = doc.getString("notes") ?: ""
                    val isActive = doc.getBoolean("isActive") ?: true
                    val enrollmentDate = doc.getLong("enrollmentDate") ?: System.currentTimeMillis()

                    Student(
                        id = id,
                        courseId = courseId,
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        enrollmentDate = enrollmentDate,
                        totalFee = totalFee,
                        paidFee = paidFee,
                        feeStatus = feeStatus,
                        notes = notes,
                        isActive = isActive
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching students from Firestore", e)
            emptyList()
        }
    }

    /**
     * Fetch remote courses from Firestore
     */
    suspend fun fetchCoursesFromCloud(): List<Course> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection("courses").get().awaitTask()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val subject = doc.getString("subject") ?: ""
                    val feeAmount = doc.getDouble("feeAmount") ?: 0.0
                    val timingSchedule = doc.getString("timingSchedule") ?: ""
                    val description = doc.getString("description") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    Course(
                        id = id,
                        name = name,
                        subject = subject,
                        feeAmount = feeAmount,
                        timingSchedule = timingSchedule,
                        description = description,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching courses from Firestore", e)
            emptyList()
        }
    }

    /**
     * Fetch remote attendance logs from Firestore
     */
    suspend fun fetchAttendanceFromCloud(): List<Attendance> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection("attendance_logs").get().awaitTask()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                    val studentId = doc.getLong("studentId") ?: return@mapNotNull null
                    val courseId = doc.getLong("courseId") ?: return@mapNotNull null
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val status = doc.getString("status") ?: "PRESENT"
                    val remark = doc.getString("remark") ?: ""
                    val recordedAt = doc.getLong("recordedAt") ?: System.currentTimeMillis()

                    Attendance(
                        id = id,
                        studentId = studentId,
                        courseId = courseId,
                        date = date,
                        status = status,
                        remark = remark,
                        recordedAt = recordedAt
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching attendance from Firestore", e)
            emptyList()
        }
    }
}
