package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartMetricMode
import com.example.data.model.MonthlyRevenueItem
import com.example.data.model.MonthlyRevenueOverview
import com.example.data.model.RevenueTimeRange
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyRevenueVisualizer(
    overview: MonthlyRevenueOverview,
    selectedTimeRange: RevenueTimeRange,
    selectedMetricMode: ChartMetricMode,
    onTimeRangeSelected: (RevenueTimeRange) -> Unit,
    onMetricModeSelected: (ChartMetricMode) -> Unit,
    onExportClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showBreakdownTable by remember { mutableStateOf(false) }

    // Auto-select latest month if items exist and none selected
    LaunchedEffect(overview.items) {
        if (overview.items.isNotEmpty() && (selectedIndex < 0 || selectedIndex >= overview.items.size)) {
            selectedIndex = overview.items.size - 1
        }
    }

    val activeItem = if (selectedIndex in overview.items.indices) overview.items[selectedIndex] else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_revenue_visualizer"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Icon, Title, and Growth badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Monthly Revenue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Completed Student Fees Overview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Growth Rate Pill & Optional Export button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overview.currentMonthGrowthRate != 0.0) {
                        val isPositive = overview.currentMonthGrowthRate >= 0
                        Surface(
                            color = if (isPositive) StatusPaidGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("growth_rate_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = if (isPositive) StatusPaidGreen else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${if (isPositive) "+" else ""}${overview.currentMonthGrowthRate.roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) StatusPaidGreen else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (onExportClick != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onExportClick() }
                                .testTag("export_revenue_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export Report",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Export",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Time Range & Metric Mode Controls
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Time Range
                RevenueTimeRange.values().forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { onTimeRangeSelected(range) },
                        label = { Text(range.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("time_range_${range.name.lowercase()}")
                    )
                }

                // Metric Mode
                ChartMetricMode.values().forEach { mode ->
                    FilterChip(
                        selected = selectedMetricMode == mode,
                        onClick = { onMetricModeSelected(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("metric_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Interactive Tooltip Card (Recharts style floating indicator)
            AnimatedVisibility(
                visible = activeItem != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (activeItem != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recharts_tooltip"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeItem.monthFull,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (activeItem.isCurrentMonth) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "CURRENT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (overview.peakMonthItem?.yearMonth == activeItem.yearMonth) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = StatusPartialAmber.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = StatusPartialAmber,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "PEAK",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StatusPartialAmber,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "Tap any bar to inspect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Tooltip Metric rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Completed Student Revenue
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Completed Student Revenue",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = currencyFormatter.format(activeItem.completedRevenue),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                    )
                                }

                                // Total Collections
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.tertiary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Total Collections",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = currencyFormatter.format(activeItem.totalRevenue),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Secondary details row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${activeItem.completedStudentsCount} students completed fees",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${activeItem.totalPaymentsCount} transactions recorded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recharts-style Canvas Bar Chart
            RechartsBarChart(
                items = overview.items,
                selectedIndex = selectedIndex,
                metricMode = selectedMetricMode,
                onSelectBar = { index -> selectedIndex = index },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .testTag("chart_canvas")
            )

            // Recharts Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Completed Student Fees",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (selectedMetricMode == ChartMetricMode.COMPARATIVE || selectedMetricMode == ChartMetricMode.ALL_COLLECTIONS) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Collections",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // KPI Financial Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FinancialKpiCard(
                    title = "Total Completed",
                    value = currencyFormatter.format(overview.totalCompletedRevenue),
                    subtitle = "In ${overview.items.size} months",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                FinancialKpiCard(
                    title = "Monthly Avg",
                    value = currencyFormatter.format(overview.averageMonthlyCompletedRevenue),
                    subtitle = "Per active month",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                FinancialKpiCard(
                    title = "Peak Month",
                    value = overview.peakMonthItem?.let { currencyFormatter.format(it.completedRevenue) } ?: "₹0",
                    subtitle = overview.peakMonthItem?.monthShort ?: "N/A",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Expandable Monthly Breakdown Ledger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBreakdownTable = !showBreakdownTable }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Month-by-Month Breakdown Table",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (showBreakdownTable) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = showBreakdownTable) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    overview.items.forEach { item ->
                        MonthlyBreakdownRow(
                            item = item,
                            currencyFormatter = currencyFormatter,
                            isSelected = activeItem?.yearMonth == item.yearMonth,
                            onClick = {
                                val idx = overview.items.indexOf(item)
                                if (idx != -1) selectedIndex = idx
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialKpiCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MonthlyBreakdownRow(
    item: MonthlyRevenueItem,
    currencyFormatter: NumberFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.monthFull,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.isCurrentMonth) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Current)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "${item.completedStudentsCount} students completed • ${item.totalPaymentsCount} payments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormatter.format(item.completedRevenue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total: ${currencyFormatter.format(item.totalRevenue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * RechartsBarChart replicates the aesthetic and interaction of Recharts <ResponsiveContainer><BarChart>:
 * - Cartesian horizontal grid lines with dashed pattern
 * - Y-Axis tick labels with compact currency format (₹0, ₹5k, ₹10k)
 * - X-Axis month labels
 * - Rounded-top bars with smooth animation
 * - Interactive cursor & active bar highlight
 */
@Composable
private fun RechartsBarChart(
    items: List<MonthlyRevenueItem>,
    selectedIndex: Int,
    metricMode: ChartMetricMode,
    onSelectBar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No revenue records available for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryVariant = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeGuideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    // Animation progress
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "BarChartAnimation"
    )

    // Calculate max value for Y-axis scaling
    val maxRaw = when (metricMode) {
        ChartMetricMode.COMPLETED_FEES -> items.maxOfOrNull { it.completedRevenue } ?: 1000.0
        ChartMetricMode.ALL_COLLECTIONS -> items.maxOfOrNull { it.totalRevenue } ?: 1000.0
        ChartMetricMode.COMPARATIVE -> max(
            items.maxOfOrNull { it.completedRevenue } ?: 1000.0,
            items.maxOfOrNull { it.totalRevenue } ?: 1000.0
        )
    }

    // Step calculation: round max up to a friendly number
    val step = when {
        maxRaw <= 5000 -> 1000.0
        maxRaw <= 15000 -> 3000.0
        maxRaw <= 30000 -> 5000.0
        else -> 10000.0
    }
    val yAxisMax = ((maxRaw / step).toInt() + 1) * step

    Canvas(
        modifier = modifier.pointerInput(items) {
            detectTapGestures { offset ->
                val leftPadding = 48.dp.toPx()
                val bottomPadding = 30.dp.toPx()
                val topPadding = 16.dp.toPx()
                val chartWidth = size.width - leftPadding
                val barSlotWidth = chartWidth / items.size

                if (offset.x >= leftPadding && offset.y in topPadding..(size.height - bottomPadding)) {
                    val clickedIndex = ((offset.x - leftPadding) / barSlotWidth).toInt()
                    if (clickedIndex in items.indices) {
                        onSelectBar(clickedIndex)
                    }
                }
            }
        }
    ) {
        val leftPadding = 48.dp.toPx()
        val bottomPadding = 30.dp.toPx()
        val topPadding = 16.dp.toPx()
        val rightPadding = 12.dp.toPx()

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        val baselineY = size.height - bottomPadding

        // 1. Cartesian Grid & Y-Axis Ticks (4 horizontal gridlines, like Recharts CartesianGrid)
        val gridLinesCount = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        for (i in 0..gridLinesCount) {
            val ratio = i.toFloat() / gridLinesCount
            val y = baselineY - (ratio * chartHeight)
            val tickValue = ratio * yAxisMax

            // Dashed horizontal grid line
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )

            // Y-Axis tick text (e.g. ₹0, ₹3k, ₹6k, ₹9k)
            val tickLabel = if (tickValue >= 1000) {
                "₹${(tickValue / 1000).toInt()}k"
            } else {
                "₹${tickValue.toInt()}"
            }

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = labelTextColor.hashCode()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                drawText(
                    tickLabel,
                    leftPadding - 8.dp.toPx(),
                    y + 4.dp.toPx(),
                    paint
                )
            }
        }

        // 2. Bars & X-Axis Month labels
        val barSlotWidth = chartWidth / items.size
        val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

        items.forEachIndexed { index, item ->
            val slotStartX = leftPadding + (index * barSlotWidth)
            val slotCenterX = slotStartX + (barSlotWidth / 2)

            // Draw active cursor highlight column (like Recharts <Tooltip cursor=... />)
            if (index == selectedIndex) {
                drawRoundRect(
                    color = activeGuideColor,
                    topLeft = Offset(slotStartX + 2.dp.toPx(), topPadding),
                    size = Size(barSlotWidth - 4.dp.toPx(), chartHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                // Draw active indicator line on baseline
                drawLine(
                    color = primaryColor,
                    start = Offset(slotStartX + 4.dp.toPx(), baselineY),
                    end = Offset(slotStartX + barSlotWidth - 4.dp.toPx(), baselineY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Determine Bar Heights based on metricMode
            when (metricMode) {
                ChartMetricMode.COMPLETED_FEES -> {
                    val barWidth = (barSlotWidth * 0.48f).coerceIn(16.dp.toPx(), 36.dp.toPx())
                    val barHeight = ((item.completedRevenue / yAxisMax).toFloat() * chartHeight * animationProgress).coerceAtLeast(0f)
                    val barTopY = baselineY - barHeight
                    val barLeftX = slotCenterX - (barWidth / 2)

                    if (barHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                                startY = barTopY,
                                endY = baselineY
                            ),
                            topLeft = Offset(barLeftX, barTopY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )
                        // If selected, add subtle outline
                        if (index == selectedIndex) {
                            drawRoundRect(
                                color = activeBorderColor,
                                topLeft = Offset(barLeftX - 1.dp.toPx(), barTopY - 1.dp.toPx()),
                                size = Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                                cornerRadius = cornerRadius,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                ChartMetricMode.ALL_COLLECTIONS -> {
                    val barWidth = (barSlotWidth * 0.48f).coerceIn(16.dp.toPx(), 36.dp.toPx())
                    val barHeight = ((item.totalRevenue / yAxisMax).toFloat() * chartHeight * animationProgress).coerceAtLeast(0f)
                    val barTopY = baselineY - barHeight
                    val barLeftX = slotCenterX - (barWidth / 2)

                    if (barHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(tertiaryColor, tertiaryColor.copy(alpha = 0.8f)),
                                startY = barTopY,
                                endY = baselineY
                            ),
                            topLeft = Offset(barLeftX, barTopY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }

                ChartMetricMode.COMPARATIVE -> {
                    // Dual side-by-side bars (Recharts grouped bar format)
                    val singleBarWidth = (barSlotWidth * 0.28f).coerceIn(10.dp.toPx(), 20.dp.toPx())
                    val barSpacing = 3.dp.toPx()

                    // Completed Bar (Left)
                    val completedHeight = ((item.completedRevenue / yAxisMax).toFloat() * chartHeight * animationProgress).coerceAtLeast(0f)
                    val completedLeftX = slotCenterX - singleBarWidth - (barSpacing / 2)
                    val completedTopY = baselineY - completedHeight

                    if (completedHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                                startY = completedTopY,
                                endY = baselineY
                            ),
                            topLeft = Offset(completedLeftX, completedTopY),
                            size = Size(singleBarWidth, completedHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // Total Collections Bar (Right)
                    val totalHeight = ((item.totalRevenue / yAxisMax).toFloat() * chartHeight * animationProgress).coerceAtLeast(0f)
                    val totalLeftX = slotCenterX + (barSpacing / 2)
                    val totalTopY = baselineY - totalHeight

                    if (totalHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(tertiaryColor, tertiaryColor.copy(alpha = 0.8f)),
                                startY = totalTopY,
                                endY = baselineY
                            ),
                            topLeft = Offset(totalLeftX, totalTopY),
                            size = Size(singleBarWidth, totalHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            // X-Axis Month label below baseline
            val isSelected = index == selectedIndex
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = if (isSelected) primaryColor.hashCode() else labelTextColor.hashCode()
                    textSize = if (isSelected) 12.sp.toPx() else 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = isSelected
                    isAntiAlias = true
                }
                drawText(
                    item.monthShort,
                    slotCenterX,
                    baselineY + 20.dp.toPx(),
                    paint
                )
            }
        }

        // X-Axis baseline line
        drawLine(
            color = gridColor,
            start = Offset(leftPadding, baselineY),
            end = Offset(size.width - rightPadding, baselineY),
            strokeWidth = 1.5f
        )
    }
}
