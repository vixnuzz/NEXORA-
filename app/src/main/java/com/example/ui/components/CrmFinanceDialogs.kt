package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.*

@Composable
fun LeadFormDialog(
    initial: CrmLead?,
    onDismiss: () -> Unit,
    onSave: (CrmLead) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var company by remember { mutableStateOf(initial?.company ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var source by remember { mutableStateOf(initial?.source ?: "Inbound") }
    var stage by remember { mutableStateOf(initial?.stageEnum() ?: LeadStage.NEW) }
    var dealValue by remember { mutableStateOf(if (initial != null) initial.dealValue.toString() else "10000") }
    var followUp by remember { mutableStateOf(initial?.nextFollowUp ?: "2026-12-05") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New CRM Lead" else "Edit Lead") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it; err = null }, label = { Text("Lead Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(company, { company = it; err = null }, label = { Text("Company *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        dealValue, { dealValue = it },
                        label = { Text("Deal Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(followUp, { followUp = it }, label = { Text("Follow-up") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                EnumDropdownRow("Pipeline Stage", LeadStage.entries, stage, { it.label }) { stage = it }
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(source, { source = it }, label = { Text("Lead Source") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank() || company.isBlank()) {
                    err = "Name and company are required."
                } else {
                    onSave(
                        (initial ?: CrmLead()).copy(
                            name = name.trim(),
                            company = company.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            source = source.trim(),
                            stage = stage.name,
                            dealValue = dealValue.toDoubleOrNull() ?: 0.0,
                            nextFollowUp = followUp.trim(),
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
fun ContactFormDialog(
    initial: CrmContact?,
    onDismiss: () -> Unit,
    onSave: (CrmContact) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var company by remember { mutableStateOf(initial?.company ?: "") }
    var roleTitle by remember { mutableStateOf(initial?.roleTitle ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: ContactStatus.ACTIVE) }
    var history by remember { mutableStateOf(initial?.activityHistory ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Contact" else "Edit Contact") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it; err = null }, label = { Text("Contact Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(company, { company = it; err = null }, label = { Text("Company *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(roleTitle, { roleTitle = it }, label = { Text("Job Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("Status", ContactStatus.entries, status, { it.label }) { status = it }
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(history, { history = it }, label = { Text("Activity History") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank() || company.isBlank()) err = "Name and company are required."
                else {
                    onSave(
                        (initial ?: CrmContact()).copy(
                            name = name.trim(),
                            company = company.trim(),
                            roleTitle = roleTitle.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            status = status.name,
                            activityHistory = history.trim()
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
fun FinanceRecordFormDialog(
    initial: FinanceRecord?,
    onDismiss: () -> Unit,
    onSave: (FinanceRecord) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.typeEnum() ?: FinanceType.INCOME) }
    var category by remember { mutableStateOf(initial?.category ?: "Operations") }
    var amount by remember { mutableStateOf(if (initial != null) initial.amount.toString() else "") }
    var dateStr by remember { mutableStateOf(initial?.dateStr ?: "2026-11-26") }
    var method by remember { mutableStateOf(initial?.paymentMethod ?: "Bank Transfer") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Record Transaction" else "Edit Transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(title, { title = it; err = null }, label = { Text("Description *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                EnumDropdownRow("Entry Type", FinanceType.entries, type, { it.label }) { type = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        amount, { amount = it; err = null },
                        label = { Text("Amount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(dateStr, { dateStr = it }, label = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(category, { category = it }, label = { Text("Category *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(method, { method = it }, label = { Text("Payment Method") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (title.isBlank() || category.isBlank() || parsed == null || parsed < 0) {
                    err = "Enter a valid description, category, and non-negative amount."
                } else {
                    onSave(
                        (initial ?: FinanceRecord()).copy(
                            title = title.trim(),
                            type = type.name,
                            category = category.trim(),
                            amount = parsed,
                            dateStr = dateStr.ifBlank { "2026-11-26" },
                            paymentMethod = method.trim(),
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
fun InvoiceFormDialog(
    initial: Invoice?,
    defaultTaxRate: Double,
    onDismiss: () -> Unit,
    onSave: (Invoice) -> Unit
) {
    var number by remember { mutableStateOf(initial?.invoiceNumber ?: "INV-${(1000..9999).random()}") }
    var customer by remember { mutableStateOf(initial?.customerName ?: "") }
    var docType by remember { mutableStateOf(initial?.docTypeEnum() ?: InvoiceDocType.INVOICE) }
    var amount by remember { mutableStateOf(if (initial != null) initial.amount.toString() else "") }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: InvoiceStatus.PENDING) }
    var dueDate by remember { mutableStateOf(initial?.dueDate ?: "2026-12-15") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Invoice / Estimate" else "Edit ${docType.label}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EnumDropdownRow("Document Type", InvoiceDocType.entries, docType, { it.label }) { docType = it }
                OutlinedTextField(number, { number = it; err = null }, label = { Text("Document Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(customer, { customer = it; err = null }, label = { Text("Customer Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        amount, { amount = it; err = null },
                        label = { Text("Amount *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due Date") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                EnumDropdownRow("Status", InvoiceStatus.entries, status, { it.label }) { status = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Line Items / Notes") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (number.isBlank() || customer.isBlank() || parsed == null || parsed < 0) {
                    err = "Enter document number, customer, and valid amount."
                } else {
                    onSave(
                        (initial ?: Invoice()).copy(
                            invoiceNumber = number.trim(),
                            customerName = customer.trim(),
                            docType = docType.name,
                            amount = parsed,
                            taxAmount = parsed * (defaultTaxRate / 100.0),
                            status = status.name,
                            dueDate = dueDate.ifBlank { "2026-12-15" },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdownRow(
    label: String,
    options: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = labelProvider(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelProvider(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
