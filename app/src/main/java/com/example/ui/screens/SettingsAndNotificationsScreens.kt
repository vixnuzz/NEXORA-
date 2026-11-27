package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ActivityLog
import com.example.model.Business
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.EmptyModuleCard
import com.example.ui.components.EnumDropdownRow
import com.example.ui.components.StatusPill
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraEmerald
import com.example.util.RolePermissions

@Composable
fun NotificationsScreen(
    logs: List<ActivityLog>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: (List<String>) -> Unit
) {
    val unreadIds = remember(logs) { logs.filter { it.unread }.map { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Notification Center & Audit Logs", style = MaterialTheme.typography.headlineSmall)
                Text("${unreadIds.size} unread events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (unreadIds.isNotEmpty()) {
                FilledTonalButton(onClick = { onMarkAllRead(unreadIds) }) {
                    Text("Mark All Read")
                }
            }
        }

        if (logs.isEmpty()) {
            EmptyModuleCard(
                title = "No Activity Yet",
                description = "All create, update, role-change, and system actions across NEXORA ONE are logged here in real time.",
                icon = Icons.Filled.Notifications
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (log.unread) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(log.module, NexoraCyan)
                                    Text(log.action, style = MaterialTheme.typography.labelSmall)
                                    if (log.unread) {
                                        StatusPill("NEW", NexoraEmerald)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${log.actorName} (${log.actorRole})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (log.unread) {
                                IconButton(onClick = { onMarkRead(log.id) }) {
                                    Icon(Icons.Filled.DoneAll, contentDescription = "Mark read")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndProfileScreen(
    business: Business?,
    businesses: List<Business>,
    userProfile: UserProfile?,
    activeRole: UserRole,
    isDarkTheme: Boolean,
    isBusy: Boolean,
    onUpdateOrg: (String, String, String, Double, String, String, UserRole) -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onSwitchBusiness: (String) -> Unit,
    onCreateNewBusiness: () -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onLoadDemoData: () -> Unit,
    onSignOut: () -> Unit
) {
    var orgName by remember(business?.id) { mutableStateOf(business?.name ?: "") }
    var orgIndustry by remember(business?.id) { mutableStateOf(business?.industry ?: "") }
    var orgCurrency by remember(business?.id) { mutableStateOf(business?.currency ?: "USD") }
    var orgTax by remember(business?.id) { mutableStateOf((business?.taxRate ?: 0.0).toString()) }
    var orgAddress by remember(business?.id) { mutableStateOf(business?.address ?: "") }
    var orgEmail by remember(business?.id) { mutableStateOf(business?.email ?: "") }
    var selectedRole by remember(business?.id, activeRole) { mutableStateOf(activeRole) }

    var profName by remember(userProfile?.userId) { mutableStateOf(userProfile?.displayName ?: "") }
    var profTitle by remember(userProfile?.userId) { mutableStateOf(userProfile?.jobTitle ?: "") }
    var profPhone by remember(userProfile?.userId) { mutableStateOf(userProfile?.phone ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Role & Permissions Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Role-Based Access Control (RBAC)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        RolePermissions.summaryFor(activeRole),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EnumDropdownRow(
                        label = "Active Workspace Role",
                        options = UserRole.entries,
                        selected = selectedRole,
                        labelProvider = { it.label }
                    ) { newRole ->
                        selectedRole = newRole
                        onSwitchRole(newRole)
                    }
                }
            }
        }

        // Organization Settings
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Organization Settings", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onCreateNewBusiness) { Text("+ Add Business") }
                    }
                    OutlinedTextField(orgName, { orgName = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(orgIndustry, { orgIndustry = it }, label = { Text("Industry") }, singleLine = true, modifier = Modifier.weight(1.3f))
                        OutlinedTextField(orgCurrency, { orgCurrency = it.uppercase().take(5) }, label = { Text("Currency") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(orgTax, { orgTax = it }, label = { Text("Tax %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.8f))
                        OutlinedTextField(orgEmail, { orgEmail = it }, label = { Text("Org Email") }, singleLine = true, modifier = Modifier.weight(1.4f))
                    }
                    OutlinedTextField(orgAddress, { orgAddress = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onUpdateOrg(
                                    orgName,
                                    orgIndustry,
                                    orgCurrency,
                                    orgTax.toDoubleOrNull() ?: 0.0,
                                    orgAddress,
                                    orgEmail,
                                    selectedRole
                                )
                            },
                            enabled = !isBusy && RolePermissions.canManageOrg(activeRole),
                            modifier = Modifier.testTag("save_org_settings_button")
                        ) { Text("Save Org Settings") }

                        OutlinedButton(
                            onClick = onLoadDemoData,
                            enabled = !isBusy
                        ) { Text("Seed [Demo] Data") }
                    }
                    if (!RolePermissions.canManageOrg(activeRole)) {
                        Text(
                            "Note: Switch role to Business Owner or Super Admin to edit organization settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Multiple Businesses Switcher
        if (businesses.size > 1) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your Business Workspaces (${businesses.size})", style = MaterialTheme.typography.titleMedium)
                        businesses.forEach { biz ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(biz.name, style = MaterialTheme.typography.titleSmall)
                                    Text("${biz.industry} • ${biz.currency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (biz.id == business?.id) {
                                    StatusPill("Active", NexoraEmerald)
                                } else {
                                    TextButton(onClick = { onSwitchBusiness(biz.id) }) { Text("Switch") }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Profile Management & Theme
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile Management", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Signed in as ${userProfile?.email ?: "Google User"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(profName, { profName = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(profTitle, { profTitle = it }, label = { Text("Job Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(profPhone, { profPhone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Theme Mode", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isDarkTheme, onCheckedChange = onToggleTheme)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onUpdateProfile(profName, profTitle, profPhone) },
                            enabled = !isBusy && profName.isNotBlank()
                        ) { Text("Save Profile") }

                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Secure Sign Out") }
                    }
                }
            }
        }
    }
}
