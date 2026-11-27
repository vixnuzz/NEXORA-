package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.*

@Composable
fun EmployeeFormDialog(
    initial: Employee?,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var department by remember { mutableStateOf(initial?.department ?: "Engineering") }
    var jobTitle by remember { mutableStateOf(initial?.jobTitle ?: "") }
    var role by remember { mutableStateOf(initial?.roleEnum() ?: UserRole.EMPLOYEE) }
    var salary by remember { mutableStateOf(if (initial != null) initial.salary.toString() else "85000") }
    var attendance by remember { mutableStateOf(initial?.attendanceEnum() ?: AttendanceStatus.PRESENT) }
    var leaveStatus by remember { mutableStateOf(initial?.leaveStatusEnum() ?: LeaveRequestStatus.NONE) }
    var payroll by remember { mutableStateOf(initial?.payrollEnum() ?: PayrollStatus.PENDING) }
    var score by remember { mutableStateOf((initial?.performanceScore ?: 88L).toString()) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Onboard Employee" else "Edit Employee") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fullName, { fullName = it; err = null }, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(jobTitle, { jobTitle = it; err = null }, label = { Text("Job Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(department, { department = it; err = null }, label = { Text("Department *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("System Role", UserRole.entries, role, { it.label }) { role = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(salary, { salary = it }, label = { Text("Annual Salary") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(score, { score = it }, label = { Text("Perf (0-100)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.8f))
                }
                EnumDropdownRow("Attendance", AttendanceStatus.entries, attendance, { it.label }) { attendance = it }
                EnumDropdownRow("Leave Request", LeaveRequestStatus.entries, leaveStatus, { it.label }) { leaveStatus = it }
                EnumDropdownRow("Payroll", PayrollStatus.entries, payroll, { it.label }) { payroll = it }
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.isBlank() || department.isBlank() || jobTitle.isBlank()) {
                    err = "Name, department, and job title are required."
                } else {
                    onSave(
                        (initial ?: Employee()).copy(
                            fullName = fullName.trim(),
                            email = email.trim(),
                            department = department.trim(),
                            jobTitle = jobTitle.trim(),
                            role = role.name,
                            salary = salary.toDoubleOrNull() ?: 0.0,
                            attendanceStatus = attendance.name,
                            leaveRequestStatus = leaveStatus.name,
                            payrollStatus = payroll.name,
                            performanceScore = (score.toLongOrNull() ?: 85L).coerceIn(0L, 100L)
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProjectFormDialog(
    initial: Project?,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: ProjectStatus.ACTIVE) }
    var deadline by remember { mutableStateOf(initial?.deadline ?: "2026-12-30") }
    var members by remember { mutableStateOf(initial?.memberNames ?: "") }
    var progress by remember { mutableStateOf((initial?.progressPercent ?: 15L).toString()) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Project" else "Edit Project") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it; err = null }, label = { Text("Project Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("Status", ProjectStatus.entries, status, { it.label }) { status = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(deadline, { deadline = it }, label = { Text("Deadline") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(progress, { progress = it }, label = { Text("Progress %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.8f))
                }
                OutlinedTextField(members, { members = it }, label = { Text("Team Members") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) err = "Project name is required."
                else {
                    onSave(
                        (initial ?: Project()).copy(
                            name = name.trim(),
                            description = description.trim(),
                            status = status.name,
                            deadline = deadline.ifBlank { "2026-12-30" },
                            memberNames = members.trim(),
                            progressPercent = (progress.toLongOrNull() ?: 0L).coerceIn(0L, 100L)
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TaskFormDialog(
    initial: ProjectTask?,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (ProjectTask) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var selectedProj by remember {
        mutableStateOf(projects.firstOrNull { it.id == initial?.projectId } ?: projects.firstOrNull())
    }
    var subtasks by remember { mutableStateOf(initial?.subtasksSummary ?: "") }
    var assignee by remember { mutableStateOf(initial?.assignee ?: "") }
    var priority by remember { mutableStateOf(initial?.priorityEnum() ?: TaskPriority.MEDIUM) }
    var column by remember { mutableStateOf(initial?.columnEnum() ?: KanbanColumn.TODO) }
    var dueDate by remember { mutableStateOf(initial?.dueDate ?: "2026-12-08") }
    var reminder by remember { mutableStateOf(initial?.reminderActive ?: true) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Kanban Task" else "Edit Task") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it; err = null }, label = { Text("Task Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (projects.isNotEmpty()) {
                    EnumDropdownRow("Project", projects, selectedProj ?: projects.first(), { it.name }) { selectedProj = it }
                }
                OutlinedTextField(subtasks, { subtasks = it }, label = { Text("Subtasks (semicolon separated)") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(assignee, { assignee = it }, label = { Text("Assignee") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("Priority", TaskPriority.entries, priority, { it.label }) { priority = it }
                EnumDropdownRow("Kanban Column", KanbanColumn.entries, column, { it.label }) { column = it }
                OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Dashboard Reminder", modifier = Modifier.weight(1f))
                    Switch(checked = reminder, onCheckedChange = { reminder = it })
                }
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) err = "Task title is required."
                else {
                    onSave(
                        (initial ?: ProjectTask()).copy(
                            projectId = selectedProj?.id ?: initial?.projectId ?: "general",
                            projectName = selectedProj?.name ?: initial?.projectName ?: "General Workspace",
                            title = title.trim(),
                            subtasksSummary = subtasks.trim(),
                            assignee = assignee.trim(),
                            priority = priority.name,
                            kanbanColumn = column.name,
                            dueDate = dueDate.ifBlank { "2026-12-08" },
                            reminderActive = reminder
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProductFormDialog(
    initial: InventoryProduct?,
    onDismiss: () -> Unit,
    onSave: (InventoryProduct) -> Unit
) {
    var sku by remember { mutableStateOf(initial?.sku ?: "NX-${(100..999).random()}") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Hardware") }
    var supplier by remember { mutableStateOf(initial?.supplierName ?: "") }
    var unitPrice by remember { mutableStateOf(if (initial != null) initial.unitPrice.toString() else "") }
    var stockQty by remember { mutableStateOf(if (initial != null) initial.stockQty.toString() else "20") }
    var reorder by remember { mutableStateOf(if (initial != null) initial.reorderLevel.toString() else "10") }
    var note by remember { mutableStateOf(initial?.lastMovementNote ?: "Manual stock update") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Inventory Product" else "Edit Product") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(sku, { sku = it; err = null }, label = { Text("SKU *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it; err = null }, label = { Text("Product Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("Supplier") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(unitPrice, { unitPrice = it }, label = { Text("Unit Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(stockQty, { stockQty = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(reorder, { reorder = it }, label = { Text("Reorder") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.7f))
                }
                OutlinedTextField(note, { note = it }, label = { Text("Movement Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (sku.isBlank() || name.isBlank()) err = "SKU and product name are required."
                else {
                    onSave(
                        (initial ?: InventoryProduct()).copy(
                            sku = sku.trim(),
                            name = name.trim(),
                            category = category.trim(),
                            supplierName = supplier.trim(),
                            unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                            stockQty = (stockQty.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                            reorderLevel = (reorder.toLongOrNull() ?: 10L).coerceAtLeast(0L),
                            lastMovementNote = note.trim()
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PurchaseOrderFormDialog(
    products: List<InventoryProduct>,
    onDismiss: () -> Unit,
    onSave: (PurchaseOrder) -> Unit
) {
    var poNumber by remember { mutableStateOf("PO-${(1000..9999).random()}") }
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var customProductName by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf(products.firstOrNull()?.supplierName ?: "") }
    var qty by remember { mutableStateOf("20") }
    var totalCost by remember { mutableStateOf("5000") }
    var status by remember { mutableStateOf(PoStatus.ORDERED) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Purchase Order") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(poNumber, { poNumber = it; err = null }, label = { Text("PO Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (products.isNotEmpty()) {
                    EnumDropdownRow("Catalog Product", products, selectedProduct ?: products.first(), { "${it.name} (${it.stockQty} left)" }) {
                        selectedProduct = it
                        supplier = it.supplierName
                    }
                } else {
                    OutlinedTextField(customProductName, { customProductName = it; err = null }, label = { Text("Product Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(supplier, { supplier = it }, label = { Text("Supplier Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(qty, { qty = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(totalCost, { totalCost = it }, label = { Text("Total Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                EnumDropdownRow("Status", PoStatus.entries, status, { it.label }) { status = it }
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val pName = selectedProduct?.name ?: customProductName.trim()
                if (poNumber.isBlank() || pName.isBlank()) err = "PO number and product name are required."
                else {
                    onSave(
                        PurchaseOrder(
                            poNumber = poNumber.trim(),
                            supplierName = supplier.trim().ifBlank { "Primary Supplier" },
                            productId = selectedProduct?.id ?: "",
                            productName = pName,
                            quantity = (qty.toLongOrNull() ?: 1L).coerceAtLeast(1L),
                            totalCost = totalCost.toDoubleOrNull() ?: 0.0,
                            status = status.name
                        )
                    )
                    onDismiss()
                }
            }) { Text("Create PO") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SalesOrderFormDialog(
    initial: SalesOrder?,
    onDismiss: () -> Unit,
    onSave: (SalesOrder) -> Unit
) {
    var orderNumber by remember { mutableStateOf(initial?.orderNumber ?: "SO-${(1000..9999).random()}") }
    var customer by remember { mutableStateOf(initial?.customerName ?: "") }
    var product by remember { mutableStateOf(initial?.productName ?: "") }
    var qty by remember { mutableStateOf((initial?.quantity ?: 1L).toString()) }
    var subtotal by remember { mutableStateOf(if (initial != null) initial.subtotal.toString() else "") }
    var discount by remember { mutableStateOf((initial?.discountPercent ?: 0.0).toString()) }
    var monthLabel by remember { mutableStateOf(initial?.monthLabel ?: "2026-11") }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: OrderStatus.PROCESSING) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Sales Order" else "Edit Sales Order") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(orderNumber, { orderNumber = it; err = null }, label = { Text("Order Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(customer, { customer = it; err = null }, label = { Text("Customer Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(product, { product = it }, label = { Text("Product / Service") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(qty, { qty = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(subtotal, { subtotal = it; err = null }, label = { Text("Subtotal *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1.1f))
                    OutlinedTextField(discount, { discount = it }, label = { Text("Disc %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.8f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(monthLabel, { monthLabel = it }, label = { Text("Month (YYYY-MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                EnumDropdownRow("Status", OrderStatus.entries, status, { it.label }) { status = it }
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val sub = subtotal.toDoubleOrNull()
                if (orderNumber.isBlank() || customer.isBlank() || sub == null) {
                    err = "Order number, customer, and subtotal are required."
                } else {
                    val disc = (discount.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                    onSave(
                        (initial ?: SalesOrder()).copy(
                            orderNumber = orderNumber.trim(),
                            customerName = customer.trim(),
                            productName = product.trim().ifBlank { "Enterprise Suite" },
                            quantity = (qty.toLongOrNull() ?: 1L).coerceAtLeast(1L),
                            subtotal = sub,
                            discountPercent = disc,
                            totalAmount = sub * (1.0 - disc / 100.0),
                            monthLabel = monthLabel.ifBlank { "2026-11" },
                            status = status.name
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CampaignFormDialog(
    initial: MarketingCampaign?,
    onDismiss: () -> Unit,
    onSave: (MarketingCampaign) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var channel by remember { mutableStateOf(initial?.channelEnum() ?: MarketingChannel.EMAIL) }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: CampaignStatus.ACTIVE) }
    var budget by remember { mutableStateOf((initial?.budget ?: 2000.0).toString()) }
    var impressions by remember { mutableStateOf((initial?.impressions ?: 10000L).toString()) }
    var clicks by remember { mutableStateOf((initial?.clicks ?: 800L).toString()) }
    var conversions by remember { mutableStateOf((initial?.conversions ?: 50L).toString()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Launch Campaign" else "Edit Campaign") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it; err = null }, label = { Text("Campaign Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("Channel", MarketingChannel.entries, channel, { it.label }) { channel = it }
                EnumDropdownRow("Status", CampaignStatus.entries, status, { it.label }) { status = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(budget, { budget = it }, label = { Text("Budget") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(impressions, { impressions = it }, label = { Text("Impressions") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(clicks, { clicks = it }, label = { Text("Clicks") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(conversions, { conversions = it }, label = { Text("Conversions") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Campaign Notes") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) err = "Campaign name is required."
                else {
                    onSave(
                        (initial ?: MarketingCampaign()).copy(
                            name = name.trim(),
                            channel = channel.name,
                            status = status.name,
                            budget = budget.toDoubleOrNull() ?: 0.0,
                            impressions = (impressions.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                            clicks = (clicks.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                            conversions = (conversions.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                            notes = notes.trim()
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TicketFormDialog(
    initial: SupportTicket?,
    onDismiss: () -> Unit,
    onSave: (SupportTicket) -> Unit
) {
    var ticketNum by remember { mutableStateOf(initial?.ticketNumber ?: "TKT-${(100..999).random()}") }
    var customer by remember { mutableStateOf(initial?.customerName ?: "") }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableStateOf(initial?.priorityEnum() ?: TicketPriority.MEDIUM) }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: TicketStatus.OPEN) }
    var agent by remember { mutableStateOf(initial?.assignedAgent ?: "") }
    var resolution by remember { mutableStateOf(initial?.resolutionNotes ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Support Ticket" else "Edit Ticket") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(ticketNum, { ticketNum = it; err = null }, label = { Text("Ticket Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(customer, { customer = it }, label = { Text("Customer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(subject, { subject = it; err = null }, label = { Text("Subject *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Issue Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("Priority", TicketPriority.entries, priority, { it.label }) { priority = it }
                EnumDropdownRow("Status", TicketStatus.entries, status, { it.label }) { status = it }
                OutlinedTextField(agent, { agent = it }, label = { Text("Assigned Agent") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(resolution, { resolution = it }, label = { Text("Resolution Notes") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (ticketNum.isBlank() || subject.isBlank()) err = "Ticket number and subject are required."
                else {
                    onSave(
                        (initial ?: SupportTicket()).copy(
                            ticketNumber = ticketNum.trim(),
                            customerName = customer.trim().ifBlank { "Customer" },
                            subject = subject.trim(),
                            description = description.trim(),
                            priority = priority.name,
                            status = status.name,
                            assignedAgent = agent.trim(),
                            resolutionNotes = resolution.trim()
                        )
                    )
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
