package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Course
import com.example.data.model.FeePayment
import com.example.data.model.MonthlyRevenueItem
import com.example.data.model.MonthlyRevenueOverview
import com.example.data.model.Student
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val file: File,
    val shareIntent: Intent,
    val format: String,
    val recordCount: Int
)

object ExportReportManager {

    private fun getExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generates a well-structured CSV file containing:
     * 1. Executive Summary & Monthly Revenue Aggregates (Completed Student Fees vs Total)
     * 2. Detailed Fee Payment Transaction Records with Student & Course associations
     */
    fun exportFeePaymentsCsv(
        context: Context,
        overview: MonthlyRevenueOverview,
        payments: List<FeePayment>,
        students: List<Student>,
        courses: List<Course>
    ): ExportResult {
        val exportDir = getExportDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "Tuition_Monthly_Fee_Report_$timestamp.csv")

        val studentsById = students.associateBy { it.id }
        val coursesById = courses.associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        FileWriter(file).use { writer ->
            // --- HEADER METADATA ---
            writer.append("TUITION CRM - MONTHLY FEE & REVENUE AUDIT REPORT\n")
            writer.append("Generated On,${dateFormat.format(Date())}\n")
            writer.append("Total Revenue Completed (All-Time Tracked),₹${"%.2f".format(overview.totalCompletedRevenue)}\n")
            writer.append("Total Payments Collected,₹${"%.2f".format(overview.totalAllRevenue)}\n")
            writer.append("Monthly Average Completed,₹${"%.2f".format(overview.averageMonthlyCompletedRevenue)}\n")
            writer.append("Peak Month,${overview.peakMonthItem?.monthFull ?: "N/A"} (₹${overview.peakMonthItem?.completedRevenue ?: 0.0})\n\n")

            // --- SECTION 1: MONTHLY AGGREGATED SUMMARY ---
            writer.append("=== MONTHLY REVENUE AGGREGATES ===\n")
            writer.append("Year-Month,Month Name,Completed Student Revenue (INR),Total Collection (INR),Completed Students Count,Transaction Count,Current Month\n")
            overview.items.forEach { item ->
                writer.append("\"${escapeCsv(item.yearMonth)}\",")
                writer.append("\"${escapeCsv(item.monthFull)}\",")
                writer.append("${item.completedRevenue},")
                writer.append("${item.totalRevenue},")
                writer.append("${item.completedStudentsCount},")
                writer.append("${item.totalPaymentsCount},")
                writer.append("${if (item.isCurrentMonth) "YES" else "NO"}\n")
            }
            writer.append("\n")

            // --- SECTION 2: INDIVIDUAL FEE TRANSACTION LEDGER ---
            writer.append("=== INDIVIDUAL PAYMENT TRANSACTIONS ===\n")
            writer.append("Receipt No,Date,Student Name,Course Name,Student Phone,Payment Amount (INR),Payment Mode,Student Fee Status,Student Total Fee (INR),Student Paid Fee (INR),Student Balance (INR),Notes\n")

            // Sort payments by date descending
            val sortedPayments = payments.sortedByDescending { it.paymentDate }
            sortedPayments.forEach { payment ->
                val student = studentsById[payment.studentId]
                val course = student?.let { coursesById[it.courseId] }
                val studentName = student?.name ?: "Unknown Student"
                val courseName = course?.name ?: "General Batch"
                val studentPhone = student?.phone ?: "N/A"
                val feeStatus = student?.feeStatus ?: "N/A"
                val totalFee = student?.totalFee ?: 0.0
                val paidFee = student?.paidFee ?: 0.0
                val balance = if (student != null) (student.totalFee - student.paidFee).coerceAtLeast(0.0) else 0.0
                val dateStr = dateFormat.format(Date(payment.paymentDate))

                writer.append("\"${escapeCsv(payment.receiptNo)}\",")
                writer.append("\"${escapeCsv(dateStr)}\",")
                writer.append("\"${escapeCsv(studentName)}\",")
                writer.append("\"${escapeCsv(courseName)}\",")
                writer.append("\"${escapeCsv(studentPhone)}\",")
                writer.append("${payment.amount},")
                writer.append("\"${escapeCsv(payment.paymentMode)}\",")
                writer.append("\"${escapeCsv(feeStatus)}\",")
                writer.append("$totalFee,")
                writer.append("$paidFee,")
                writer.append("$balance,")
                writer.append("\"${escapeCsv(payment.notes)}\"\n")
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Tuition Monthly Fee Payment Report (CSV)")
            putExtra(Intent.EXTRA_TEXT, "Attached is the Tuition CRM Monthly Fee Payment & Revenue Export (.csv).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return ExportResult(
            file = file,
            shareIntent = shareIntent,
            format = "CSV",
            recordCount = payments.size
        )
    }

    /**
     * Generates a multi-page PDF document containing:
     * - Official Tuition CRM Header with company branding
     * - Executive Monthly Revenue Statistics & KPIs
     * - Monthly Aggregates Breakdown Table
     * - Detailed Recent Transaction Ledger
     */
    fun exportFeePaymentsPdf(
        context: Context,
        overview: MonthlyRevenueOverview,
        payments: List<FeePayment>,
        students: List<Student>,
        courses: List<Course>
    ): ExportResult {
        val exportDir = getExportDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "Tuition_Monthly_Fee_Report_$timestamp.pdf")

        val studentsById = students.associateBy { it.id }
        val coursesById = courses.associateBy { it.id }
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val shortDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val pdfDocument = PdfDocument()

        // Standard A4 dimensions in points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.rgb(25, 42, 86) // Dark Navy
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(80, 90, 110)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(33, 50, 100)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(240, 244, 255)
            style = Paint.Style.FILL
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableRowPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val tableRowBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val kpiBoxPaint = Paint().apply {
            color = Color.rgb(245, 248, 255)
            style = Paint.Style.FILL
        }

        val kpiBorderPaint = Paint().apply {
            color = Color.rgb(199, 210, 254)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        var yPos = margin + 15f

        // Draw Header
        canvas.drawText("TUITION CRM - MONTHLY FEE & REVENUE AUDIT", margin, yPos, titlePaint)
        yPos += 14f
        canvas.drawText("Generated on: ${dateFormat.format(Date())} • Official Record", margin, yPos, subtitlePaint)
        yPos += 18f

        // Decorative horizontal rule
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, Paint().apply {
            color = Color.rgb(59, 130, 246)
            strokeWidth = 2.5f
        })
        yPos += 18f

        // Draw 3 KPI Summary Cards
        val cardWidth = (pageWidth - (margin * 2) - 20) / 3
        val cardHeight = 52f

        // Card 1: Completed Revenue
        drawKpiCard(
            canvas = canvas,
            x = margin,
            y = yPos,
            width = cardWidth,
            height = cardHeight,
            label = "COMPLETED REVENUE",
            value = currencyFormatter.format(overview.totalCompletedRevenue),
            subtext = "From fully paid students",
            boxPaint = kpiBoxPaint,
            borderPaint = kpiBorderPaint,
            titlePaint = subtitlePaint,
            valPaint = sectionTitlePaint
        )

        // Card 2: Total Collections
        drawKpiCard(
            canvas = canvas,
            x = margin + cardWidth + 10,
            y = yPos,
            width = cardWidth,
            height = cardHeight,
            label = "TOTAL COLLECTIONS",
            value = currencyFormatter.format(overview.totalAllRevenue),
            subtext = "Across all installments",
            boxPaint = kpiBoxPaint,
            borderPaint = kpiBorderPaint,
            titlePaint = subtitlePaint,
            valPaint = sectionTitlePaint
        )

        // Card 3: Monthly Average
        drawKpiCard(
            canvas = canvas,
            x = margin + (cardWidth * 2) + 20,
            y = yPos,
            width = cardWidth,
            height = cardHeight,
            label = "MONTHLY AVG",
            value = currencyFormatter.format(overview.averageMonthlyCompletedRevenue),
            subtext = "Peak: ${overview.peakMonthItem?.monthShort ?: "N/A"}",
            boxPaint = kpiBoxPaint,
            borderPaint = kpiBorderPaint,
            titlePaint = subtitlePaint,
            valPaint = sectionTitlePaint
        )

        yPos += cardHeight + 24f

        // SECTION 1: Monthly Aggregates Table
        canvas.drawText("1. Monthly Revenue Aggregates (Completed Student Fees)", margin, yPos, sectionTitlePaint)
        yPos += 14f

        // Table Header
        val colX = floatArrayOf(margin + 6, margin + 110, margin + 220, margin + 330, margin + 420)
        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 20f, headerBgPaint)
        canvas.drawText("Month", colX[0], yPos + 14f, tableHeaderPaint)
        canvas.drawText("Completed Rev", colX[1], yPos + 14f, tableHeaderPaint)
        canvas.drawText("Total Collected", colX[2], yPos + 14f, tableHeaderPaint)
        canvas.drawText("Completed Students", colX[3], yPos + 14f, tableHeaderPaint)
        canvas.drawText("Payments Count", colX[4], yPos + 14f, tableHeaderPaint)
        yPos += 22f

        overview.items.forEach { item ->
            val rowBg = if (item.isCurrentMonth) Color.rgb(240, 253, 244) else Color.WHITE
            canvas.drawRect(margin, yPos - 2f, pageWidth - margin, yPos + 16f, Paint().apply { color = rowBg })
            
            val monthLabel = if (item.isCurrentMonth) "${item.monthFull} (Current)" else item.monthFull
            canvas.drawText(truncateText(monthLabel, 22), colX[0], yPos + 12f, if (item.isCurrentMonth) tableRowBoldPaint else tableRowPaint)
            canvas.drawText(currencyFormatter.format(item.completedRevenue), colX[1], yPos + 12f, tableRowBoldPaint)
            canvas.drawText(currencyFormatter.format(item.totalRevenue), colX[2], yPos + 12f, tableRowPaint)
            canvas.drawText("${item.completedStudentsCount} students", colX[3], yPos + 12f, tableRowPaint)
            canvas.drawText("${item.totalPaymentsCount} txns", colX[4], yPos + 12f, tableRowPaint)
            
            yPos += 18f
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        }

        yPos += 24f

        // SECTION 2: Detailed Payment Transaction Ledger
        canvas.drawText("2. Detailed Fee Payment Ledger", margin, yPos, sectionTitlePaint)
        yPos += 14f

        val txColX = floatArrayOf(margin + 4, margin + 85, margin + 155, margin + 270, margin + 360, margin + 440)
        
        fun drawTxHeader() {
            canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 20f, headerBgPaint)
            canvas.drawText("Date", txColX[0], yPos + 14f, tableHeaderPaint)
            canvas.drawText("Receipt", txColX[1], yPos + 14f, tableHeaderPaint)
            canvas.drawText("Student & Course", txColX[2], yPos + 14f, tableHeaderPaint)
            canvas.drawText("Amount", txColX[3], yPos + 14f, tableHeaderPaint)
            canvas.drawText("Mode", txColX[4], yPos + 14f, tableHeaderPaint)
            canvas.drawText("Status", txColX[5], yPos + 14f, tableHeaderPaint)
            yPos += 22f
        }

        drawTxHeader()

        val sortedPayments = payments.sortedByDescending { it.paymentDate }
        for (payment in sortedPayments) {
            // Check page boundary
            if (yPos > pageHeight - margin - 30f) {
                // Page footer
                canvas.drawText("Page $pageNumber", pageWidth - margin - 40, pageHeight - margin + 10, subtitlePaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f

                canvas.drawText("TUITION CRM - DETAILED FEE PAYMENT LEDGER (Cont.)", margin, yPos, sectionTitlePaint)
                yPos += 18f
                drawTxHeader()
            }

            val student = studentsById[payment.studentId]
            val course = student?.let { coursesById[it.courseId] }
            val studentCourseLabel = "${student?.name ?: "Unknown"} (${course?.name ?: "Course"})"

            canvas.drawText(shortDate.format(Date(payment.paymentDate)), txColX[0], yPos + 12f, tableRowPaint)
            canvas.drawText(truncateText(payment.receiptNo, 12), txColX[1], yPos + 12f, tableRowPaint)
            canvas.drawText(truncateText(studentCourseLabel, 26), txColX[2], yPos + 12f, tableRowPaint)
            canvas.drawText(currencyFormatter.format(payment.amount), txColX[3], yPos + 12f, tableRowBoldPaint)
            canvas.drawText(payment.paymentMode, txColX[4], yPos + 12f, tableRowPaint)
            
            // Status tag
            val status = student?.feeStatus ?: "N/A"
            canvas.drawText(status, txColX[5], yPos + 12f, tableRowBoldPaint)

            yPos += 18f
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
        }

        // Draw final page footer
        canvas.drawText("Page $pageNumber • End of Tuition CRM Audit Report", margin, pageHeight - margin + 10, subtitlePaint)
        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Tuition Monthly Fee Payment Report (PDF)")
            putExtra(Intent.EXTRA_TEXT, "Attached is the official Tuition CRM Monthly Fee Payment & Revenue Audit Report (.pdf).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return ExportResult(
            file = file,
            shareIntent = shareIntent,
            format = "PDF",
            recordCount = payments.size
        )
    }

    private fun drawKpiCard(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        subtext: String,
        boxPaint: Paint,
        borderPaint: Paint,
        titlePaint: Paint,
        valPaint: Paint
    ) {
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, boxPaint)
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, borderPaint)
        canvas.drawText(label, x + 8f, y + 15f, titlePaint)
        canvas.drawText(value, x + 8f, y + 33f, valPaint)
        canvas.drawText(subtext, x + 8f, y + 46f, titlePaint)
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) text.take(maxLength - 3) + "..." else text
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}
