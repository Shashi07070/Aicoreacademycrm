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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Student
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPendingRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RecordFeeDialog(
    students: List<Student>,
    preselectedStudent: Student? = null,
    onDismiss: () -> Unit,
    onRecordFee: (
        studentId: Long,
        amount: Double,
        paymentMode: String,
        receiptNo: String,
        notes: String
    ) -> Unit
) {
    var selectedStudentId by remember {
        mutableStateOf(preselectedStudent?.id ?: students.firstOrNull()?.id)
    }

    val currentStudent = students.find { it.id == selectedStudentId }

    var amountStr by remember {
        val pending = currentStudent?.pendingFee ?: 0.0
        mutableStateOf(if (pending > 0) pending.toInt().toString() else "")
    }

    var paymentMode by remember { mutableStateOf("Cash") }
    var receiptNo by remember {
        mutableStateOf("REC-${System.currentTimeMillis() % 100000}")
    }
    var notes by remember { mutableStateOf("") }

    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var paymentModeDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val paymentModes = listOf("Cash", "UPI", "Bank Transfer", "Cheque")
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Tuition Fee Payment",
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
                // Student Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currentStudent?.name ?: "Select Student *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Student *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { studentDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(s.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Due: ${currencyFormatter.format(s.pendingFee)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (s.pendingFee > 0) StatusPendingRed else StatusPaidGreen
                                        )
                                    }
                                },
                                onClick = {
                                    selectedStudentId = s.id
                                    amountStr = if (s.pendingFee > 0) s.pendingFee.toInt().toString() else ""
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Student Balance Overview Card
                if (currentStudent != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Fee:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormatter.format(currentStudent.totalFee), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Already Paid:", style = MaterialTheme.typography.bodySmall)
                                Text(currencyFormatter.format(currentStudent.paidFee), fontWeight = FontWeight.Bold, color = StatusPaidGreen, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining Due:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    currencyFormatter.format(currentStudent.pendingFee),
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentStudent.pendingFee > 0) StatusPendingRed else StatusPaidGreen,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Payment Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMessage = null
                    },
                    label = { Text("Payment Amount (₹ / $) *") },
                    placeholder = { Text("e.g. 1000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fee_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick full amount shortcut
                if (currentStudent != null && currentStudent.pendingFee > 0) {
                    OutlinedButton(
                        onClick = {
                            amountStr = currentStudent.pendingFee.toInt().toString()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Fill Full Due (${currencyFormatter.format(currentStudent.pendingFee)})")
                    }
                }

                // Payment Mode Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paymentMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Mode *") },
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

                // Receipt / Reference Number
                OutlinedTextField(
                    value = receiptNo,
                    onValueChange = { receiptNo = it },
                    label = { Text("Receipt / Ref Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Note
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Payment Notes (Optional)") },
                    placeholder = { Text("e.g. Installment 2, UPI txn id 847294") },
                    singleLine = true,
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
                    val studentId = selectedStudentId
                    if (studentId == null) {
                        errorMessage = "Please select a student."
                        return@Button
                    }
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        errorMessage = "Please enter a valid amount."
                        return@Button
                    }

                    onRecordFee(
                        studentId,
                        amount,
                        paymentMode,
                        receiptNo.trim(),
                        notes.trim()
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_fee_button")
            ) {
                Text("Record & Generate Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
