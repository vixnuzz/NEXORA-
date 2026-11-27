package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import com.example.util.AppModule
import com.example.util.RolePermissions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

data class BusinessWorkspaceData(
    val leads: List<CrmLead> = emptyList(),
    val contacts: List<CrmContact> = emptyList(),
    val financeRecords: List<FinanceRecord> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val projects: List<Project> = emptyList(),
    val tasks: List<ProjectTask> = emptyList(),
    val products: List<InventoryProduct> = emptyList(),
    val purchaseOrders: List<PurchaseOrder> = emptyList(),
    val salesOrders: List<SalesOrder> = emptyList(),
    val campaigns: List<MarketingCampaign> = emptyList(),
    val tickets: List<SupportTicket> = emptyList(),
    val activityLogs: List<ActivityLog> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class NexoraViewModel(
    private val repository: NexoraRepository,
    private val prefsRepository: PreferencesRepository,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String
) : ViewModel() {

    private val _selectedModule = MutableStateFlow(AppModule.DASHBOARD)
    val selectedModule: StateFlow<AppModule> = _selectedModule.asStateFlow()

    private val _globalSearch = MutableStateFlow("")
    val globalSearch: StateFlow<String> = _globalSearch.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _selectedBusinessIdOverride = MutableStateFlow<String?>(null)

    val darkThemePreference: StateFlow<Boolean?> = prefsRepository.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { error ->
            unwrapFirestoreError(error, "UserProfile")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val businessesState: StateFlow<DataUiState<List<Business>>> = repository.observeBusinesses()
        .map<List<Business>, DataUiState<List<Business>>> { DataUiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error, "Businesses")
            emit(DataUiState.Error(root.message ?: "Failed to load businesses"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val activeBusiness: StateFlow<Business?> = combine(
        businessesState,
        userProfile,
        prefsRepository.activeBusinessIdFlow,
        _selectedBusinessIdOverride
    ) { bizState, profile, storedId, overrideId ->
        val list = (bizState as? DataUiState.Success)?.data.orEmpty()
        if (list.isEmpty()) return@combine null
        val preferred = overrideId
            ?: profile?.activeBusinessId?.takeIf { it.isNotBlank() }
            ?: storedId
        list.firstOrNull { it.id == preferred } ?: list.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeRole: StateFlow<UserRole> = activeBusiness
        .map { it?.roleEnum() ?: UserRole.BUSINESS_OWNER }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserRole.BUSINESS_OWNER)

    private val activeBusinessIdFlow: Flow<String> = activeBusiness
        .map { it?.id.orEmpty() }
        .distinctUntilChanged()

    val workspaceState: StateFlow<DataUiState<BusinessWorkspaceData>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) {
                flowOf(DataUiState.Success(BusinessWorkspaceData()))
            } else {
                val part1 = combine(
                    repository.observeCrmLeads(bizId),
                    repository.observeCrmContacts(bizId),
                    repository.observeFinanceRecords(bizId),
                    repository.observeInvoices(bizId),
                    repository.observeEmployees(bizId)
                ) { leads, contacts, finance, invoices, employees ->
                    Part1(leads, contacts, finance, invoices, employees)
                }
                val part2 = combine(
                    repository.observeProjects(bizId),
                    repository.observeProjectTasks(bizId),
                    repository.observeInventoryProducts(bizId),
                    repository.observePurchaseOrders(bizId)
                ) { projects, tasks, products, pos ->
                    Part2(projects, tasks, products, pos)
                }
                val part3 = combine(
                    repository.observeSalesOrders(bizId),
                    repository.observeMarketingCampaigns(bizId),
                    repository.observeSupportTickets(bizId),
                    repository.observeActivityLogs(bizId)
                ) { orders, campaigns, tickets, logs ->
                    Part3(orders, campaigns, tickets, logs)
                }
                combine(part1, part2, part3) { p1, p2, p3 ->
                    DataUiState.Success(
                        BusinessWorkspaceData(
                            leads = p1.leads,
                            contacts = p1.contacts,
                            financeRecords = p1.finance,
                            invoices = p1.invoices,
                            employees = p1.employees,
                            projects = p2.projects,
                            tasks = p2.tasks,
                            products = p2.products,
                            purchaseOrders = p2.pos,
                            salesOrders = p3.orders,
                            campaigns = p3.campaigns,
                            tickets = p3.tickets,
                            activityLogs = p3.logs
                        )
                    ) as DataUiState<BusinessWorkspaceData>
                }.catch { error ->
                    val root = unwrapFirestoreError(error, "Workspace:$bizId")
                    emit(DataUiState.Error(root.message ?: "Failed to sync workspace data"))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val dashboardMetrics: StateFlow<DashboardMetrics> = workspaceState
        .map { state ->
            val data = (state as? DataUiState.Success)?.data ?: return@map DashboardMetrics()
            computeDashboardMetrics(data)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardMetrics())

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName, initialEmail)
        }
    }

    private fun unwrapFirestoreError(error: Throwable, tag: String): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w("NexoraVM", "Error observing $tag", root)
        return root
    }

    private fun computeDashboardMetrics(d: BusinessWorkspaceData): DashboardMetrics {
        val ledgerIncome = d.financeRecords.filter { it.typeEnum() == FinanceType.INCOME }.sumOf { it.amount }
        val paidInvoices = d.invoices.filter { it.statusEnum() == InvoiceStatus.PAID }.sumOf { it.amount }
        val activeSales = d.salesOrders.filter { it.statusEnum() != OrderStatus.CANCELLED }.sumOf { it.totalAmount }
        val totalRevenue = ledgerIncome + paidInvoices + activeSales

        val ledgerExpenses = d.financeRecords.filter { it.typeEnum() == FinanceType.EXPENSE }.sumOf { it.amount }
        val receivedPoCost = d.purchaseOrders.filter { it.statusEnum() == PoStatus.RECEIVED }.sumOf { it.totalCost }
        val totalExpenses = ledgerExpenses + receivedPoCost

        val activeCustomers = d.contacts.count {
            it.statusEnum() == ContactStatus.ACTIVE || it.statusEnum() == ContactStatus.VIP
        }
        val pendingInvoices = d.invoices.filter {
            it.docTypeEnum() == InvoiceDocType.INVOICE &&
                it.statusEnum() in setOf(InvoiceStatus.PENDING, InvoiceStatus.SENT, InvoiceStatus.OVERDUE)
        }
        val activeProjects = d.projects.count { it.statusEnum() == ProjectStatus.ACTIVE }
        val lowStockCount = d.products.count { it.needsRestock }
        val openTickets = d.tickets.count {
            it.statusEnum() !in setOf(TicketStatus.RESOLVED, TicketStatus.CLOSED)
        }
        val pipelineValue = d.leads
            .filter { it.stageEnum() != LeadStage.LOST && it.stageEnum() != LeadStage.WON }
            .sumOf { it.dealValue }

        val salesByMonth = mutableMapOf<String, Double>()
        val expByMonth = mutableMapOf<String, Double>()
        d.salesOrders.filter { it.statusEnum() != OrderStatus.CANCELLED }.forEach { order ->
            val key = order.monthLabel.ifBlank { "2026-11" }
            salesByMonth[key] = (salesByMonth[key] ?: 0.0) + order.totalAmount
        }
        d.financeRecords.forEach { rec ->
            val key = rec.dateStr.take(7).ifBlank { "2026-11" }
            if (rec.typeEnum() == FinanceType.INCOME) {
                salesByMonth[key] = (salesByMonth[key] ?: 0.0) + rec.amount
            } else {
                expByMonth[key] = (expByMonth[key] ?: 0.0) + rec.amount
            }
        }
        val allMonths = (salesByMonth.keys + expByMonth.keys).distinct().sorted().takeLast(6)
        val series = allMonths.map { m ->
            MonthlyMetricPoint(
                monthLabel = m,
                salesAmount = salesByMonth[m] ?: 0.0,
                expenseAmount = expByMonth[m] ?: 0.0
            )
        }

        val upcomingTasks = d.tasks
            .filter { it.columnEnum() != KanbanColumn.DONE }
            .sortedByDescending { it.reminderActive }
            .take(5)

        return DashboardMetrics(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netProfit = totalRevenue - totalExpenses,
            activeCustomers = activeCustomers,
            totalEmployees = d.employees.size,
            pendingInvoicesCount = pendingInvoices.size,
            pendingInvoicesTotal = pendingInvoices.sumOf { it.amount },
            activeProjects = activeProjects,
            lowStockCount = lowStockCount,
            openTicketsCount = openTickets,
            pipelineValue = pipelineValue,
            monthlySeries = series,
            recentTransactions = d.financeRecords.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }.take(5),
            recentActivities = d.activityLogs.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }.take(6),
            upcomingTasks = upcomingTasks,
            unreadNotifications = d.activityLogs.count { it.unread }
        )
    }

    fun selectModule(module: AppModule) {
        val role = activeRole.value
        val allowed = RolePermissions.accessibleModules(role)
        if (module in allowed) {
            _selectedModule.value = module
        } else {
            _snackbarMessage.value = "Access restricted for role ${role.label}"
        }
    }

    fun setGlobalSearch(query: String) {
        _globalSearch.value = query
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDarkTheme(enabled) }
    }

    fun switchBusiness(businessId: String) {
        viewModelScope.launch {
            _selectedBusinessIdOverride.value = businessId
            prefsRepository.setActiveBusinessId(businessId)
            repository.setActiveBusinessOnProfile(businessId)
        }
    }

    private fun actorName(): String = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
    private fun actorRole(): String = activeRole.value.label
    private fun currentBizId(): String = activeBusiness.value?.id.orEmpty()

    private fun launchMutation(
        successMsg: String,
        block: suspend (bizId: String, actor: String, role: String) -> Result<Unit>
    ) {
        val bizId = currentBizId()
        if (bizId.isBlank()) {
            _snackbarMessage.value = "Create or select a business workspace first."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = block(bizId, actorName(), actorRole())
            _isBusy.value = false
            res.onSuccess { _snackbarMessage.value = successMsg }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Operation failed" }
        }
    }

    fun createBusiness(
        name: String,
        industry: String,
        currency: String,
        taxRate: Double,
        address: String,
        email: String,
        loadDemoData: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createBusiness(name, industry, currency, taxRate, address, email)
            res.onSuccess { newBizId ->
                _selectedBusinessIdOverride.value = newBizId
                prefsRepository.setActiveBusinessId(newBizId)
                if (loadDemoData) {
                    repository.seedEnterpriseDemoData(newBizId, actorName(), UserRole.BUSINESS_OWNER.label)
                }
                _snackbarMessage.value = "Workspace '$name' is ready"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to create workspace"
            }
            _isBusy.value = false
        }
    }

    fun updateOrganizationSettings(
        name: String,
        industry: String,
        currency: String,
        taxRate: Double,
        address: String,
        email: String,
        role: UserRole
    ) {
        val bizId = currentBizId()
        if (bizId.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateBusiness(bizId, name, industry, currency, taxRate, address, email, role)
                .onSuccess {
                    if (_selectedModule.value !in RolePermissions.accessibleModules(role)) {
                        _selectedModule.value = AppModule.DASHBOARD
                    }
                    _snackbarMessage.value = "Organization settings saved"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update settings" }
            _isBusy.value = false
        }
    }

    fun switchRole(role: UserRole) {
        val bizId = currentBizId()
        if (bizId.isBlank()) return
        viewModelScope.launch {
            repository.updateBusinessRole(bizId, role)
                .onSuccess {
                    if (_selectedModule.value !in RolePermissions.accessibleModules(role)) {
                        _selectedModule.value = AppModule.DASHBOARD
                    }
                    _snackbarMessage.value = "Active role set to ${role.label}"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to switch role" }
        }
    }

    fun updateUserProfile(displayName: String, jobTitle: String, phone: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateUserProfile(displayName, jobTitle, phone, currentBizId().takeIf { it.isNotBlank() })
                .onSuccess { _snackbarMessage.value = "Profile updated" }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Profile update failed" }
            _isBusy.value = false
        }
    }

    fun seedDemoDataForActiveBusiness() {
        launchMutation("Sample enterprise demo data loaded") { bizId, actor, role ->
            repository.seedEnterpriseDemoData(bizId, actor, role)
        }
    }

    fun upsertLead(lead: CrmLead) = launchMutation("Lead saved") { bizId, actor, role ->
        repository.upsertLead(bizId, lead, actor, role)
    }

    fun upsertContact(contact: CrmContact) = launchMutation("Contact saved") { bizId, actor, role ->
        repository.upsertContact(bizId, contact, actor, role)
    }

    fun upsertFinanceRecord(record: FinanceRecord) = launchMutation("Transaction recorded") { bizId, actor, role ->
        repository.upsertFinanceRecord(bizId, record, actor, role)
    }

    fun upsertInvoice(invoice: Invoice) = launchMutation("${invoice.docTypeEnum().label} saved") { bizId, actor, role ->
        repository.upsertInvoice(bizId, invoice, actor, role)
    }

    fun upsertEmployee(employee: Employee) = launchMutation("Employee record saved") { bizId, actor, role ->
        repository.upsertEmployee(bizId, employee, actor, role)
    }

    fun upsertProject(project: Project) = launchMutation("Project saved") { bizId, actor, role ->
        repository.upsertProject(bizId, project, actor, role)
    }

    fun upsertProjectTask(task: ProjectTask) = launchMutation("Task updated") { bizId, actor, role ->
        repository.upsertProjectTask(bizId, task, actor, role)
    }

    fun upsertProduct(product: InventoryProduct) = launchMutation("Product saved") { bizId, actor, role ->
        repository.upsertProduct(bizId, product, actor, role)
    }

    fun upsertPurchaseOrder(po: PurchaseOrder) = launchMutation("Purchase order saved") { bizId, actor, role ->
        repository.upsertPurchaseOrder(bizId, po, actor, role)
    }

    fun upsertSalesOrder(order: SalesOrder) = launchMutation("Sales order saved") { bizId, actor, role ->
        repository.upsertSalesOrder(bizId, order, actor, role)
    }

    fun upsertCampaign(campaign: MarketingCampaign) = launchMutation("Campaign saved") { bizId, actor, role ->
        repository.upsertCampaign(bizId, campaign, actor, role)
    }

    fun upsertSupportTicket(ticket: SupportTicket) = launchMutation("Ticket saved") { bizId, actor, role ->
        repository.upsertSupportTicket(bizId, ticket, actor, role)
    }

    fun deleteRecord(collection: String, docId: String, label: String) =
        launchMutation("Deleted $label") { bizId, actor, role ->
            repository.deleteDoc(bizId, collection, docId, label, actor, role)
        }

    fun markNotificationRead(logId: String) {
        val bizId = currentBizId()
        if (bizId.isBlank()) return
        viewModelScope.launch { repository.markNotificationRead(bizId, logId) }
    }

    fun markAllNotificationsRead(logIds: List<String>) {
        val bizId = currentBizId()
        if (bizId.isBlank() || logIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(bizId, logIds)
                .onSuccess { _snackbarMessage.value = "All notifications marked read" }
        }
    }

    private data class Part1(
        val leads: List<CrmLead>,
        val contacts: List<CrmContact>,
        val finance: List<FinanceRecord>,
        val invoices: List<Invoice>,
        val employees: List<Employee>
    )

    private data class Part2(
        val projects: List<Project>,
        val tasks: List<ProjectTask>,
        val products: List<InventoryProduct>,
        val pos: List<PurchaseOrder>
    )

    private data class Part3(
        val orders: List<SalesOrder>,
        val campaigns: List<MarketingCampaign>,
        val tickets: List<SupportTicket>,
        val logs: List<ActivityLog>
    )
}
