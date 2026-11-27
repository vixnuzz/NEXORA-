package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Business
import com.example.model.UserRole
import com.example.util.AppModule
import com.example.util.RolePermissions
import kotlinx.coroutines.launch

fun iconForModule(module: AppModule): ImageVector = when (module) {
    AppModule.DASHBOARD -> Icons.Filled.Dashboard
    AppModule.CRM -> Icons.Filled.Handshake
    AppModule.FINANCE -> Icons.Filled.AccountBalanceWallet
    AppModule.HR -> Icons.Filled.Badge
    AppModule.PROJECTS -> Icons.Filled.ViewKanban
    AppModule.INVENTORY -> Icons.Filled.Inventory2
    AppModule.SALES -> Icons.Filled.PointOfSale
    AppModule.MARKETING -> Icons.Filled.Campaign
    AppModule.SUPPORT -> Icons.Filled.SupportAgent
    AppModule.AI_ASSISTANT -> Icons.Filled.AutoAwesome
    AppModule.NOTIFICATIONS -> Icons.Filled.Notifications
    AppModule.SETTINGS -> Icons.Filled.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraAppShell(
    activeBusiness: Business?,
    businesses: List<Business>,
    activeRole: UserRole,
    selectedModule: AppModule,
    globalSearch: String,
    unreadNotifications: Int,
    isDarkTheme: Boolean,
    snackbarHostState: SnackbarHostState,
    onSelectModule: (AppModule) -> Unit,
    onSearchChange: (String) -> Unit,
    onSwitchBusiness: (String) -> Unit,
    onNewBusinessClick: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val allowedModules = remember(activeRole) { RolePermissions.accessibleModules(activeRole) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearchBar by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                SidebarContent(
                    activeBusiness = activeBusiness,
                    businesses = businesses,
                    activeRole = activeRole,
                    selectedModule = selectedModule,
                    allowedModules = allowedModules,
                    unreadNotifications = unreadNotifications,
                    isDarkTheme = isDarkTheme,
                    onSelectModule = onSelectModule,
                    onSwitchBusiness = onSwitchBusiness,
                    onNewBusinessClick = onNewBusinessClick,
                    onToggleTheme = onToggleTheme,
                    onSignOut = onSignOut,
                    modifier = Modifier
                        .width(272.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                Scaffold(
                    modifier = Modifier.weight(1f),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        NexoraTopBar(
                            showMenuButton = false,
                            activeBusiness = activeBusiness,
                            activeRole = activeRole,
                            selectedModule = selectedModule,
                            showSearchBar = showSearchBar,
                            globalSearch = globalSearch,
                            unreadNotifications = unreadNotifications,
                            onMenuClick = {},
                            onToggleSearch = { showSearchBar = !showSearchBar },
                            onSearchChange = onSearchChange,
                            onNotificationsClick = { onSelectModule(AppModule.NOTIFICATIONS) },
                            onAiClick = { onSelectModule(AppModule.AI_ASSISTANT) }
                        )
                    },
                    content = content
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(292.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        SidebarContent(
                            activeBusiness = activeBusiness,
                            businesses = businesses,
                            activeRole = activeRole,
                            selectedModule = selectedModule,
                            allowedModules = allowedModules,
                            unreadNotifications = unreadNotifications,
                            isDarkTheme = isDarkTheme,
                            onSelectModule = { mod ->
                                onSelectModule(mod)
                                scope.launch { drawerState.close() }
                            },
                            onSwitchBusiness = { id ->
                                onSwitchBusiness(id)
                                scope.launch { drawerState.close() }
                            },
                            onNewBusinessClick = {
                                scope.launch { drawerState.close() }
                                onNewBusinessClick()
                            },
                            onToggleTheme = onToggleTheme,
                            onSignOut = {
                                scope.launch { drawerState.close() }
                                onSignOut()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        NexoraTopBar(
                            showMenuButton = true,
                            activeBusiness = activeBusiness,
                            activeRole = activeRole,
                            selectedModule = selectedModule,
                            showSearchBar = showSearchBar,
                            globalSearch = globalSearch,
                            unreadNotifications = unreadNotifications,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onToggleSearch = { showSearchBar = !showSearchBar },
                            onSearchChange = onSearchChange,
                            onNotificationsClick = { onSelectModule(AppModule.NOTIFICATIONS) },
                            onAiClick = { onSelectModule(AppModule.AI_ASSISTANT) }
                        )
                    },
                    bottomBar = {
                        val quickTabs = listOf(
                            AppModule.DASHBOARD,
                            AppModule.CRM,
                            AppModule.FINANCE,
                            AppModule.PROJECTS,
                            AppModule.AI_ASSISTANT
                        ).filter { it in allowedModules }

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            quickTabs.forEach { mod ->
                                NavigationBarItem(
                                    selected = selectedModule == mod,
                                    onClick = { onSelectModule(mod) },
                                    icon = {
                                        Icon(
                                            imageVector = iconForModule(mod),
                                            contentDescription = mod.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            when (mod) {
                                                AppModule.DASHBOARD -> "Home"
                                                AppModule.CRM -> "CRM"
                                                AppModule.FINANCE -> "Finance"
                                                AppModule.PROJECTS -> "Projects"
                                                AppModule.AI_ASSISTANT -> "AI"
                                                else -> mod.title
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("bottom_nav_${mod.route}")
                                )
                            }
                        }
                    },
                    content = content
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexoraTopBar(
    showMenuButton: Boolean,
    activeBusiness: Business?,
    activeRole: UserRole,
    selectedModule: AppModule,
    showSearchBar: Boolean,
    globalSearch: String,
    unreadNotifications: Int,
    onMenuClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            navigationIcon = {
                if (showMenuButton) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("open_sidebar_button")
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open navigation sidebar")
                    }
                }
            },
            title = {
                Column {
                    Text(
                        text = selectedModule.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${activeBusiness?.name ?: "Workspace"} • ${activeRole.badgeShort}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.testTag("global_search_toggle")
                ) {
                    Icon(
                        imageVector = if (showSearchBar) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Global Search"
                    )
                }
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("notification_center_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge {
                                    Text(if (unreadNotifications > 99) "99+" else "$unreadNotifications")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
                IconButton(
                    onClick = onAiClick,
                    modifier = Modifier.testTag("ai_copilot_shortcut")
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "AI Copilot",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        if (showSearchBar) {
            OutlinedTextField(
                value = globalSearch,
                onValueChange = onSearchChange,
                placeholder = { Text("Search across CRM, Finance, HR, Projects, Inventory, Tickets...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (globalSearch.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("global_search_input")
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}

@Composable
private fun SidebarContent(
    activeBusiness: Business?,
    businesses: List<Business>,
    activeRole: UserRole,
    selectedModule: AppModule,
    allowedModules: Set<AppModule>,
    unreadNotifications: Int,
    isDarkTheme: Boolean,
    onSelectModule: (AppModule) -> Unit,
    onSwitchBusiness: (String) -> Unit,
    onNewBusinessClick: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bizMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(12.dp)) {
        // Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nexora_launcher),
                contentDescription = "NEXORA ONE logo",
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("NEXORA ONE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Enterprise SaaS Suite",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Multi-Business Switcher
        Box {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bizMenuExpanded = true }
                    .testTag("business_switcher_button")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Apartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBusiness?.name ?: "Select Business",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeBusiness?.currency ?: "USD"} • ${activeRole.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Icon(Icons.Filled.UnfoldMore, contentDescription = "Switch organization")
                }
            }

            DropdownMenu(
                expanded = bizMenuExpanded,
                onDismissRequest = { bizMenuExpanded = false }
            ) {
                businesses.forEach { biz ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(biz.name, fontWeight = FontWeight.SemiBold)
                                Text(biz.industry, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        leadingIcon = {
                            if (biz.id == activeBusiness?.id) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Filled.Business, contentDescription = null)
                            }
                        },
                        onClick = {
                            bizMenuExpanded = false
                            onSwitchBusiness(biz.id)
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("+ New Business Workspace", color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        bizMenuExpanded = false
                        onNewBusinessClick()
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Modules List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(AppModule.entries.filter { it in allowedModules }) { mod ->
                val isSelected = mod == selectedModule
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mod.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (mod == AppModule.NOTIFICATIONS && unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "$unreadNotifications",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    },
                    selected = isSelected,
                    onClick = { onSelectModule(mod) },
                    icon = { Icon(iconForModule(mod), contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("drawer_nav_${mod.route}")
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Theme & Sign Out Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("Dark Theme", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = isDarkTheme,
                onCheckedChange = onToggleTheme,
                modifier = Modifier.testTag("theme_switch")
            )
        }

        TextButton(
            onClick = onSignOut,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Secure Sign Out")
        }
    }
}
