package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MonthlyMetricPoint
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraEmerald
import com.example.ui.theme.StatusError
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatMoney(amount: Double, currencyCode: String = "USD"): String {
    return try {
        val fmt = NumberFormat.getCurrencyInstance(Locale.US)
        fmt.currency = Currency.getInstance(currencyCode.ifBlank { "USD" })
        fmt.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        fmt.format(amount)
    } catch (_: Exception) {
        "$currencyCode ${String.format(Locale.US, "%.2f", amount)}"
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ModuleHeaderBar(
    title: String,
    subtitle: String,
    canCreate: Boolean,
    canExport: Boolean,
    addLabel: String,
    addTag: String,
    onAddClick: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canExport) {
                OutlinedButton(
                    onClick = onExportCsv,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CSV")
                }
            }
            if (canCreate) {
                Button(
                    onClick = onAddClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(addTag)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(addLabel)
                }
            }
        }
    }
}

@Composable
fun EmptyModuleCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Filled.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun MonthlySalesAnalyticsChart(
    points: List<MonthlyMetricPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Monthly Sales & Expense Analytics", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Live aggregation from Sales Orders & Finance Ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot("Sales", NexoraEmerald)
                    LegendDot("Expenses", StatusError)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No monthly sales or expense records yet. Add an order or transaction to populate live charts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = points.maxOf { maxOf(it.salesAmount, it.expenseAmount) }.coerceAtLeast(1.0)
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = h - (h * i / gridSteps.toFloat())
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    val groupWidth = w / points.size.toFloat()
                    val barWidth = (groupWidth * 0.28f).coerceAtMost(36f)
                    val gap = barWidth * 0.25f

                    points.forEachIndexed { idx, pt ->
                        val centerX = groupWidth * idx + groupWidth / 2f
                        val salesH = ((pt.salesAmount / maxVal) * (h * 0.9f)).toFloat().coerceAtLeast(4f)
                        val expH = ((pt.expenseAmount / maxVal) * (h * 0.9f)).toFloat().coerceAtLeast(4f)

                        drawRoundRect(
                            color = NexoraEmerald,
                            topLeft = Offset(centerX - barWidth - gap / 2f, h - salesH),
                            size = Size(barWidth, salesH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = StatusError.copy(alpha = 0.85f),
                            topLeft = Offset(centerX + gap / 2f, h - expH),
                            size = Size(barWidth, expH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    points.forEach { pt ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                pt.monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatMoney(pt.salesAmount, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = NexoraCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
