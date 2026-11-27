package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HrScreen(
    employees: List<Employee>,
    currency: String,
    searchQuery: String,
    canEdit: Boolean,
    canDelete: Boolean,
    canExport: Boolean,
    onUpsertEmployee: (Employee) -> Unit,
    onDeleteEmployee: (Employee) -> Unit,
    onExportCsv: (String, List<String>, List<List<String>>) -> Unit
) {
    var deptFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Employee?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val departments = remember(employees) { employees.map { it.department }.distinct().sorted() }
    val filtered = remember(employees, searchQuery, deptFilter) {
        employees.filter { e ->
            (deptFilter == null || e.department == deptFilter) &&
                (searchQuery.isBlank() ||
                    e.fullName.contains(searchQuery, true) ||
                    e.department.contains(searchQuery, true) ||
                    e.jobTitle.contains(searchQuery, true))
        }
    }

    val totalPayroll = remember(employees) { employees.sumOf { it.salary } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModuleHeaderBar(
            title = "HR & Workforce",
            subtitle = "${employees.size} staff • Annual Payroll ${formatMoney(totalPayroll, currency)}",
            canCreate = canEdit,
            canExport = canExport,
            addLabel = "Add Staff",
            addTag = "hr_add_button",
            onAddClick = { editing = null; showDialog = true },
            onExportCsv = {
                onExportCsv(
                    "hr_employees",
                    listOf("Name", "Department", "Title", "Role", "Salary", "Attendance", "LeaveStatus", "Performance", "Payroll"),
                    filtered.map {
                        listOf(it.fullName, it.department, it.jobTitle, it.role, it.salary.toString(), it.attendanceStatus, it.leaveRequestStatus, it.performanceScore.toString(), it.payrollStatus)
                    }
                )
            }
        )

        if (departments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = deptFilter == null, onClick = { deptFilter = null }, label = { Text("All Depts") })
                departments.forEach { dept ->
                    FilterChip(
                        selected = deptFilter == dept,
                        onClick = { deptFilter = if (deptFilter == dept) null else dept },
                        label = { Text(dept) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyModuleCard(
                title = "No Employee Records",
                description = "Manage employee profiles, departments, attendance, leave approvals, payroll, and performance.",
                icon = Icons.Filled.Badge,
                actionLabel = if (canEdit) "Onboard Employee" else null,
                onAction = if (canEdit) { { editing = null; showDialog = true } } else null
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { emp ->
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
                                    Text(emp.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${emp.jobTitle} • ${emp.department} (${emp.roleEnum().label})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    emp.attendanceEnum().label,
                                    when (emp.attendanceEnum()) {
                                        AttendanceStatus.PRESENT -> NexoraEmerald
                                        AttendanceStatus.REMOTE -> NexoraCyan
                                        AttendanceStatus.ON_LEAVE -> StatusWarning
                                        AttendanceStatus.ABSENT -> StatusError
                                    }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Salary: ${formatMoney(emp.salary, currency)}", style = MaterialTheme.typography.bodySmall)
                                Text("Perf: ${emp.performanceScore}/100", style = MaterialTheme.typography.labelMedium, color = NexoraCyan)
                                StatusPill(
                                    "Payroll: ${emp.payrollEnum().label}",
                                    if (emp.payrollEnum() == PayrollStatus.PAID) NexoraEmerald else StatusWarning
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (emp.performanceScore / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                            if (emp.leaveStatusEnum() == LeaveRequestStatus.PENDING) {
                                Spacer(Modifier.height(8.dp))
                                StatusPill("Pending Leave Request (${emp.leaveBalanceDays}d bal)", StatusWarning)
                            }
                            if (canEdit) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (emp.payrollEnum() == PayrollStatus.PENDING) {
                                        FilledTonalButton(onClick = { onUpsertEmployee(emp.copy(payrollStatus = PayrollStatus.PAID.name)) }) {
                                            Text("Run Payroll")
                                        }
                                    }
                                    if (emp.leaveStatusEnum() == LeaveRequestStatus.PENDING) {
                                        FilledTonalButton(onClick = {
                                            onUpsertEmployee(
                                                emp.copy(
                                                    leaveRequestStatus = LeaveRequestStatus.APPROVED.name,
                                                    attendanceStatus = AttendanceStatus.ON_LEAVE.name
                                                )
                                            )
                                        }) { Text("Approve Leave") }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { editing = emp; showDialog = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit employee")
                                    }
                                    if (canDelete) {
                                        IconButton(onClick = { onDeleteEmployee(emp) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete employee", tint = StatusError)
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
        EmployeeFormDialog(editing, { showDialog = false }, onUpsertEmployee)
    }
}
