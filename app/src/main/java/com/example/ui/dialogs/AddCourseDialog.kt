package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Course

@Composable
fun AddCourseDialog(
    editingCourse: Course? = null,
    onDismiss: () -> Unit,
    onSaveCourse: (
        name: String,
        subject: String,
        feeAmount: Double,
        timingSchedule: String,
        description: String
    ) -> Unit,
    onUpdateCourse: ((Course) -> Unit)? = null
) {
    var name by remember { mutableStateOf(editingCourse?.name ?: "") }
    var subject by remember { mutableStateOf(editingCourse?.subject ?: "") }
    var feeAmountStr by remember {
        mutableStateOf(if (editingCourse != null) editingCourse.feeAmount.toInt().toString() else "")
    }
    var timingSchedule by remember { mutableStateOf(editingCourse?.timingSchedule ?: "") }
    var description by remember { mutableStateOf(editingCourse?.description ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingCourse != null) "Edit Tuition Course" else "Create New Tuition Course",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Course / Batch Name *") },
                    placeholder = { Text("e.g. Class 10 - Mathematics") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        errorMessage = null
                    },
                    label = { Text("Subject *") },
                    placeholder = { Text("e.g. Mathematics, Physics, Chemistry") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_subject_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = feeAmountStr,
                    onValueChange = {
                        feeAmountStr = it
                        errorMessage = null
                    },
                    label = { Text("Standard Course Fee (₹ / $) *") },
                    placeholder = { Text("e.g. 2500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_fee_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = timingSchedule,
                    onValueChange = { timingSchedule = it },
                    label = { Text("Timing & Days Schedule") },
                    placeholder = { Text("e.g. Mon, Wed, Fri (4:00 PM - 5:30 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Course Description (Optional)") },
                    placeholder = { Text("Board syllabus, chapters covered, etc.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter course name."
                        return@Button
                    }
                    if (subject.isBlank()) {
                        errorMessage = "Please enter subject."
                        return@Button
                    }
                    val fee = feeAmountStr.toDoubleOrNull() ?: 0.0
                    if (fee <= 0) {
                        errorMessage = "Please enter a valid course fee."
                        return@Button
                    }

                    if (editingCourse != null && onUpdateCourse != null) {
                        onUpdateCourse(
                            editingCourse.copy(
                                name = name.trim(),
                                subject = subject.trim(),
                                feeAmount = fee,
                                timingSchedule = timingSchedule.trim(),
                                description = description.trim()
                            )
                        )
                    } else {
                        onSaveCourse(
                            name.trim(),
                            subject.trim(),
                            fee,
                            timingSchedule.trim(),
                            description.trim()
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("save_course_button")
            ) {
                Text(if (editingCourse != null) "Update Course" else "Create Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
