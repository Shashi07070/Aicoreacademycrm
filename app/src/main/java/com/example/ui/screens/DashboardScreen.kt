package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Student
import com.example.ui.TuitionViewModel
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedLight
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TuitionViewModel,
    onNavigateToStudents: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToFees: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onAddStudentClick: () -> Unit,
    onAddCourseClick: () -> Unit,
    onRecordFeeClick: (Student?) -> Unit,
    onStudentClick: (Student) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.crmMetrics.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()

    val pendingStudents = students.filter { it.feeStatus == "PENDING" || it.feeStatus == "PARTIAL" }
    val todayDateFormatted = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())

    val currencyFormatter = NumberFormat.getNumberInstance(Locale("en", "IN"))

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                Text(
                    text = todayDateFormatted.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF111827)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinimalActionButton(
                    label = "+ Student",
                    icon = Icons.Default.PersonAdd,
                    onClick = onAddStudentClick,
                    modifier = Modifier.weight(1f)
                )
                MinimalActionButton(
                    label = "+ Course",
                    icon = Icons.Default.MenuBook,
                    onClick = onAddCourseClick,
                    modifier = Modifier.weight(1f)
                )
                MinimalActionButton(
                    label = "+ Fee",
                    icon = Icons.Default.Payments,
                    onClick = { onRecordFeeClick(null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MinimalSummaryCard(
                        title = "Students",
                        value = "${metrics.totalStudents}",
                        subtext = "Enrolled",
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF3B82F6),
                        onClick = onNavigateToStudents,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalSummaryCard(
                        title = "Courses",
                        value = "${courses.size}",
                        subtext = "Active batches",
                        icon = Icons.Default.MenuBook,
                        iconTint = Color(0xFF8B5CF6),
                        onClick = onNavigateToCourses,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MinimalSummaryCard(
                        title = "Pending Fees",
                        value = "₹${currencyFormatter.format(metrics.totalFeesPending.toLong())}",
                        subtext = "${metrics.pendingCount} pending",
                        icon = Icons.Default.Warning,
                        iconTint = if (metrics.totalFeesPending > 0) BrandRed else Color(0xFF10B981),
                        onClick = onNavigateToFees,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalSummaryCard(
                        title = "Attendance",
                        value = "${metrics.todayPresent}",
                        subtext = "${metrics.attendanceRatePercent}% today",
                        icon = Icons.Default.EventNote,
                        iconTint = Color(0xFF10B981),
                        onClick = onNavigateToAttendance,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            val progress = if (metrics.totalFeesExpected > 0) {
                (metrics.totalFeesCollected / metrics.totalFeesExpected).toFloat().coerceIn(0f, 1f)
            } else 0f
            val percentage = (progress * 100).toInt()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFees() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fee Collection",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "$percentage% cleared",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF16A34A)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF16A34A),
                        trackColor = Color(0xFFF3F4F6)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Collected: ₹${currencyFormatter.format(metrics.totalFeesCollected.toLong())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            text = "Due: ₹${currencyFormatter.format(metrics.totalFeesPending.toLong())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (metrics.totalFeesPending > 0) BrandRed else Color(0xFF6B7280),
                            fontWeight = if (metrics.totalFeesPending > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (pendingStudents.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pending Dues (${pendingStudents.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151)
                    )
                    TextButton(onClick = onNavigateToFees) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandRed
                        )
                    }
                }
            }

            items(pendingStudents.take(2)) { student ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStudentClick(student) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Pending: ₹${currencyFormatter.format(student.pendingFee.toLong())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandRed
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MinimalActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF374151),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1F2937),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MinimalSummaryCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF6B7280)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF111827),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                maxLines = 1
            )
        }
    }
}
