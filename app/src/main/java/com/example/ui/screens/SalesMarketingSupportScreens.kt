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
import java.util.Locale

@Composable
fun SalesScreen(
    orders: List<SalesOrder>,
    currency: String,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertOrder: (SalesOrder) -> Unit,
    onDeleteOrder: (SalesOrder) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var editing by remember { mutableStateOf<SalesOrder?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(orders, searchQuery) {
        orders.filter { o ->
            searchQuery.isBlank() ||
                o.orderNumber.contains(searchQuery, true) ||
                o.customerName.contains(searchQuery, true) ||
                o.productName.contains(searchQuery, true)
        }
    }
    val totalSales = remember(orders) { orders.filter { it.statusEnum() != OrderStatus.CANCELLED }.sumOf { it.totalAmount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "Sales & Orders",
            subtitle = "${orders.size} orders • Total ${formatMoney(totalSales, currency)}",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = "New Order",
            addTag = "sales_add_button",
            onAddClick = { editing = null; showDialog = true },
            onExportCsv = {
                onExportCsv(
                    "sales_orders",
                    listOf("OrderNumber", "Customer", "Product", "Qty", "Subtotal", "Discount%", "Total", "Month", "Status"),
                    filtered.map { listOf(it.orderNumber, it.customerName, it.productName, it.quantity.toString(), it.subtotal.toString(), it.discountPercent.toString(), it.totalAmount.toString(), it.monthLabel, it.status) }
                )
            }
        )

        if (filtered.isEmpty()) {
            EmptyModuleCard(
                title = "No Sales Orders",
                description = "Create orders with discounts and monthly revenue attribution for live dashboard analytics.",
                icon = Icons.Filled.PointOfSale,
                actionLabel = if (canEdit) "Create Order" else null,
                onAction = if (canEdit) { { editing = null; showDialog = true } } else null
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { order ->
                    val st = order.statusEnum()
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
                                    Text("${order.orderNumber} • ${order.monthLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(order.customerName, style = MaterialTheme.typography.titleMedium)
                                    Text("${order.productName} x${order.quantity} (${order.discountPercent}% off)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatMoney(order.totalAmount, currency), style = MaterialTheme.typography.titleMedium, color = NexoraEmerald, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    StatusPill(
                                        st.label,
                                        when (st) {
                                            OrderStatus.DELIVERED -> NexoraEmerald
                                            OrderStatus.SHIPPED -> NexoraCyan
                                            OrderStatus.PROCESSING -> StatusWarning
                                            OrderStatus.CANCELLED -> StatusError
                                        }
                                    )
                                }
                            }
                            if (canEdit) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    if (st == OrderStatus.PROCESSING) {
                                        TextButton(onClick = { onUpsertOrder(order.copy(status = OrderStatus.SHIPPED.name)) }) { Text("Ship") }
                                    } else if (st == OrderStatus.SHIPPED) {
                                        TextButton(onClick = { onUpsertOrder(order.copy(status = OrderStatus.DELIVERED.name)) }) { Text("Deliver") }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { editing = order; showDialog = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit order")
                                    }
                                    if (canDelete) {
                                        IconButton(onClick = { onDeleteOrder(order) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete order", tint = StatusError)
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

    if (showDialog) {
        SalesOrderFormDialog(editing, { showDialog = false }, onUpsertOrder)
    }
}

@Composable
fun MarketingScreen(
    campaigns: List<MarketingCampaign>,
    currency: String,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertCampaign: (MarketingCampaign) -> Unit,
    onDeleteCampaign: (MarketingCampaign) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var editing by remember { mutableStateOf<MarketingCampaign?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(campaigns, searchQuery) {
        campaigns.filter { c ->
            searchQuery.isBlank() || c.name.contains(searchQuery, true) || c.channel.contains(searchQuery, true)
        }
    }
    val totalConversions = remember(campaigns) { campaigns.sumOf { it.conversions } }
    val totalSpend = remember(campaigns) { campaigns.sumOf { it.budget } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "Marketing & Campaigns",
            subtitle = "${campaigns.size} campaigns • $totalConversions conversions • ${formatMoney(totalSpend, currency)} budget",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = "New Campaign",
            addTag = "marketing_add_button",
            onAddClick = { editing = null; showDialog = true },
            onExportCsv = {
                onExportCsv(
                    "marketing_campaigns",
                    listOf("Name", "Channel", "Status", "Budget", "Impressions", "Clicks", "Conversions", "CTR%"),
                    filtered.map {
                        listOf(it.name, it.channel, it.status, it.budget.toString(), it.impressions.toString(), it.clicks.toString(), it.conversions.toString(), String.format(Locale.US, "%.2f", it.ctrPercent))
                    }
                )
            }
        )

        if (filtered.isEmpty()) {
            EmptyModuleCard(
                title = "No Marketing Campaigns",
                description = "Track email campaigns, social media ads, CTR, and conversion attribution.",
                icon = Icons.Filled.Campaign,
                actionLabel = if (canEdit) "Create Campaign" else null,
                onAction = if (canEdit) { { editing = null; showDialog = true } } else null
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { camp ->
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
                                    Text(camp.channelEnum().label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(camp.name, style = MaterialTheme.typography.titleMedium)
                                }
                                StatusPill(
                                    camp.statusEnum().label,
                                    when (camp.statusEnum()) {
                                        CampaignStatus.ACTIVE -> NexoraEmerald
                                        CampaignStatus.COMPLETED -> NexoraCyan
                                        CampaignStatus.PAUSED -> StatusWarning
                                        CampaignStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Budget: ${formatMoney(camp.budget, currency)}", style = MaterialTheme.typography.bodySmall)
                                Text("CTR: ${String.format(Locale.US, "%.1f%%", camp.ctrPercent)}", style = MaterialTheme.typography.labelMedium, color = NexoraCyan)
                                Text("Conv: ${camp.conversions} (${String.format(Locale.US, "%.1f%%", camp.conversionRatePercent)})", style = MaterialTheme.typography.labelMedium, color = NexoraEmerald)
                            }
                            if (canEdit) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { editing = camp; showDialog = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit campaign")
                                    }
                                    if (canDelete) {
                                        IconButton(onClick = { onDeleteCampaign(camp) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete campaign", tint = StatusError)
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

    if (showDialog) {
        CampaignFormDialog(editing, { showDialog = false }, onUpsertCampaign)
    }
}

@Composable
fun SupportDeskScreen(
    tickets: List<SupportTicket>,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertTicket: (SupportTicket) -> Unit,
    onDeleteTicket: (SupportTicket) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var statusFilter by remember { mutableStateOf<TicketStatus?>(null) }
    var editing by remember { mutableStateOf<SupportTicket?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(tickets, searchQuery, statusFilter) {
        tickets.filter { t ->
            (statusFilter == null || t.statusEnum() == statusFilter) &&
                (searchQuery.isBlank() ||
                    t.ticketNumber.contains(searchQuery, true) ||
                    t.subject.contains(searchQuery, true) ||
                    t.customerName.contains(searchQuery, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "Support Desk",
            subtitle = "${tickets.count { it.statusEnum() != TicketStatus.RESOLVED && it.statusEnum() != TicketStatus.CLOSED }} open tickets",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = "New Ticket",
            addTag = "support_add_button",
            onAddClick = { editing = null; showDialog = true },
            onExportCsv = {
                onExportCsv(
                    "support_tickets",
                    listOf("TicketNumber", "Customer", "Subject", "Priority", "Status", "AssignedAgent", "ResolutionNotes"),
                    filtered.map { listOf(it.ticketNumber, it.customerName, it.subject, it.priority, it.status, it.assignedAgent, it.resolutionNotes) }
                )
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All") })
            FilterChip(selected = statusFilter == TicketStatus.OPEN, onClick = { statusFilter = TicketStatus.OPEN }, label = { Text("Open") })
            FilterChip(selected = statusFilter == TicketStatus.IN_PROGRESS, onClick = { statusFilter = TicketStatus.IN_PROGRESS }, label = { Text("Active") })
            FilterChip(selected = statusFilter == TicketStatus.RESOLVED, onClick = { statusFilter = TicketStatus.RESOLVED }, label = { Text("Resolved") })
        }

        if (filtered.isEmpty()) {
            EmptyModuleCard(
                title = "No Support Tickets",
                description = "Track customer support requests, priorities, agent assignment, and resolution history.",
                icon = Icons.Filled.SupportAgent,
                actionLabel = if (canEdit) "Create Ticket" else null,
                onAction = if (canEdit) { { editing = null; showDialog = true } } else null
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { t ->
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
                                    Text("${t.ticketNumber} • ${t.customerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(t.subject, style = MaterialTheme.typography.titleMedium)
                                }
                                StatusPill(
                                    t.priorityEnum().label,
                                    when (t.priorityEnum()) {
                                        TicketPriority.CRITICAL -> StatusError
                                        TicketPriority.HIGH -> StatusWarning
                                        TicketPriority.MEDIUM -> NexoraCyan
                                        TicketPriority.LOW -> NexoraEmerald
                                    }
                                )
                            }
                            if (t.description.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(t.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Agent: ${t.assignedAgent.ifBlank { "Unassigned" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                StatusPill(t.statusEnum().label, MaterialTheme.colorScheme.primary)
                            }
                            if (canEdit) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (t.statusEnum() != TicketStatus.RESOLVED && t.statusEnum() != TicketStatus.CLOSED) {
                                        FilledTonalButton(onClick = { onUpsertTicket(t.copy(status = TicketStatus.RESOLVED.name)) }) {
                                            Text("Resolve")
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { editing = t; showDialog = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit ticket")
                                    }
                                    if (canDelete) {
                                        IconButton(onClick = { onDeleteTicket(t) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete ticket", tint = StatusError)
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

    if (showDialog) {
        TicketFormDialog(editing, { showDialog = false }, onUpsertTicket)
    }
}
