package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
fun CrmScreen(
    leads: List<CrmLead>,
    contacts: List<CrmContact>,
    currency: String,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertLead: (CrmLead) -> Unit,
    onDeleteLead: (CrmLead) -> Unit,
    onUpsertContact: (CrmContact) -> Unit,
    onDeleteContact: (CrmContact) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var stageFilter by remember { mutableStateOf<LeadStage?>(null) }
    var sortByDealDesc by remember { mutableStateOf(true) }
    var editingLead by remember { mutableStateOf<CrmLead?>(null) }
    var showLeadDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<CrmContact?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }

    val filteredLeads = remember(leads, searchQuery, stageFilter, sortByDealDesc) {
        leads.filter { l ->
            (stageFilter == null || l.stageEnum() == stageFilter) &&
                (searchQuery.isBlank() ||
                    l.name.contains(searchQuery, true) ||
                    l.company.contains(searchQuery, true) ||
                    l.notes.contains(searchQuery, true))
        }.let { list ->
            if (sortByDealDesc) list.sortedByDescending { it.dealValue } else list.sortedBy { it.name }
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        contacts.filter { c ->
            searchQuery.isBlank() ||
                c.name.contains(searchQuery, true) ||
                c.company.contains(searchQuery, true) ||
                c.email.contains(searchQuery, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "CRM & Sales Pipeline",
            subtitle = "${leads.size} deals • ${contacts.size} contacts",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = if (selectedTab == 0) "New Lead" else "New Contact",
            addTag = "crm_add_button",
            onAddClick = {
                if (selectedTab == 0) {
                    editingLead = null; showLeadDialog = true
                } else {
                    editingContact = null; showContactDialog = true
                }
            },
            onExportCsv = {
                if (selectedTab == 0) {
                    onExportCsv(
                        "crm_leads",
                        listOf("Name", "Company", "Stage", "DealValue", "FollowUp", "Email", "Phone"),
                        filteredLeads.map { listOf(it.name, it.company, it.stage, it.dealValue.toString(), it.nextFollowUp, it.email, it.phone) }
                    )
                } else {
                    onExportCsv(
                        "crm_contacts",
                        listOf("Name", "Company", "Title", "Status", "Email", "Phone", "ActivityHistory"),
                        filteredContacts.map { listOf(it.name, it.company, it.roleTitle, it.status, it.email, it.phone, it.activityHistory) }
                    )
                }
            }
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Pipeline (${leads.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Contacts (${contacts.size})") })
        }

        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = stageFilter == null,
                    onClick = { stageFilter = null },
                    label = { Text("All Stages") }
                )
                LeadStage.entries.forEach { st ->
                    FilterChip(
                        selected = stageFilter == st,
                        onClick = { stageFilter = if (stageFilter == st) null else st },
                        label = { Text(st.label) }
                    )
                }
                AssistChip(
                    onClick = { sortByDealDesc = !sortByDealDesc },
                    label = { Text(if (sortByDealDesc) "Sort: Value ↓" else "Sort: Name") },
                    leadingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (filteredLeads.isEmpty()) {
                EmptyModuleCard(
                    title = "No CRM Leads Found",
                    description = "Capture leads, track deal values, and move opportunities through your sales pipeline.",
                    icon = Icons.Filled.Handshake,
                    actionLabel = if (canEdit) "Create First Lead" else null,
                    onAction = if (canEdit) { { editingLead = null; showLeadDialog = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredLeads, key = { it.id }) { lead ->
                        val st = lead.stageEnum()
                        val stColor = when (st) {
                            LeadStage.WON -> NexoraEmerald
                            LeadStage.LOST -> StatusError
                            LeadStage.PROPOSAL -> NexoraCyan
                            LeadStage.QUALIFIED -> StatusInfo
                            else -> StatusWarning
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
                                        Text(lead.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${lead.company} • ${lead.source}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(st.label, stColor)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        formatMoney(lead.dealValue, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NexoraEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (lead.nextFollowUp.isNotBlank()) {
                                        Text(
                                            "Follow-up: ${lead.nextFollowUp}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (lead.notes.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(lead.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                if (canEdit) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val nextStage = when (st) {
                                            LeadStage.NEW -> LeadStage.CONTACTED
                                            LeadStage.CONTACTED -> LeadStage.QUALIFIED
                                            LeadStage.QUALIFIED -> LeadStage.PROPOSAL
                                            LeadStage.PROPOSAL -> LeadStage.WON
                                            else -> null
                                        }
                                        if (nextStage != null) {
                                            FilledTonalButton(
                                                onClick = { onUpsertLead(lead.copy(stage = nextStage.name)) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Advance -> ${nextStage.label}")
                                            }
                                        }
                                        if (st == LeadStage.WON) {
                                            FilledTonalButton(
                                                onClick = {
                                                    onUpsertContact(
                                                        CrmContact(
                                                            name = lead.name,
                                                            company = lead.company,
                                                            email = lead.email,
                                                            phone = lead.phone,
                                                            status = ContactStatus.ACTIVE.name,
                                                            activityHistory = "Converted from Won deal (${formatMoney(lead.dealValue, currency)})"
                                                        )
                                                    )
                                                }
                                            ) { Text("Convert to Customer") }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { editingLead = lead; showLeadDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit lead")
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { onDeleteLead(lead) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete lead", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredContacts.isEmpty()) {
                EmptyModuleCard(
                    title = "No Contacts Yet",
                    description = "Build your customer & company directory with full activity history.",
                    icon = Icons.Filled.Contacts,
                    actionLabel = if (canEdit) "Add Contact" else null,
                    onAction = if (canEdit) { { editingContact = null; showContactDialog = true } } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredContacts, key = { it.id }) { c ->
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
                                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${c.company}${if (c.roleTitle.isNotBlank()) " • ${c.roleTitle}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(
                                        c.statusEnum().label,
                                        when (c.statusEnum()) {
                                            ContactStatus.VIP -> StatusWarning
                                            ContactStatus.ACTIVE -> NexoraEmerald
                                            ContactStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                if (c.email.isNotBlank() || c.phone.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        listOfNotNull(c.email.takeIf { it.isNotBlank() }, c.phone.takeIf { it.isNotBlank() }).joinToString(" • "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (c.activityHistory.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(c.activityHistory, style = MaterialTheme.typography.bodySmall)
                                }
                                if (canEdit) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { editingContact = c; showContactDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit contact")
                                        }
                                        if (canDelete) {
                                            IconButton(onClick = { onDeleteContact(c) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete contact", tint = StatusError)
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
    }

    if (showLeadDialog) {
        LeadFormDialog(
            initial = editingLead,
            onDismiss = { showLeadDialog = false },
            onSave = onUpsertLead
        )
    }
    if (showContactDialog) {
        ContactFormDialog(
            initial = editingContact,
            onDismiss = { showContactDialog = false },
            onSave = onUpsertContact
        )
    }
}
