package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.MonthlySalesAnalyticsChart
import com.example.ui.components.StatusPill
import com.example.ui.components.formatMoney
import com.example.ui.theme.*
import com.example.util.AppModule

@Composable
fun DashboardScreen(
    business: Business?,
    metrics: DashboardMetrics,
    activeRole: UserRole,
    onNavigate: (AppModule) -> Unit,
    onToggleTaskDone: (ProjectTask) -> Unit,
    onLoadDemoData: () -> Unit
) {
    val currency = business?.currency ?: "USD"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Welcome & Demo Indicator
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = business?.name ?: "NEXORA ONE",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (business?.demoSeeded == true) {
                                StatusPill("Demo Seed Active", StatusWarning)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${business?.industry ?: "Enterprise"} • Viewing as ${activeRole.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (business?.demoSeeded == false && metrics.totalRevenue == 0.0 && metrics.totalEmployees == 0) {
                            OutlinedButton(
                                onClick = onLoadDemoData,
                                modifier = Modifier.testTag("dashboard_load_demo_button")
                            ) {
                                Text("Load Demo Data")
                            }
                        }
                        FilledTonalButton(
                            onClick = { onNavigate(AppModule.AI_ASSISTANT) },
                            modifier = Modifier.testTag("dashboard_ask_ai_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Brief")
                        }
                    }
                }
            }
        }

        // Primary Financial KPI Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Total Revenue",
                    value = formatMoney(metrics.totalRevenue, currency),
                    subtitle = "Ledger + Paid + Orders",
                    icon = Icons.Filled.TrendingUp,
                    accent = NexoraEmerald,
                    onClick = { onNavigate(AppModule.FINANCE) },
                    modifier = Modifier.weight(1f).testTag("kpi_revenue")
                )
                KpiCard(
                    title = "Total Expenses",
                    value = formatMoney(metrics.totalExpenses, currency),
                    subtitle = "Operating + POs",
                    icon = Icons.Filled.ReceiptLong,
                    accent = StatusError,
                    onClick = { onNavigate(AppModule.FINANCE) },
                    modifier = Modifier.weight(1f).testTag("kpi_expenses")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Net Profit",
                    value = formatMoney(metrics.netProfit, currency),
                    subtitle = if (metrics.netProfit >= 0) "Healthy margin" else "Operating deficit",
                    icon = Icons.Filled.AccountBalance,
                    accent = if (metrics.netProfit >= 0) NexoraCyan else StatusError,
                    onClick = { onNavigate(AppModule.FINANCE) },
                    modifier = Modifier.weight(1f).testTag("kpi_net_profit")
                )
                KpiCard(
                    title = "Pending Invoices",
                    value = "${metrics.pendingInvoicesCount}",
                    subtitle = formatMoney(metrics.pendingInvoicesTotal, currency),
                    icon = Icons.Filled.RequestQuote,
                    accent = StatusWarning,
                    onClick = { onNavigate(AppModule.FINANCE) },
                    modifier = Modifier.weight(1f).testTag("kpi_pending_invoices")
                )
            }
        }

        // Operations KPI Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniKpiCard(
                    title = "Active Customers",
                    value = "${metrics.activeCustomers}",
                    icon = Icons.Filled.People,
                    accent = NexoraCyan,
                    onClick = { onNavigate(AppModule.CRM) },
                    modifier = Modifier.weight(1f).testTag("kpi_customers")
                )
                MiniKpiCard(
                    title = "Employees",
                    value = "${metrics.totalEmployees}",
                    icon = Icons.Filled.Badge,
                    accent = NexoraIndigoLight,
                    onClick = { onNavigate(AppModule.HR) },
                    modifier = Modifier.weight(1f).testTag("kpi_employees")
                )
                MiniKpiCard(
                    title = "Active Projects",
                    value = "${metrics.activeProjects}",
                    icon = Icons.Filled.ViewKanban,
                    accent = NexoraEmerald,
                    onClick = { onNavigate(AppModule.PROJECTS) },
                    modifier = Modifier.weight(1f).testTag("kpi_projects")
                )
            }
        }

        // Low-Stock & Pipeline Alerts Strip
        if (metrics.lowStockCount > 0 || metrics.openTicketsCount > 0 || metrics.pipelineValue > 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (metrics.lowStockCount > 0) {
                        AlertBannerCard(
                            title = "${metrics.lowStockCount} Low-Stock Alert(s)",
                            subtitle = "Tap to restock inventory",
                            color = StatusWarning,
                            icon = Icons.Filled.WarningAmber,
                            onClick = { onNavigate(AppModule.INVENTORY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AlertBannerCard(
                        title = "Pipeline ${formatMoney(metrics.pipelineValue, currency)}",
                        subtitle = "${metrics.openTicketsCount} open support ticket(s)",
                        color = NexoraCyan,
                        icon = Icons.Filled.Handshake,
                        onClick = { onNavigate(AppModule.CRM) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Monthly Sales Analytics Chart
        item {
            MonthlySalesAnalyticsChart(
                points = metrics.monthlySeries,
                currency = currency
            )
        }

        // Tasks & Reminders
        item {
            SectionCard(
                title = "Tasks & Reminders",
                actionLabel = "Board",
                onAction = { onNavigate(AppModule.PROJECTS) }
            ) {
                if (metrics.upcomingTasks.isEmpty()) {
                    Text(
                        "No pending tasks. Create tasks in Projects & Kanban.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    metrics.upcomingTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.columnEnum() == KanbanColumn.DONE,
                                onCheckedChange = { onToggleTaskDone(task) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${task.projectName} • Due ${task.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (task.reminderActive) {
                                Icon(
                                    Icons.Filled.Alarm,
                                    contentDescription = "Reminder set",
                                    tint = StatusWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            StatusPill(
                                label = task.priorityEnum().label,
                                color = when (task.priorityEnum()) {
                                    TaskPriority.URGENT -> StatusError
                                    TaskPriority.HIGH -> StatusWarning
                                    TaskPriority.MEDIUM -> NexoraCyan
                                    TaskPriority.LOW -> NexoraEmerald
                                }
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            SectionCard(
                title = "Recent Transactions",
                actionLabel = "Ledger",
                onAction = { onNavigate(AppModule.FINANCE) }
            ) {
                if (metrics.recentTransactions.isEmpty()) {
                    Text(
                        "No income or expense entries recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    metrics.recentTransactions.forEach { rec ->
                        val isIncome = rec.typeEnum() == FinanceType.INCOME
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncome) NexoraEmerald else StatusError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${rec.category} • ${rec.dateStr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = (if (isIncome) "+" else "-") + formatMoney(rec.amount, currency),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isIncome) NexoraEmerald else StatusError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Recent Activities
        item {
            SectionCard(
                title = "Recent Activities",
                actionLabel = "All Logs",
                onAction = { onNavigate(AppModule.NOTIFICATIONS) }
            ) {
                if (metrics.recentActivities.isEmpty()) {
                    Text(
                        "Activity logs will appear automatically as your team creates and updates records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    metrics.recentActivities.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(log.module, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${log.actorName} (${log.actorRole})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlertBannerCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
