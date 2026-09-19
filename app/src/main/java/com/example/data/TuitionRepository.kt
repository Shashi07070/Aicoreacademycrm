package com.example.data

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TuitionRepository(
    private val courseDao: CourseDao,
    private val studentDao: StudentDao,
    private val feePaymentDao: FeePaymentDao,
    private val attendanceDao: AttendanceDao,
    private val scheduledClassDao: ScheduledClassDao
) {
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val allPayments: Flow<List<FeePayment>> = feePaymentDao.getAllPayments()
    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()
    val allScheduledClasses: Flow<List<ScheduledClass>> = scheduledClassDao.getAllClasses()

    fun getClassesForDate(date: String): Flow<List<ScheduledClass>> = scheduledClassDao.getClassesForDate(date)

    suspend fun insertScheduledClass(scheduledClass: ScheduledClass): Long = withContext(Dispatchers.IO) {
        scheduledClassDao.insertClass(scheduledClass)
    }

    suspend fun updateScheduledClass(scheduledClass: ScheduledClass) = withContext(Dispatchers.IO) {
        scheduledClassDao.updateClass(scheduledClass)
    }

    suspend fun deleteScheduledClass(scheduledClass: ScheduledClass) = withContext(Dispatchers.IO) {
        scheduledClassDao.deleteClass(scheduledClass)
    }

    fun getStudentById(id: Long): Flow<Student?> = studentDao.getStudentById(id)
    fun getCourseById(id: Long): Flow<Course?> = courseDao.getCourseById(id)
    fun getPaymentsForStudent(studentId: Long): Flow<List<FeePayment>> = feePaymentDao.getPaymentsForStudent(studentId)
    fun getAttendanceForStudent(studentId: Long): Flow<List<Attendance>> = attendanceDao.getAttendanceForStudent(studentId)
    fun getAttendanceForDateAndCourse(date: String, courseId: Long): Flow<List<Attendance>> =
        attendanceDao.getAttendanceForDateAndCourse(date, courseId)
    fun getAttendanceForDate(date: String): Flow<List<Attendance>> = attendanceDao.getAttendanceForDate(date)

    suspend fun insertCourse(course: Course): Long = withContext(Dispatchers.IO) {
        courseDao.insertCourse(course)
    }

    suspend fun updateCourse(course: Course) = withContext(Dispatchers.IO) {
        courseDao.updateCourse(course)
    }

    suspend fun deleteCourse(course: Course) = withContext(Dispatchers.IO) {
        courseDao.deleteCourse(course)
    }

    suspend fun insertStudent(
        student: Student,
        initialPayment: Double = 0.0,
        paymentMode: String = "Cash",
        receiptNo: String = "",
        paymentDate: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val studentId = studentDao.insertStudent(student)
        if (initialPayment > 0) {
            val payment = FeePayment(
                studentId = studentId,
                amount = initialPayment,
                paymentDate = paymentDate,
                paymentMode = paymentMode,
                receiptNo = receiptNo.ifBlank { "REC-${System.currentTimeMillis() % 100000}" },
                notes = "Initial admission payment"
            )
            feePaymentDao.insertPayment(payment)
            recalculateAndUpdateStudentFee(studentId, student.totalFee)
        }
        studentId
    }

    suspend fun updateStudent(student: Student) = withContext(Dispatchers.IO) {
        studentDao.updateStudent(student)
        recalculateAndUpdateStudentFee(student.id, student.totalFee)
    }

    suspend fun deleteStudent(student: Student) = withContext(Dispatchers.IO) {
        studentDao.deleteStudent(student)
    }

    suspend fun recordFeePayment(
        studentId: Long,
        amount: Double,
        paymentMode: String,
        receiptNo: String,
        notes: String,
        paymentDate: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val payment = FeePayment(
            studentId = studentId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMode = paymentMode,
            receiptNo = receiptNo.ifBlank { "REC-${System.currentTimeMillis() % 100000}" },
            notes = notes
        )
        val paymentId = feePaymentDao.insertPayment(payment)
        val student = studentDao.getStudentByIdImmediate(studentId)
        if (student != null) {
            recalculateAndUpdateStudentFee(studentId, student.totalFee)
        }
        paymentId
    }

    suspend fun deleteFeePayment(payment: FeePayment) = withContext(Dispatchers.IO) {
        feePaymentDao.deletePayment(payment)
        val student = studentDao.getStudentByIdImmediate(payment.studentId)
        if (student != null) {
            recalculateAndUpdateStudentFee(student.id, student.totalFee)
        }
    }

    private suspend fun recalculateAndUpdateStudentFee(studentId: Long, totalFee: Double) {
        val totalPaid = feePaymentDao.getTotalPaidForStudent(studentId) ?: 0.0
        val status = when {
            totalPaid <= 0.0 -> "PENDING"
            totalPaid >= totalFee -> "COMPLETED"
            else -> "PARTIAL"
        }
        studentDao.updateStudentPaymentState(studentId, totalPaid, status)
    }

    suspend fun markAttendance(
        studentId: Long,
        courseId: Long,
        date: String,
        status: String,
        remark: String = ""
    ) = withContext(Dispatchers.IO) {
        val attendance = Attendance(
            studentId = studentId,
            courseId = courseId,
            date = date,
            status = status,
            remark = remark
        )
        attendanceDao.insertOrUpdateAttendance(attendance)
    }

    suspend fun markAllPresent(
        students: List<Student>,
        courseId: Long,
        date: String
    ) = withContext(Dispatchers.IO) {
        val attendances = students.map { s ->
            Attendance(
                studentId = s.id,
                courseId = courseId,
                date = date,
                status = "PRESENT"
            )
        }
        attendanceDao.insertAll(attendances)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (courseDao.getCount() == 0 && studentDao.getCount() == 0) {
            val course1Id = courseDao.insertCourse(
                Course(
                    name = "Class 10 - Mathematics",
                    subject = "Mathematics",
                    feeAmount = 2500.0,
                    timingSchedule = "Mon, Wed, Fri (4:00 PM - 5:30 PM)",
                    description = "CBSE & ICSE Board Syllabus, Algebra, Geometry, Trigonometry"
                )
            )
            val course2Id = courseDao.insertCourse(
                Course(
                    name = "Class 12 - Physics",
                    subject = "Physics",
                    feeAmount = 3200.0,
                    timingSchedule = "Tue, Thu, Sat (5:30 PM - 7:00 PM)",
                    description = "Electrodynamics, Optics, Modern Physics & Numerical Practice"
                )
            )
            val course3Id = courseDao.insertCourse(
                Course(
                    name = "Science & Biology Foundation",
                    subject = "Biology",
                    feeAmount = 2000.0,
                    timingSchedule = "Sat, Sun (10:00 AM - 12:00 PM)",
                    description = "Medical foundation concepts and board preparation"
                )
            )

            // Helper to get past month timestamps
            fun getMonthTime(monthsAgo: Int): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -monthsAgo)
                cal.set(Calendar.DAY_OF_MONTH, 12)
                return cal.timeInMillis
            }

            // Seed Students with historical completed fees across past 6 months
            insertStudent(
                Student(
                    courseId = course1Id,
                    name = "Rahul Mehta",
                    phone = "+91 98111 22334",
                    email = "rahul.m@example.com",
                    address = "Sector 21, Gandhinagar",
                    totalFee = 2500.0,
                    paidFee = 2500.0,
                    feeStatus = "COMPLETED",
                    notes = "Batch topper in Class 10 Math",
                    enrollmentDate = getMonthTime(5)
                ),
                initialPayment = 2500.0,
                paymentMode = "UPI",
                receiptNo = "REC-901",
                paymentDate = getMonthTime(5)
            )

            insertStudent(
                Student(
                    courseId = course2Id,
                    name = "Sneha Reddy",
                    phone = "+91 97222 33445",
                    email = "sneha.r@example.com",
                    address = "Jubilee Hills, Road 36",
                    totalFee = 3200.0,
                    paidFee = 3200.0,
                    feeStatus = "COMPLETED",
                    notes = "Physics competitive prep completed",
                    enrollmentDate = getMonthTime(4)
                ),
                initialPayment = 3200.0,
                paymentMode = "Bank Transfer",
                receiptNo = "REC-902",
                paymentDate = getMonthTime(4)
            )

            insertStudent(
                Student(
                    courseId = course3Id,
                    name = "Vikram Joshi",
                    phone = "+91 98333 44556",
                    email = "vikram.j@example.com",
                    address = "Shivaji Nagar, Pune",
                    totalFee = 2000.0,
                    paidFee = 2000.0,
                    feeStatus = "COMPLETED",
                    notes = "Full course fee cleared",
                    enrollmentDate = getMonthTime(3)
                ),
                initialPayment = 2000.0,
                paymentMode = "Cash",
                receiptNo = "REC-903",
                paymentDate = getMonthTime(3)
            )

            insertStudent(
                Student(
                    courseId = course1Id,
                    name = "Tanvi Shah",
                    phone = "+91 98444 55667",
                    email = "tanvi.s@example.com",
                    address = "Ellisbridge, Ahmedabad",
                    totalFee = 2500.0,
                    paidFee = 2500.0,
                    feeStatus = "COMPLETED",
                    notes = "Completed fee in full",
                    enrollmentDate = getMonthTime(2)
                ),
                initialPayment = 2500.0,
                paymentMode = "UPI",
                receiptNo = "REC-904",
                paymentDate = getMonthTime(2)
            )

            val s1 = insertStudent(
                Student(
                    courseId = course1Id,
                    name = "Aarav Sharma",
                    phone = "+91 98765 43210",
                    email = "aarav.sharma@example.com",
                    address = "Flat 402, Sunshine Heights",
                    totalFee = 2500.0,
                    paidFee = 2500.0,
                    feeStatus = "COMPLETED",
                    notes = "Excellent student, regular in class",
                    enrollmentDate = getMonthTime(1)
                ),
                initialPayment = 2500.0,
                paymentMode = "UPI",
                receiptNo = "REC-1001",
                paymentDate = getMonthTime(1)
            )

            val s2 = insertStudent(
                Student(
                    courseId = course1Id,
                    name = "Priya Verma",
                    phone = "+91 98234 56789",
                    email = "priya.verma@example.com",
                    address = "Sector 14, Green Park",
                    totalFee = 2500.0,
                    paidFee = 1000.0,
                    feeStatus = "PARTIAL",
                    notes = "First installment paid, second due next week",
                    enrollmentDate = getMonthTime(1)
                ),
                initialPayment = 1000.0,
                paymentMode = "Cash",
                receiptNo = "REC-1002",
                paymentDate = getMonthTime(1)
            )

            val s3 = insertStudent(
                Student(
                    courseId = course2Id,
                    name = "Rohan Kulkarni",
                    phone = "+91 97123 45678",
                    email = "rohan.k@example.com",
                    address = "B-12, Royal Enclave",
                    totalFee = 3200.0,
                    paidFee = 0.0,
                    feeStatus = "PENDING",
                    notes = "Admission pending fee confirmation"
                )
            )

            val s4 = insertStudent(
                Student(
                    courseId = course3Id,
                    name = "Ananya Patel",
                    phone = "+91 99887 76655",
                    email = "ananya.p@example.com",
                    address = "Villa 7, Palm Meadows",
                    totalFee = 2000.0,
                    paidFee = 2000.0,
                    feeStatus = "COMPLETED",
                    notes = "Paid in full via Bank Transfer"
                ),
                initialPayment = 2000.0,
                paymentMode = "Bank Transfer",
                receiptNo = "REC-1003",
                paymentDate = System.currentTimeMillis()
            )

            val s5 = insertStudent(
                Student(
                    courseId = course2Id,
                    name = "Devendra Singh",
                    phone = "+91 94567 12345",
                    email = "dev.singh@example.com",
                    address = "Block C, Metro Residency",
                    totalFee = 3200.0,
                    paidFee = 1500.0,
                    feeStatus = "PARTIAL",
                    notes = "Partial payment received"
                ),
                initialPayment = 1500.0,
                paymentMode = "Cash",
                receiptNo = "REC-1004",
                paymentDate = System.currentTimeMillis()
            )

            // Seed today's attendance
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            markAttendance(s1, course1Id, todayStr, "PRESENT")
            markAttendance(s2, course1Id, todayStr, "PRESENT")
            markAttendance(s3, course2Id, todayStr, "ABSENT", "Informed sick leave")
            markAttendance(s4, course3Id, todayStr, "PRESENT")
            markAttendance(s5, course2Id, todayStr, "LATE", "Arrived 15m late")

            // Seed scheduled classes for Timetable
            val cal = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            cal.time = Date()
            val classDate1 = dateFormat.format(cal.time)
            
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val classDate2 = dateFormat.format(cal.time)
            
            cal.add(Calendar.DAY_OF_MONTH, 2)
            val classDate3 = dateFormat.format(cal.time)

            insertScheduledClass(
                ScheduledClass(
                    courseId = course1Id,
                    courseName = "Deep Learning & Neural Networks",
                    date = classDate1,
                    dayOfWeek = "Today",
                    startTime = "09:30 AM",
                    endTime = "11:00 AM",
                    topic = "Backpropagation & Gradient Descent Algorithms",
                    room = "Lab Alpha-101",
                    instructor = "Dr. Alan Vance",
                    classType = "Lecture"
                )
            )
            insertScheduledClass(
                ScheduledClass(
                    courseId = course2Id,
                    courseName = "Advanced Mathematics for ML",
                    date = classDate1,
                    dayOfWeek = "Today",
                    startTime = "02:00 PM",
                    endTime = "03:30 PM",
                    topic = "Linear Algebra: Eigenvectors & SVD Decomposition",
                    room = "Seminar Room B",
                    instructor = "Prof. Elena Rostova",
                    classType = "Workshop"
                )
            )
            insertScheduledClass(
                ScheduledClass(
                    courseId = course3Id,
                    courseName = "Python for Data Engineering",
                    date = classDate2,
                    dayOfWeek = "Tomorrow",
                    startTime = "10:00 AM",
                    endTime = "12:00 PM",
                    topic = "Distributed Systems & PySpark Pipeline Optimization",
                    room = "Hall C-204",
                    instructor = "Devendra Singh",
                    classType = "Lab"
                )
            )
            insertScheduledClass(
                ScheduledClass(
                    courseId = course1Id,
                    courseName = "Deep Learning & Neural Networks",
                    date = classDate3,
                    dayOfWeek = "Upcoming",
                    startTime = "11:30 AM",
                    endTime = "01:00 PM",
                    topic = "Transformer Architectures & Attention Mechanisms",
                    room = "Lab Alpha-101",
                    instructor = "Dr. Alan Vance",
                    classType = "Lecture"
                )
            )
        }
    }
}
