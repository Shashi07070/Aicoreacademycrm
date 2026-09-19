package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminAuthManager
import com.example.data.AppDatabase
import com.example.data.TuitionRepository
import com.example.data.cloud.CloudSyncState
import com.example.data.cloud.FirestoreSyncService
import com.example.data.model.Attendance
import com.example.data.model.ChartMetricMode
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.data.model.MonthlyRevenueItem
import com.example.data.model.MonthlyRevenueOverview
import com.example.data.model.RevenueTimeRange
import com.example.data.model.ScheduledClass
import com.example.data.model.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TuitionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = TuitionRepository(
        courseDao = db.courseDao(),
        studentDao = db.studentDao(),
        feePaymentDao = db.feePaymentDao(),
        attendanceDao = db.attendanceDao(),
        scheduledClassDao = db.scheduledClassDao()
    )
    val authManager = AdminAuthManager(application)
    val firestoreSyncService = FirestoreSyncService(application)

    val isLocked: StateFlow<Boolean> = authManager.isLocked
    val isCloudConfigured = MutableStateFlow(firestoreSyncService.isFirebaseConfigured())
    val cloudSyncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)

    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<FeePayment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledClasses: StateFlow<List<ScheduledClass>> = repository.allScheduledClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter states
    val searchQuery = MutableStateFlow("")
    val feeStatusFilter = MutableStateFlow("ALL") // ALL, PENDING, PARTIAL, COMPLETED
    val courseFilter = MutableStateFlow<Long?>(null) // null = all courses

    // Revenue Visualization states
    val revenueTimeRange = MutableStateFlow(RevenueTimeRange.LAST_6_MONTHS)
    val chartMetricMode = MutableStateFlow(ChartMetricMode.COMPLETED_FEES)

    // Selected student for detail sheet
    val selectedStudent = MutableStateFlow<Student?>(null)

    // Attendance selected date and course
    val selectedAttendanceDate = MutableStateFlow(getTodayDateString())
    val selectedAttendanceCourseId = MutableStateFlow<Long?>(null)

    // Auth error message
    val authErrorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Filtered students flow
    val filteredStudents: StateFlow<List<Student>> = combine(
        students,
        searchQuery,
        feeStatusFilter,
        courseFilter
    ) { list, query, feeFilter, courseId ->
        list.filter { student ->
            val matchesQuery = query.isBlank() ||
                    student.name.contains(query, ignoreCase = true) ||
                    student.phone.contains(query, ignoreCase = true)

            val matchesFee = when (feeFilter) {
                "ALL" -> true
                "PENDING" -> student.feeStatus == "PENDING"
                "PARTIAL" -> student.feeStatus == "PARTIAL"
                "COMPLETED" -> student.feeStatus == "COMPLETED"
                else -> true
            }

            val matchesCourse = courseId == null || student.courseId == courseId

            matchesQuery && matchesFee && matchesCourse
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CRM Metrics
    val crmMetrics = combine(students, allAttendance, selectedAttendanceDate) { studentList, attendanceList, today ->
        val totalStudents = studentList.size
        val totalFeesExpected = studentList.sumOf { it.totalFee }
        val totalFeesCollected = studentList.sumOf { it.paidFee }
        val totalFeesPending = studentList.sumOf { it.pendingFee }
        val pendingFeeCount = studentList.count { it.feeStatus == "PENDING" || it.feeStatus == "PARTIAL" }
        val completedFeeCount = studentList.count { it.feeStatus == "COMPLETED" }

        val todayAttendance = attendanceList.filter { it.date == today }
        val presentCount = todayAttendance.count { it.status == "PRESENT" }
        val absentCount = todayAttendance.count { it.status == "ABSENT" }
        val lateCount = todayAttendance.count { it.status == "LATE" }
        val attendanceRate = if (totalStudents > 0) (presentCount.toFloat() / totalStudents * 100).toInt() else 0

        CrmMetrics(
            totalStudents = totalStudents,
            totalFeesExpected = totalFeesExpected,
            totalFeesCollected = totalFeesCollected,
            totalFeesPending = totalFeesPending,
            pendingCount = pendingFeeCount,
            completedCount = completedFeeCount,
            todayPresent = presentCount,
            todayAbsent = absentCount,
            todayLate = lateCount,
            attendanceRatePercent = attendanceRate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CrmMetrics())

    // Monthly Revenue Data Visualization Flow (aggregates completed student fees)
    val monthlyRevenueOverview: StateFlow<MonthlyRevenueOverview> = combine(
        allPayments,
        students,
        revenueTimeRange
    ) { payments, studentList, timeRange ->
        computeMonthlyRevenueOverview(payments, studentList, timeRange.monthCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyRevenueOverview())

    fun setRevenueTimeRange(range: RevenueTimeRange) {
        revenueTimeRange.value = range
    }

    fun setChartMetricMode(mode: ChartMetricMode) {
        chartMetricMode.value = mode
    }

    // Admin Auth Actions
    fun unlock(username: String, pass: String): Boolean {
        val success = authManager.authenticate(username, pass)
        if (!success) {
            authErrorMessage.value = "Invalid username or password. Default is admin1 / masterkey786"
        } else {
            authErrorMessage.value = null
        }
        return success
    }

    fun lock() {
        authManager.lock()
        authErrorMessage.value = null
    }

    fun updateAdminCredentials(newUsername: String, newPass: String): Boolean {
        return authManager.updateCredentials(newUsername, newPass)
    }

    fun resetAdminCredentials() {
        authManager.resetToDefaults()
    }

    // Student Actions
    fun addStudent(
        name: String,
        courseId: Long?,
        phone: String,
        email: String,
        address: String,
        totalFee: Double,
        initialPayment: Double,
        paymentMode: String,
        notes: String
    ) {
        viewModelScope.launch {
            val student = Student(
                courseId = courseId,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                totalFee = totalFee,
                notes = notes.trim()
            )
            val newId = repository.insertStudent(
                student = student,
                initialPayment = initialPayment,
                paymentMode = paymentMode
            )
            val savedStudent = student.copy(id = newId)
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.saveStudent(savedStudent)
            }
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
            if (selectedStudent.value?.id == student.id) {
                selectedStudent.value = student
            }
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.saveStudent(student)
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            if (selectedStudent.value?.id == student.id) {
                selectedStudent.value = null
            }
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.deleteStudent(student.id)
            }
        }
    }

    // Course Actions
    fun addCourse(
        name: String,
        subject: String,
        feeAmount: Double,
        timingSchedule: String,
        description: String
    ) {
        viewModelScope.launch {
            val course = Course(
                name = name.trim(),
                subject = subject.trim(),
                feeAmount = feeAmount,
                timingSchedule = timingSchedule.trim(),
                description = description.trim()
            )
            val newId = repository.insertCourse(course)
            val savedCourse = course.copy(id = newId)
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.saveCourse(savedCourse)
            }
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.saveCourse(course)
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.deleteCourse(course.id)
            }
        }
    }

    // Timetable / Schedule Actions
    fun addScheduledClass(
        courseId: Long,
        courseName: String,
        date: String,
        startTime: String,
        endTime: String,
        topic: String,
        room: String,
        instructor: String,
        classType: String = "Lecture"
    ) {
        viewModelScope.launch {
            val scheduledClass = ScheduledClass(
                courseId = courseId,
                courseName = courseName.trim(),
                date = date.trim(),
                startTime = startTime.trim(),
                endTime = endTime.trim(),
                topic = topic.trim(),
                room = room.trim().ifBlank { "Hall A" },
                instructor = instructor.trim().ifBlank { "Faculty Staff" },
                classType = classType.trim().ifBlank { "Lecture" }
            )
            repository.insertScheduledClass(scheduledClass)
        }
    }

    fun deleteScheduledClass(scheduledClass: ScheduledClass) {
        viewModelScope.launch {
            repository.deleteScheduledClass(scheduledClass)
        }
    }

    // Fee Payment Actions
    fun recordPayment(
        studentId: Long,
        amount: Double,
        paymentMode: String,
        receiptNo: String,
        notes: String
    ) {
        viewModelScope.launch {
            val paymentId = repository.recordFeePayment(studentId, amount, paymentMode, receiptNo, notes)
            // Refresh selected student if open
            students.value.find { it.id == studentId }?.let {
                selectedStudent.value = it
                if (firestoreSyncService.isFirebaseConfigured()) {
                    firestoreSyncService.saveStudent(it)
                }
            }
            if (firestoreSyncService.isFirebaseConfigured()) {
                val payment = FeePayment(
                    id = paymentId,
                    studentId = studentId,
                    amount = amount,
                    paymentMode = paymentMode,
                    receiptNo = receiptNo,
                    notes = notes
                )
                firestoreSyncService.saveFeePayment(payment)
            }
        }
    }

    fun deletePayment(payment: FeePayment) {
        viewModelScope.launch {
            repository.deleteFeePayment(payment)
            if (firestoreSyncService.isFirebaseConfigured()) {
                firestoreSyncService.deleteFeePayment(payment.id)
            }
        }
    }

    fun getPaymentsForStudent(studentId: Long) = repository.getPaymentsForStudent(studentId)

    fun getAttendanceForStudent(studentId: Long) = repository.getAttendanceForStudent(studentId)

    // Attendance Actions
    fun markAttendance(
        studentId: Long,
        courseId: Long,
        date: String,
        status: String,
        remark: String = ""
    ) {
        viewModelScope.launch {
            repository.markAttendance(studentId, courseId, date, status, remark)
            if (firestoreSyncService.isFirebaseConfigured()) {
                val att = Attendance(
                    studentId = studentId,
                    courseId = courseId,
                    date = date,
                    status = status,
                    remark = remark
                )
                firestoreSyncService.saveAttendance(att)
            }
        }
    }

    fun markAllPresentForCourse(courseId: Long, date: String) {
        viewModelScope.launch {
            val courseStudents = students.value.filter { it.courseId == courseId }
            repository.markAllPresent(courseStudents, courseId, date)
            if (firestoreSyncService.isFirebaseConfigured()) {
                val attList = courseStudents.map { s ->
                    Attendance(
                        studentId = s.id,
                        courseId = courseId,
                        date = date,
                        status = "PRESENT"
                    )
                }
                firestoreSyncService.saveAttendanceBatch(attList)
            }
        }
    }

    // Cloud Sync Full Actions
    fun syncAllToCloud() {
        viewModelScope.launch {
            cloudSyncState.value = CloudSyncState.Syncing
            val result = firestoreSyncService.syncAllToCloud(
                students = students.value,
                courses = courses.value,
                attendances = allAttendance.value,
                payments = allPayments.value
            )
            cloudSyncState.value = result
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            cloudSyncState.value = CloudSyncState.Syncing
            try {
                val remoteCourses = firestoreSyncService.fetchCoursesFromCloud()
                val remoteStudents = firestoreSyncService.fetchStudentsFromCloud()
                val remoteAttendance = firestoreSyncService.fetchAttendanceFromCloud()

                for (course in remoteCourses) {
                    repository.insertCourse(course)
                }
                for (student in remoteStudents) {
                    repository.updateStudent(student)
                }
                for (att in remoteAttendance) {
                    repository.markAttendance(att.studentId, att.courseId, att.date, att.status, att.remark)
                }
                cloudSyncState.value = CloudSyncState.Success(
                    "Restored ${remoteStudents.size} students, ${remoteCourses.size} courses, and ${remoteAttendance.size} attendance logs from Firestore."
                )
            } catch (e: Exception) {
                cloudSyncState.value = CloudSyncState.Error("Restore failed: ${e.message}")
            }
        }
    }

    fun dismissCloudSyncState() {
        cloudSyncState.value = CloudSyncState.Idle
    }

    private fun computeMonthlyRevenueOverview(
        payments: List<FeePayment>,
        studentList: List<Student>,
        monthCount: Int
    ): MonthlyRevenueOverview {
        val studentsById = studentList.associateBy { it.id }
        val yyyyMmFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthShortFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthFullFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val currentCal = Calendar.getInstance()
        val currentMonthKey = yyyyMmFormat.format(currentCal.time)

        val targetMonths = mutableListOf<Triple<String, String, String>>()
        for (i in (monthCount - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val key = yyyyMmFormat.format(cal.time)
            val shortName = monthShortFormat.format(cal.time)
            val fullName = monthFullFormat.format(cal.time)
            targetMonths.add(Triple(key, shortName, fullName))
        }

        val paymentsByMonth = payments.groupBy { yyyyMmFormat.format(Date(it.paymentDate)) }

        val items = targetMonths.map { (monthKey, shortName, fullName) ->
            val monthPayments = paymentsByMonth[monthKey] ?: emptyList()
            val completedPayments = monthPayments.filter { payment ->
                val student = studentsById[payment.studentId]
                student?.feeStatus == "COMPLETED"
            }
            val completedRev = completedPayments.sumOf { it.amount }
            val totalRev = monthPayments.sumOf { it.amount }
            val distinctCompletedStudents = completedPayments.map { it.studentId }.distinct().size

            MonthlyRevenueItem(
                yearMonth = monthKey,
                monthShort = shortName,
                monthFull = fullName,
                completedRevenue = completedRev,
                totalRevenue = totalRev,
                completedStudentsCount = distinctCompletedStudents,
                totalPaymentsCount = monthPayments.size,
                isCurrentMonth = monthKey == currentMonthKey
            )
        }

        val totalCompletedRevenue = items.sumOf { it.completedRevenue }
        val totalAllRevenue = items.sumOf { it.totalRevenue }
        val avgCompleted = if (items.isNotEmpty()) totalCompletedRevenue / items.size else 0.0
        val peakItem = items.maxByOrNull { it.completedRevenue }
        val lowestItem = items.filter { it.completedRevenue > 0 }.minByOrNull { it.completedRevenue }

        val growthRate = if (items.size >= 2) {
            val curr = items.last().completedRevenue
            val prev = items[items.size - 2].completedRevenue
            if (prev > 0) {
                ((curr - prev) / prev) * 100.0
            } else if (curr > 0) 100.0 else 0.0
        } else 0.0

        return MonthlyRevenueOverview(
            items = items,
            totalCompletedRevenue = totalCompletedRevenue,
            totalAllRevenue = totalAllRevenue,
            averageMonthlyCompletedRevenue = avgCompleted,
            peakMonthItem = peakItem,
            lowestMonthItem = lowestItem,
            currentMonthGrowthRate = growthRate
        )
    }

    companion object {
        fun getTodayDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }
}

data class CrmMetrics(
    val totalStudents: Int = 0,
    val totalFeesExpected: Double = 0.0,
    val totalFeesCollected: Double = 0.0,
    val totalFeesPending: Double = 0.0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val todayLate: Int = 0,
    val attendanceRatePercent: Int = 0
)
