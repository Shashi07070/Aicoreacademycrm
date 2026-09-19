package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Student
import com.example.ui.TuitionViewModel
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusPendingRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: TuitionViewModel,
    modifier: Modifier = Modifier
) {
    val currentDate by viewModel.selectedAttendanceDate.collectAsStateWithLifecycle()
    val selectedCourseId by viewModel.selectedAttendanceCourseId.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()

    var studentForRemark by remember { mutableStateOf<Student?>(null) }
    var remarkText by remember { mutableStateOf("") }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var dateDropdownExpanded by remember { mutableStateOf(false) }
    var showSavedBanner by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    val displayDate = try {
        val dateObj = dateFormat.parse(currentDate) ?: Date()
        displayDateFormat.format(dateObj)
    } catch (e: Exception) {
        currentDate
    }

    val studentsInScope = if (selectedCourseId != null) {
        students.filter { it.courseId == selectedCourseId }
    } else {
        students
    }

    val attendanceForDate = allAttendance.filter { it.date == currentDate }
    val attendanceMap = attendanceForDate.associateBy { it.studentId }

    val presentCount = studentsInScope.count { attendanceMap[it.id]?.status == "PRESENT" }
    val lateCount = studentsInScope.count { attendanceMap[it.id]?.status == "LATE" }
    val absentCount = studentsInScope.count { attendanceMap[it.id]?.status == "ABSENT" }
    val excusedCount = studentsInScope.count { attendanceMap[it.id]?.status == "EXCUSED" }
    val unmarkedCount = studentsInScope.count { attendanceMap[it.id] == null }

    val changeDay = { days: Int ->
        try {
            val cal = Calendar.getInstance()
            cal.time = dateFormat.parse(currentDate) ?: Date()
            cal.add(Calendar.DAY_OF_YEAR, days)
            viewModel.selectedAttendanceDate.value = dateFormat.format(cal.time)
        } catch (e: Exception) {
            viewModel.selectedAttendanceDate.value = TuitionViewModel.getTodayDateString()
        }
    }

    val selectedCourse = courses.find { it.id == selectedCourseId }
    val courseLabel = selectedCourse?.subject ?: "All Courses"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .testTag("attendance_screen")
    ) {
        // Top Filter Bar: Dropdowns for "Course" and "Date"
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dropdowns Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Course Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            onClick = { courseDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF9FAFB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "COURSE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = courseLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFF111827),
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Courses (${students.size} students)") },
                                onClick = {
                                    viewModel.selectedAttendanceCourseId.value = null
                                    courseDropdownExpanded = false
                                }
                            )
                            courses.forEach { c ->
                                val count = students.count { it.courseId == c.id }
                                DropdownMenuItem(
                                    text = { Text("${c.subject} ($count students)") },
                                    onClick = {
                                        viewModel.selectedAttendanceCourseId.value = c.id
                                        courseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Dropdown / Navigation
                    Box(modifier = Modifier.weight(1.1f)) {
                        OutlinedCard(
                            onClick = { dateDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF9FAFB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DATE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = displayDate,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color(0xFF111827),
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = BrandRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dateDropdownExpanded,
                            onDismissRequest = { dateDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Today (${TuitionViewModel.getTodayDateString()})") },
                                onClick = {
                                    viewModel.selectedAttendanceDate.value = TuitionViewModel.getTodayDateString()
                                    dateDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Yesterday") },
                                onClick = {
                                    changeDay(-1)
                                    dateDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Previous Day (◀)") },
                                onClick = {
                                    changeDay(-1)
                                    dateDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Next Day (▶)") },
                                onClick = {
                                    changeDay(1)
                                    dateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Primary red "Save Attendance" button
                Button(
                    onClick = {
                        showSavedBanner = true
                        coroutineScope.launch {
                            delay(2500)
                            showSavedBanner = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Attendance (${studentsInScope.size - unmarkedCount}/${studentsInScope.size} Marked)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    )
                }

                // Secondary bulk-action buttons & stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                studentsInScope.forEach { student ->
                                    viewModel.markAttendance(
                                        studentId = student.id,
                                        courseId = student.courseId ?: 0L,
                                        date = currentDate,
                                        status = "PRESENT"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "All Present",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF374151)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                studentsInScope.forEach { student ->
                                    viewModel.markAttendance(
                                        studentId = student.id,
                                        courseId = student.courseId ?: 0L,
                                        date = currentDate,
                                        status = "ABSENT"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = BrandRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF374151)
                            )
                        }
                    }

                    // Day stepper icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { changeDay(-1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4B5563)
                            )
                        }
                        IconButton(
                            onClick = { changeDay(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }
        }

        // Saved Confirmation Banner
        AnimatedVisibility(
            visible = showSavedBanner,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFFDCFCE7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attendance saved successfully for $displayDate",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    )
                }
            }
        }

        // Quick Stats Strip
        Surface(
            color = Color(0xFFF3F4F6),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttendanceSummaryTag("Present (P)", presentCount, Color(0xFF16A34A))
                AttendanceSummaryTag("Absent (A)", absentCount, BrandRed)
                AttendanceSummaryTag("Late (L)", lateCount, Color(0xFFD97706))
                AttendanceSummaryTag("Excused (E)", excusedCount, Color(0xFF2563EB))
            }
        }

        // Student List below
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (studentsInScope.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No students enrolled in this selection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                items(studentsInScope, key = { it.id }) { student ->
                    val course = courses.find { it.id == student.courseId }
                    val currentAttendance = attendanceMap[student.id]
                    val status = currentAttendance?.status

                    StudentAttendanceCard(
                        student = student,
                        courseName = course?.subject ?: "General",
                        currentStatus = status,
                        remark = currentAttendance?.remark ?: "",
                        onStatusSelected = { newStatus ->
                            viewModel.markAttendance(
                                studentId = student.id,
                                courseId = student.courseId ?: 0L,
                                date = currentDate,
                                status = newStatus,
                                remark = currentAttendance?.remark ?: ""
                            )
                        },
                        onAddRemark = {
                            studentForRemark = student
                            remarkText = currentAttendance?.remark ?: ""
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Remark Dialog
    if (studentForRemark != null) {
        AlertDialog(
            onDismissRequest = { studentForRemark = null },
            title = { Text("Attendance Note for ${studentForRemark?.name}") },
            text = {
                Column {
                    Text(
                        text = "Add note or reason for absence/late arrival/excuse:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        label = { Text("Note / Remark") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        studentForRemark?.let { s ->
                            val currentAttendance = attendanceMap[s.id]
                            viewModel.markAttendance(
                                studentId = s.id,
                                courseId = s.courseId ?: 0L,
                                date = currentDate,
                                status = currentAttendance?.status ?: "ABSENT",
                                remark = remarkText.trim()
                            )
                        }
                        studentForRemark = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForRemark = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StudentAttendanceCard(
    student: Student,
    courseName: String,
    currentStatus: String?,
    remark: String,
    onStatusSelected: (String) -> Unit,
    onAddRemark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x10000000)
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Initials avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "$courseName • ${student.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                IconButton(
                    onClick = onAddRemark,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Add remark",
                        tint = if (remark.isNotBlank()) BrandRed else Color(0xFF9CA3AF)
                    )
                }
            }

            if (remark.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Note: $remark",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Circular toggle buttons for P (Present), A (Absent), L (Late), and E (Excused)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // P: Present
                CircularAttendanceToggle(
                    letter = "P",
                    label = "Present",
                    isSelected = currentStatus == "PRESENT",
                    activeColor = Color(0xFF16A34A),
                    activeBgColor = Color(0xFF16A34A),
                    activeTextColor = Color.White,
                    onClick = { onStatusSelected("PRESENT") },
                    modifier = Modifier.weight(1f)
                )

                // A: Absent
                CircularAttendanceToggle(
                    letter = "A",
                    label = "Absent",
                    isSelected = currentStatus == "ABSENT",
                    activeColor = BrandRed,
                    activeBgColor = BrandRed,
                    activeTextColor = Color.White,
                    onClick = { onStatusSelected("ABSENT") },
                    modifier = Modifier.weight(1f)
                )

                // L: Late
                CircularAttendanceToggle(
                    letter = "L",
                    label = "Late",
                    isSelected = currentStatus == "LATE",
                    activeColor = Color(0xFFD97706),
                    activeBgColor = Color(0xFFD97706),
                    activeTextColor = Color.White,
                    onClick = { onStatusSelected("LATE") },
                    modifier = Modifier.weight(1f)
                )

                // E: Excused
                CircularAttendanceToggle(
                    letter = "E",
                    label = "Excused",
                    isSelected = currentStatus == "EXCUSED",
                    activeColor = Color(0xFF2563EB),
                    activeBgColor = Color(0xFF2563EB),
                    activeTextColor = Color.White,
                    onClick = { onStatusSelected("EXCUSED") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CircularAttendanceToggle(
    letter: String,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBgColor: Color,
    activeTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) activeBgColor else Color(0xFFF3F4F6)
                )
                .then(
                    if (!isSelected) {
                        Modifier.background(Color.Transparent)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) activeBgColor else Color(0xFFF3F4F6),
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) activeTextColor else Color(0xFF6B7280)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) activeColor else Color(0xFF6B7280)
        )
    }
}

@Composable
private fun AttendanceSummaryTag(
    label: String,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF374151)
        )
    }
}
