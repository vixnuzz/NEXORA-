package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun FinanceScreen(
    records: List<FinanceRecord>,
    invoices: List<Invoice>,
    metrics: DashboardMetrics,
    currency: String,
    defaultTaxRate: Double,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertRecord: (FinanceRecord) -> Unit,
    onDeleteRecord: (FinanceRecord) -> Unit,
    onUpsertInvoice: (Invoice) -> Unit,
    onDeleteInvoice: (Invoice) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var typeFilter by remember { mutableStateOf<FinanceType?>(null) }
    var editingRecord by remember { mutableStateOf<FinanceRecord?>(null) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showInvoiceDialog by remember { mutableStateOf(false) }

    val filteredRecords = remember(records, searchQuery, typeFilter) {
        records.filter { r ->
            (typeFilter == null || r.typeEnum() == typeFilter) &&
                (searchQuery.isBlank() ||
                    r.title.contains(searchQuery, true) ||
                    r.category.contains(searchQuery, true))
        }
    }

    val filteredInvoices = remember(invoices, searchQuery) {
        invoices.filter { inv ->
            searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, true) ||
                inv.customerName.contains(searchQuery, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "Finance & Accounting",
            subtitle = "Net Profit: ${formatMoney(metrics.netProfit, currency)}",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = if (selectedTab == 1) "New Invoice" else "New Entry",
            addTag = "finance_add_button",
            onAddClick = {
                if (selectedTab == 1) {
                    editingInvoice = null; showInvoiceDialog = true
                } else {
                    editingRecord = null; showRecordDialog = true
                }
            },
            onExportCsv = {
                if (selectedTab == 1) {
                    onExportCsv(
                        "invoices_estimates",
                        listOf("Number", "Type", "Customer", "Amount", "Tax", "Status", "DueDate"),
                        filteredInvoices.map { listOf(it.invoiceNumber, it.docType, it.customerName, it.amount.toString(), it.taxAmount.toString(), it.status, it.dueDate) }
                    )
                } else {
                    onExportCsv(
                        "finance_ledger",
                        listOf("Date", "Title", "Type", "Category", "Amount", "Method", "Notes"),
                        filteredRecords.map { listOf(it.dateStr, it.title, it.type, it.category, it.amount.toString(), it.paymentMethod, it.notes) }
                    )
                }
            }
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Ledger (${records.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Invoices (${invoices.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("P&L Report") })
        }

        when (selectedTab) {
            0 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = typeFilter == null, onClick = { typeFilter = null }, label = { Text("All") })
                    FilterChip(selected = typeFilter == FinanceType.INCOME, onClick = { typeFilter = FinanceType.INCOME }, label = { Text("Income") })
                    FilterChip(selected = typeFilter == FinanceType.EXPENSE, onClick = { typeFilter = FinanceType.EXPENSE }, label = { Text("Expenses") })
                }

                if (filteredRecords.isEmpty()) {
                    EmptyModuleCard(
                        title = "No Ledger Entries",
                        description = "Track income, operating expenses, and payments with real-time P&L aggregation.",
                        icon = Icons.Filled.AccountBalanceWallet,
                        actionLabel = if (canEdit) "Add Entry" else null,
                        onAction = if (canEdit) { { editingRecord = null; showRecordDialog = true } } else null
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredRecords, key = { it.id }) { rec ->
                            val isIncome = rec.typeEnum() == FinanceType.INCOME
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rec.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${rec.category} • ${rec.dateStr} • ${rec.paymentMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        (if (isIncome) "+" else "-") + formatMoney(rec.amount, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isIncome) NexoraEmerald else StatusError,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (canEdit) {
                                        IconButton(onClick = { editingRecord = rec; showRecordDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit transaction")
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { onDeleteRecord(rec) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete transaction", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (filteredInvoices.isEmpty()) {
                    EmptyModuleCard(
                        title = "No Invoices or Estimates",
                        description = "Create customer invoices and quotations, track due dates, and record payments.",
                        icon = Icons.Filled.ReceiptLong,
                        actionLabel = if (canEdit) "Create Invoice" else null,
                        onAction = if (canEdit) { { editingInvoice = null; showInvoiceDialog = true } } else null
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredInvoices, key = { it.id }) { inv ->
                            val st = inv.statusEnum()
                            val stColor = when (st) {
                                InvoiceStatus.PAID, InvoiceStatus.ACCEPTED -> NexoraEmerald
                                InvoiceStatus.OVERDUE -> StatusError
                                InvoiceStatus.PENDING, InvoiceStatus.SENT -> StatusWarning
                                InvoiceStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${inv.invoiceNumber} • ${inv.docTypeEnum().label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Text(inv.customerName, style = MaterialTheme.typography.titleMedium)
                                            Text("Due ${inv.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(formatMoney(inv.amount, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))
                                            StatusPill(st.label, stColor)
                                        }
                                    }
                                    if (canEdit) {
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (inv.docTypeEnum() == InvoiceDocType.INVOICE && st != InvoiceStatus.PAID) {
                                                FilledTonalButton(onClick = { onUpsertInvoice(inv.copy(status = InvoiceStatus.PAID.name)) }) {
                                                    Text("Mark Paid")
                                                }
                                            }
                                            if (inv.docTypeEnum() == InvoiceDocType.ESTIMATE) {
                                                FilledTonalButton(onClick = {
                                                    onUpsertInvoice(
                                                        inv.copy(
                                                            docType = InvoiceDocType.INVOICE.name,
                                                            invoiceNumber = inv.invoiceNumber.replace("EST", "INV"),
                                                            status = InvoiceStatus.PENDING.name
                                                        )
                                                    )
                                                }) { Text("Convert to Invoice") }
                                            }
                                            Spacer(Modifier.weight(1f))
                                            IconButton(onClick = { editingInvoice = inv; showInvoiceDialog = true }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit invoice")
                                            }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteInvoice(inv) }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete invoice", tint = StatusError)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                val categoryTotals = remember(records) {
                    records.groupBy { it.typeEnum() to it.category }
                        .map { (key, list) -> Triple(key.first, key.second, list.sumOf { it.amount }) }
                        .sortedByDescending { it.third }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Profit & Loss Statement (Live)", style = MaterialTheme.typography.titleMedium)
                                PnlRow("Total Gross Revenue", formatMoney(metrics.totalRevenue, currency), NexoraEmerald)
                                PnlRow("Total Operating Expenses", formatMoney(metrics.totalExpenses, currency), StatusError)
                                HorizontalDivider()
                                PnlRow(
                                    "Net Operating Profit",
                                    formatMoney(metrics.netProfit, currency),
                                    if (metrics.netProfit >= 0) NexoraCyan else StatusError
                                )
                                val margin = if (metrics.totalRevenue > 0) (metrics.netProfit * 100.0 / metrics.totalRevenue) else 0.0
                                PnlRow("Net Profit Margin", String.format("%.1f%%", margin), MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    items(categoryTotals) { (type, cat, total) ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$cat (${type.label})", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    formatMoney(total, currency),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (type == FinanceType.INCOME) NexoraEmerald else StatusError
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        FinanceRecordFormDialog(editingRecord, { showRecordDialog = false }, onUpsertRecord)
    }
    if (showInvoiceDialog) {
        InvoiceFormDialog(editingInvoice, defaultTaxRate, { showInvoiceDialog = false }, onUpsertInvoice)
    }
}

@Composable
private fun PnlRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}
