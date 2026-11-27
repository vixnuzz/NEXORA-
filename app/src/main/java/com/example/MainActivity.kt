package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.NexoraRepository
import com.example.data.PreferencesRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.model.KanbanColumn
import com.example.ui.components.AuthScreen
import com.example.ui.components.BusinessOnboardingScreen
import com.example.ui.components.CreateBusinessDialog
import com.example.ui.components.NexoraAppShell
import com.example.ui.screens.*
import com.example.ui.theme.NexoraOneTheme
import com.example.util.AppModule
import com.example.util.CsvExporter
import com.example.util.RolePermissions
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.BusinessWorkspaceData
import com.example.viewmodel.DataUiState
import com.example.viewmodel.NexoraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent BEFORE Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val prefsRepo = remember(applicationContext) { PreferencesRepository(applicationContext) }
            val darkPref by prefsRepo.darkThemeFlow.collectAsStateWithLifecycle(initialValue = null)
            val useDarkTheme = darkPref ?: isSystemInDarkTheme()

            NexoraOneTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NexoraAppRoot(isDarkTheme = useDarkTheme)
                }
            }
        }
    }
}

@Composable
private fun NexoraAppRoot(isDarkTheme: Boolean) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedNexoraWorkspace(
                currentUserId = user.uid,
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun AuthenticatedNexoraWorkspace(
    currentUserId: String,
    displayName: String,
    email: String,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NexoraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NexoraViewModel(
                    repository = NexoraRepository(db),
                    prefsRepository = PreferencesRepository(app),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val aiViewModel: AiAssistantViewModel = viewModel(key = "ai_$currentUserId")

    val businessesState by viewModel.businessesState.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val selectedModule by viewModel.selectedModule.collectAsStateWithLifecycle()
    val globalSearch by viewModel.globalSearch.collectAsStateWithLifecycle()
    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateBusinessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // BackHandler returns to Dashboard from any secondary module
    BackHandler(enabled = selectedModule != AppModule.DASHBOARD) {
        viewModel.selectModule(AppModule.DASHBOARD)
    }

    when (val bState = businessesState) {
        is DataUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DataUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(bState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is DataUiState.Success -> {
            val businesses = bState.data
            if (businesses.isEmpty()) {
                BusinessOnboardingScreen(
                    isBusy = isBusy,
                    userEmail = email,
                    onCreateBusiness = viewModel::createBusiness
                )
            } else {
                val workspace = (workspaceState as? DataUiState.Success)?.data ?: BusinessWorkspaceData()
                val currency = activeBusiness?.currency ?: "USD"
                val defaultTax = activeBusiness?.taxRate ?: 0.0
                val canExport = RolePermissions.canExportCsv(activeRole)

                val exportCsv: (String, List<String>, List<List<String>>) -> Unit = { prefix, headers, rows ->
                    CsvExporter.exportAndShare(context, prefix, headers, rows)
                        .onSuccess { fileName -> viewModel.showMessage("Exported $fileName") }
                        .onFailure { err -> viewModel.showMessage(err.localizedMessage ?: "Export failed") }
                }

                NexoraAppShell(
                    activeBusiness = activeBusiness,
                    businesses = businesses,
                    activeRole = activeRole,
                    selectedModule = selectedModule,
                    globalSearch = globalSearch,
                    unreadNotifications = metrics.unreadNotifications,
                    isDarkTheme = isDarkTheme,
                    snackbarHostState = snackbarHostState,
                    onSelectModule = viewModel::selectModule,
                    onSearchChange = viewModel::setGlobalSearch,
                    onSwitchBusiness = viewModel::switchBusiness,
                    onNewBusinessClick = { showCreateBusinessDialog = true },
                    onToggleTheme = viewModel::toggleDarkTheme,
                    onSignOut = { signOut(credentialManager, scope) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (workspaceState is DataUiState.Error) {
                            val errMsg = (workspaceState as DataUiState.Error).message
                            Text(
                                text = "Sync error: $errMsg",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            AnimatedContent(
                                targetState = selectedModule,
                                label = "module_transition"
                            ) { mod ->
                                val canEdit = RolePermissions.canCreateOrEdit(activeRole, mod)
                                val canDelete = RolePermissions.canDelete(activeRole, mod)

                                when (mod) {
                                    AppModule.DASHBOARD -> DashboardScreen(
                                        business = activeBusiness,
                                        metrics = metrics,
                                        activeRole = activeRole,
                                        onNavigate = viewModel::selectModule,
                                        onToggleTaskDone = { task ->
                                            val next = if (task.columnEnum() == KanbanColumn.DONE) {
                                                KanbanColumn.TODO.name
                                            } else {
                                                KanbanColumn.DONE.name
                                            }
                                            viewModel.upsertProjectTask(task.copy(kanbanColumn = next))
                                        },
                                        onLoadDemoData = viewModel::seedDemoDataForActiveBusiness
                                    )
                                    AppModule.CRM -> CrmScreen(
                                        leads = workspace.leads,
                                        contacts = workspace.contacts,
                                        currency = currency,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertLead = viewModel::upsertLead,
                                        onDeleteLead = { viewModel.deleteRecord("crm_leads", it.id, it.name) },
                                        onUpsertContact = viewModel::upsertContact,
                                        onDeleteContact = { viewModel.deleteRecord("crm_contacts", it.id, it.name) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.FINANCE -> FinanceScreen(
                                        records = workspace.financeRecords,
                                        invoices = workspace.invoices,
                                        metrics = metrics,
                                        currency = currency,
                                        defaultTaxRate = defaultTax,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertRecord = viewModel::upsertFinanceRecord,
                                        onDeleteRecord = { viewModel.deleteRecord("finance_records", it.id, it.title) },
                                        onUpsertInvoice = viewModel::upsertInvoice,
                                        onDeleteInvoice = { viewModel.deleteRecord("invoices", it.id, it.invoiceNumber) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.HR -> HrScreen(
                                        employees = workspace.employees,
                                        currency = currency,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertEmployee = viewModel::upsertEmployee,
                                        onDeleteEmployee = { viewModel.deleteRecord("employees", it.id, it.fullName) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.PROJECTS -> ProjectsScreen(
                                        projects = workspace.projects,
                                        tasks = workspace.tasks,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertProject = viewModel::upsertProject,
                                        onDeleteProject = { viewModel.deleteRecord("projects", it.id, it.name) },
                                        onUpsertTask = viewModel::upsertProjectTask,
                                        onDeleteTask = { viewModel.deleteRecord("project_tasks", it.id, it.title) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.INVENTORY -> InventoryScreen(
                                        products = workspace.products,
                                        purchaseOrders = workspace.purchaseOrders,
                                        currency = currency,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertProduct = viewModel::upsertProduct,
                                        onDeleteProduct = { viewModel.deleteRecord("inventory_products", it.id, it.name) },
                                        onUpsertPurchaseOrder = viewModel::upsertPurchaseOrder,
                                        onDeletePurchaseOrder = { viewModel.deleteRecord("purchase_orders", it.id, it.poNumber) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.SALES -> SalesScreen(
                                        orders = workspace.salesOrders,
                                        currency = currency,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertOrder = viewModel::upsertSalesOrder,
                                        onDeleteOrder = { viewModel.deleteRecord("sales_orders", it.id, it.orderNumber) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.MARKETING -> MarketingScreen(
                                        campaigns = workspace.campaigns,
                                        currency = currency,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertCampaign = viewModel::upsertCampaign,
                                        onDeleteCampaign = { viewModel.deleteRecord("marketing_campaigns", it.id, it.name) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.SUPPORT -> SupportDeskScreen(
                                        tickets = workspace.tickets,
                                        searchQuery = globalSearch,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        canExport = canExport,
                                        onUpsertTicket = viewModel::upsertSupportTicket,
                                        onDeleteTicket = { viewModel.deleteRecord("support_tickets", it.id, it.ticketNumber) },
                                        onExportCsv = exportCsv
                                    )
                                    AppModule.AI_ASSISTANT -> AiAssistantScreen(
                                        aiViewModel = aiViewModel,
                                        business = activeBusiness,
                                        metrics = metrics,
                                        workspace = workspace
                                    )
                                    AppModule.NOTIFICATIONS -> NotificationsScreen(
                                        logs = workspace.activityLogs,
                                        onMarkRead = viewModel::markNotificationRead,
                                        onMarkAllRead = viewModel::markAllNotificationsRead
                                    )
                                    AppModule.SETTINGS -> SettingsAndProfileScreen(
                                        business = activeBusiness,
                                        businesses = businesses,
                                        userProfile = userProfile,
                                        activeRole = activeRole,
                                        isDarkTheme = isDarkTheme,
                                        isBusy = isBusy,
                                        onUpdateOrg = viewModel::updateOrganizationSettings,
                                        onSwitchRole = viewModel::switchRole,
                                        onSwitchBusiness = viewModel::switchBusiness,
                                        onCreateNewBusiness = { showCreateBusinessDialog = true },
                                        onUpdateProfile = viewModel::updateUserProfile,
                                        onToggleTheme = viewModel::toggleDarkTheme,
                                        onLoadDemoData = viewModel::seedDemoDataForActiveBusiness,
                                        onSignOut = { signOut(credentialManager, scope) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateBusinessDialog) {
        CreateBusinessDialog(
            isBusy = isBusy,
            defaultEmail = email,
            onDismiss = { showCreateBusinessDialog = false },
            onCreate = viewModel::createBusiness
        )
    }
}
