package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.data.model.MonthlyRevenueOverview
import com.example.data.model.Student
import com.example.ui.theme.StatusPaidGreen
import com.example.util.ExportReportManager
import com.example.util.ExportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

enum class ExportReportType {
    PDF_AUDIT,
    CSV_SPREADSHEET
}

@Composable
fun ExportReportDialog(
    overview: MonthlyRevenueOverview,
    payments: List<FeePayment>,
    students: List<Student>,
    courses: List<Course>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportReportType.PDF_AUDIT) }
    var isGenerating by remember { mutableStateOf(false) }
    var exportSuccessResult by remember { mutableStateOf<ExportResult?>(null) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Export Fee Records",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Structured monthly data export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scope Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "DATA SUMMARY TO EXPORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Periods:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${overview.items.size} Months aggregated",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Completed Fees:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(overview.totalCompletedRevenue),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusPaidGreen
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Ledger Receipts:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${payments.size} transactions (${currencyFormatter.format(overview.totalAllRevenue)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Format Selection Heading
                Text(
                    text = "Select Export Format",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // PDF Option Card
                ExportFormatOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = Color(0xFFDC2626), // PDF Red
                    title = "PDF Audit Document (.pdf)",
                    description = "Official executive report with KPI cards, monthly revenue tables, and full payment ledger.",
                    isSelected = selectedFormat == ExportReportType.PDF_AUDIT,
                    onClick = { selectedFormat = ExportReportType.PDF_AUDIT },
                    testTag = "export_option_pdf"
                )

                // CSV Option Card
                ExportFormatOptionCard(
                    icon = Icons.Default.TableChart,
                    iconTint = Color(0xFF16A34A), // Excel Green
                    title = "CSV Spreadsheet (.csv)",
                    description = "Raw structured data compatible with Microsoft Excel, Google Sheets, or external accounting software.",
                    isSelected = selectedFormat == ExportReportType.CSV_SPREADSHEET,
                    onClick = { selectedFormat = ExportReportType.CSV_SPREADSHEET },
                    testTag = "export_option_csv"
                )

                if (exportSuccessResult != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusPaidGreen.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusPaidGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${exportSuccessResult?.format} generated successfully (${exportSuccessResult?.file?.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusPaidGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportSuccessResult != null) {
                Button(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent.createChooser(
                                    exportSuccessResult!!.shareIntent,
                                    "Share / Save ${exportSuccessResult!!.format} Report"
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error launching share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("share_exported_file_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Save File")
                }
            } else {
                Button(
                    onClick = {
                        isGenerating = true
                        coroutineScope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    if (selectedFormat == ExportReportType.PDF_AUDIT) {
                                        ExportReportManager.exportFeePaymentsPdf(
                                            context = context,
                                            overview = overview,
                                            payments = payments,
                                            students = students,
                                            courses = courses
                                        )
                                    } else {
                                        ExportReportManager.exportFeePaymentsCsv(
                                            context = context,
                                            overview = overview,
                                            payments = payments,
                                            students = students,
                                            courses = courses
                                        )
                                    }
                                }
                                exportSuccessResult = result
                                isGenerating = false

                                // Automatically open chooser
                                try {
                                    context.startActivity(
                                        Intent.createChooser(
                                            result.shareIntent,
                                            "Share / Save ${result.format} Report"
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export saved to: ${result.file.name}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                isGenerating = false
                                Toast.makeText(context, "Failed to export: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("confirm_export_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Export")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating,
                modifier = Modifier.testTag("dismiss_export_button")
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ExportFormatOptionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) CardDefaults.outlinedCardBorder() else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}
