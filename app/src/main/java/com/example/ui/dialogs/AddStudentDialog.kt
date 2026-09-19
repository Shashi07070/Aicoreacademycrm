package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.example.data.model.Student

@Composable
fun AddStudentDialog(
    courses: List<Course>,
    editingStudent: Student? = null,
    onDismiss: () -> Unit,
    onSaveStudent: (
        name: String,
        courseId: Long?,
        phone: String,
        email: String,
        address: String,
        totalFee: Double,
        initialPayment: Double,
        paymentMode: String,
        notes: String
    ) -> Unit,
    onUpdateStudent: ((Student) -> Unit)? = null
) {
    var name by remember { mutableStateOf(editingStudent?.name ?: "") }
    var selectedCourseId by remember {
        mutableStateOf(editingStudent?.courseId ?: courses.firstOrNull()?.id)
    }
    var phone by remember { mutableStateOf(editingStudent?.phone ?: "") }
    var email by remember { mutableStateOf(editingStudent?.email ?: "") }
    var address by remember { mutableStateOf(editingStudent?.address ?: "") }
    var totalFeeStr by remember {
        val initialFee = editingStudent?.totalFee
            ?: courses.find { it.id == selectedCourseId }?.feeAmount
            ?: 0.0
        mutableStateOf(if (initialFee > 0) initialFee.toInt().toString() else "")
    }
    var initialPaymentStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf(editingStudent?.notes ?: "") }

    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var paymentModeDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val paymentModes = listOf("Cash", "UPI", "Bank Transfer", "Cheque")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingStudent != null) "Edit Student Profile" else "Enroll New Student",
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
                // Student Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Student Full Name *") },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Course Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentCourseName = courses.find { it.id == selectedCourseId }?.name ?: "Select Course *"
                    OutlinedTextField(
                        value = currentCourseName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course / Batch *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { courseDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.name} (${course.subject})") },
                                onClick = {
                                    selectedCourseId = course.id
                                    // Autofill standard fee if new student
                                    if (editingStudent == null && totalFeeStr.isBlank()) {
                                        totalFeeStr = course.feeAmount.toInt().toString()
                                    }
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Phone Contact
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorMessage = null
                    },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("+91 98765 43210") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    placeholder = { Text("student@gmail.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Total Course Fee
                OutlinedTextField(
                    value = totalFeeStr,
                    onValueChange = {
                        totalFeeStr = it
                        errorMessage = null
                    },
                    label = { Text("Total Course Fee (₹ / $) *") },
                    placeholder = { Text("2500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_fee_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Initial Payment (Only shown on new enrollment)
                if (editingStudent == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = initialPaymentStr,
                            onValueChange = { initialPaymentStr = it },
                            label = { Text("Initial Payment") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = paymentMode,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mode") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { paymentModeDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = paymentModeDropdownExpanded,
                                onDismissRequest = { paymentModeDropdownExpanded = false }
                            ) {
                                paymentModes.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode) },
                                        onClick = {
                                            paymentMode = mode
                                            paymentModeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Parent Details (Optional)") },
                    placeholder = { Text("e.g. Attending weekend batches") },
                    maxLines = 2,
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
                        errorMessage = "Please enter student name."
                        return@Button
                    }
                    if (phone.isBlank()) {
                        errorMessage = "Please enter phone number."
                        return@Button
                    }
                    val totalFee = totalFeeStr.toDoubleOrNull() ?: 0.0

                    if (editingStudent != null && onUpdateStudent != null) {
                        onUpdateStudent(
                            editingStudent.copy(
                                name = name.trim(),
                                courseId = selectedCourseId,
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                totalFee = totalFee,
                                notes = notes.trim()
                            )
                        )
                    } else {
                        val initialPayment = initialPaymentStr.toDoubleOrNull() ?: 0.0
                        onSaveStudent(
                            name.trim(),
                            selectedCourseId,
                            phone.trim(),
                            email.trim(),
                            address.trim(),
                            totalFee,
                            initialPayment,
                            paymentMode,
                            notes.trim()
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("save_student_button")
            ) {
                Text(if (editingStudent != null) "Update Student" else "Enroll Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
