package com.example.data

import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.*
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private suspend fun NexoraRepository.runWrite(
    path: String,
    op: OperationType,
    block: suspend () -> Unit
): Result<Unit> {
    return try {
        requireUserId()
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }
}

// --- CRM ---
suspend fun NexoraRepository.upsertLead(
    businessId: String,
    lead: CrmLead,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "crm_leads")
    val isCreate = lead.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(lead.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(lead.name.isNotBlank() && lead.company.isNotBlank()) { "Name and company are required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "name" to lead.name.trim().take(120),
                    "company" to lead.company.trim().take(120),
                    "email" to lead.email.trim().take(150),
                    "phone" to lead.phone.trim().take(40),
                    "source" to lead.source.trim().take(60),
                    "stage" to lead.stage,
                    "dealValue" to lead.dealValue.coerceAtLeast(0.0),
                    "nextFollowUp" to lead.nextFollowUp.trim().take(60),
                    "notes" to lead.notes.trim().take(1000),
                    "assignedTo" to lead.assignedTo.trim().take(120)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "name" to lead.name.trim().take(120),
                    "company" to lead.company.trim().take(120),
                    "email" to lead.email.trim().take(150),
                    "phone" to lead.phone.trim().take(40),
                    "source" to lead.source.trim().take(60),
                    "stage" to lead.stage,
                    "dealValue" to lead.dealValue.coerceAtLeast(0.0),
                    "nextFollowUp" to lead.nextFollowUp.trim().take(60),
                    "notes" to lead.notes.trim().take(1000),
                    "assignedTo" to lead.assignedTo.trim().take(120)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "CRM", if (isCreate) "CREATE" else "UPDATE",
            "${if (isCreate) "Added" else "Updated"} lead ${lead.name} (${lead.company})")
    }
}

suspend fun NexoraRepository.deleteDoc(
    businessId: String,
    collection: String,
    docId: String,
    label: String,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val ref = moduleCol(uid, businessId, collection).document(docId)
    return runWrite(ref.path, OperationType.DELETE) {
        require(docId.isNotBlank())
        ref.delete().await()
        logActivity(businessId, actorName, actorRole, collection.uppercase(), "DELETE", "Deleted $label")
    }
}

suspend fun NexoraRepository.upsertContact(
    businessId: String,
    contact: CrmContact,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "crm_contacts")
    val isCreate = contact.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(contact.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(contact.name.isNotBlank() && contact.company.isNotBlank()) { "Name and company are required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "name" to contact.name.trim().take(120),
                    "company" to contact.company.trim().take(120),
                    "roleTitle" to contact.roleTitle.trim().take(100),
                    "email" to contact.email.trim().take(150),
                    "phone" to contact.phone.trim().take(40),
                    "status" to contact.status,
                    "activityHistory" to contact.activityHistory.trim().take(1000)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "name" to contact.name.trim().take(120),
                    "company" to contact.company.trim().take(120),
                    "roleTitle" to contact.roleTitle.trim().take(100),
                    "email" to contact.email.trim().take(150),
                    "phone" to contact.phone.trim().take(40),
                    "status" to contact.status,
                    "activityHistory" to contact.activityHistory.trim().take(1000)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "CRM", if (isCreate) "CREATE" else "UPDATE",
            "${if (isCreate) "Added" else "Updated"} contact ${contact.name} (${contact.company})")
    }
}

// --- FINANCE ---
suspend fun NexoraRepository.upsertFinanceRecord(
    businessId: String,
    record: FinanceRecord,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "finance_records")
    val isCreate = record.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(record.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(record.title.isNotBlank() && record.category.isNotBlank()) { "Title and category required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "title" to record.title.trim().take(150),
                    "type" to record.type,
                    "category" to record.category.trim().take(80),
                    "amount" to record.amount.coerceAtLeast(0.0),
                    "dateStr" to record.dateStr.ifBlank { "2026-11-26" }.take(40),
                    "paymentMethod" to record.paymentMethod.trim().take(60),
                    "notes" to record.notes.trim().take(1000)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "title" to record.title.trim().take(150),
                    "type" to record.type,
                    "category" to record.category.trim().take(80),
                    "amount" to record.amount.coerceAtLeast(0.0),
                    "dateStr" to record.dateStr.ifBlank { "2026-11-26" }.take(40),
                    "paymentMethod" to record.paymentMethod.trim().take(60),
                    "notes" to record.notes.trim().take(1000)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "FINANCE", if (isCreate) "CREATE" else "UPDATE",
            "${record.typeEnum().label}: ${record.title} (${record.amount})")
    }
}

suspend fun NexoraRepository.upsertInvoice(
    businessId: String,
    invoice: Invoice,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "invoices")
    val isCreate = invoice.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(invoice.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(invoice.invoiceNumber.isNotBlank() && invoice.customerName.isNotBlank()) { "Number and customer required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "invoiceNumber" to invoice.invoiceNumber.trim().take(60),
                    "customerName" to invoice.customerName.trim().take(120),
                    "docType" to invoice.docType,
                    "amount" to invoice.amount.coerceAtLeast(0.0),
                    "taxAmount" to invoice.taxAmount.coerceAtLeast(0.0),
                    "status" to invoice.status,
                    "dueDate" to invoice.dueDate.ifBlank { "2026-12-15" }.take(40),
                    "notes" to invoice.notes.trim().take(1000)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "invoiceNumber" to invoice.invoiceNumber.trim().take(60),
                    "customerName" to invoice.customerName.trim().take(120),
                    "docType" to invoice.docType,
                    "amount" to invoice.amount.coerceAtLeast(0.0),
                    "taxAmount" to invoice.taxAmount.coerceAtLeast(0.0),
                    "status" to invoice.status,
                    "dueDate" to invoice.dueDate.ifBlank { "2026-12-15" }.take(40),
                    "notes" to invoice.notes.trim().take(1000)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "FINANCE", if (isCreate) "CREATE" else "UPDATE",
            "${invoice.docTypeEnum().label} ${invoice.invoiceNumber} (${invoice.statusEnum().label})")
    }
}

// --- HR ---
suspend fun NexoraRepository.upsertEmployee(
    businessId: String,
    employee: Employee,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "employees")
    val isCreate = employee.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(employee.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(employee.fullName.isNotBlank() && employee.department.isNotBlank() && employee.jobTitle.isNotBlank()) {
            "Name, department, and job title are required"
        }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "fullName" to employee.fullName.trim().take(120),
                    "email" to employee.email.trim().take(150),
                    "department" to employee.department.trim().take(80),
                    "jobTitle" to employee.jobTitle.trim().take(100),
                    "role" to employee.role,
                    "salary" to employee.salary.coerceAtLeast(0.0),
                    "attendanceStatus" to employee.attendanceStatus,
                    "leaveBalanceDays" to employee.leaveBalanceDays.coerceIn(0L, 365L),
                    "leaveRequestStatus" to employee.leaveRequestStatus,
                    "performanceScore" to employee.performanceScore.coerceIn(0L, 100L),
                    "payrollStatus" to employee.payrollStatus
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "fullName" to employee.fullName.trim().take(120),
                    "email" to employee.email.trim().take(150),
                    "department" to employee.department.trim().take(80),
                    "jobTitle" to employee.jobTitle.trim().take(100),
                    "role" to employee.role,
                    "salary" to employee.salary.coerceAtLeast(0.0),
                    "attendanceStatus" to employee.attendanceStatus,
                    "leaveBalanceDays" to employee.leaveBalanceDays.coerceIn(0L, 365L),
                    "leaveRequestStatus" to employee.leaveRequestStatus,
                    "performanceScore" to employee.performanceScore.coerceIn(0L, 100L),
                    "payrollStatus" to employee.payrollStatus
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "HR", if (isCreate) "CREATE" else "UPDATE",
            "${if (isCreate) "Onboarded" else "Updated"} employee ${employee.fullName} (${employee.department})")
    }
}

// --- PROJECTS & TASKS ---
suspend fun NexoraRepository.upsertProject(
    businessId: String,
    project: Project,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "projects")
    val isCreate = project.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(project.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(project.name.isNotBlank()) { "Project name is required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "name" to project.name.trim().take(150),
                    "description" to project.description.trim().take(1000),
                    "status" to project.status,
                    "deadline" to project.deadline.ifBlank { "2026-12-30" }.take(40),
                    "memberNames" to project.memberNames.trim().take(500),
                    "progressPercent" to project.progressPercent.coerceIn(0L, 100L)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "name" to project.name.trim().take(150),
                    "description" to project.description.trim().take(1000),
                    "status" to project.status,
                    "deadline" to project.deadline.ifBlank { "2026-12-30" }.take(40),
                    "memberNames" to project.memberNames.trim().take(500),
                    "progressPercent" to project.progressPercent.coerceIn(0L, 100L)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "PROJECTS", if (isCreate) "CREATE" else "UPDATE",
            "${if (isCreate) "Created" else "Updated"} project ${project.name}")
    }
}

suspend fun NexoraRepository.upsertProjectTask(
    businessId: String,
    task: ProjectTask,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "project_tasks")
    val isCreate = task.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(task.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(task.title.isNotBlank()) { "Task title is required" }
        val projId = task.projectId.ifBlank { "general" }
        val projName = task.projectName.ifBlank { "General Workspace" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "projectId" to projId.take(128),
                    "projectName" to projName.take(150),
                    "title" to task.title.trim().take(150),
                    "subtasksSummary" to task.subtasksSummary.trim().take(500),
                    "assignee" to task.assignee.trim().take(120),
                    "priority" to task.priority,
                    "kanbanColumn" to task.kanbanColumn,
                    "dueDate" to task.dueDate.ifBlank { "2026-12-10" }.take(40),
                    "reminderActive" to task.reminderActive
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "projectId" to projId.take(128),
                    "projectName" to projName.take(150),
                    "title" to task.title.trim().take(150),
                    "subtasksSummary" to task.subtasksSummary.trim().take(500),
                    "assignee" to task.assignee.trim().take(120),
                    "priority" to task.priority,
                    "kanbanColumn" to task.kanbanColumn,
                    "dueDate" to task.dueDate.ifBlank { "2026-12-10" }.take(40),
                    "reminderActive" to task.reminderActive
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "PROJECTS", if (isCreate) "CREATE" else "UPDATE",
            "Task '${task.title}' moved to ${task.columnEnum().label}")
    }
}

// --- INVENTORY & PURCHASE ORDERS ---
suspend fun NexoraRepository.upsertProduct(
    businessId: String,
    product: InventoryProduct,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "inventory_products")
    val isCreate = product.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(product.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(product.sku.isNotBlank() && product.name.isNotBlank()) { "SKU and product name required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "sku" to product.sku.trim().take(60),
                    "name" to product.name.trim().take(150),
                    "category" to product.category.ifBlank { "General" }.trim().take(80),
                    "supplierName" to product.supplierName.ifBlank { "Primary Supplier" }.trim().take(120),
                    "unitPrice" to product.unitPrice.coerceAtLeast(0.0),
                    "stockQty" to product.stockQty.coerceAtLeast(0L),
                    "reorderLevel" to product.reorderLevel.coerceAtLeast(0L),
                    "lastMovementNote" to product.lastMovementNote.ifBlank { "Initial stock entry" }.take(300)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "sku" to product.sku.trim().take(60),
                    "name" to product.name.trim().take(150),
                    "category" to product.category.ifBlank { "General" }.trim().take(80),
                    "supplierName" to product.supplierName.ifBlank { "Primary Supplier" }.trim().take(120),
                    "unitPrice" to product.unitPrice.coerceAtLeast(0.0),
                    "stockQty" to product.stockQty.coerceAtLeast(0L),
                    "reorderLevel" to product.reorderLevel.coerceAtLeast(0L),
                    "lastMovementNote" to product.lastMovementNote.take(300)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "INVENTORY", if (isCreate) "CREATE" else "UPDATE",
            "Inventory ${product.name} (${product.stockQty} units)")
    }
}

suspend fun NexoraRepository.upsertPurchaseOrder(
    businessId: String,
    po: PurchaseOrder,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "purchase_orders")
    val isCreate = po.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(po.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(po.poNumber.isNotBlank() && po.productName.isNotBlank()) { "PO number and product required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "poNumber" to po.poNumber.trim().take(60),
                    "supplierName" to po.supplierName.ifBlank { "Supplier" }.trim().take(120),
                    "productId" to po.productId.take(128),
                    "productName" to po.productName.trim().take(150),
                    "quantity" to po.quantity.coerceAtLeast(1L),
                    "totalCost" to po.totalCost.coerceAtLeast(0.0),
                    "status" to po.status
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "poNumber" to po.poNumber.trim().take(60),
                    "supplierName" to po.supplierName.ifBlank { "Supplier" }.trim().take(120),
                    "productId" to po.productId.take(128),
                    "productName" to po.productName.trim().take(150),
                    "quantity" to po.quantity.coerceAtLeast(1L),
                    "totalCost" to po.totalCost.coerceAtLeast(0.0),
                    "status" to po.status
                )
            ).await()
        }
        // If PO marked RECEIVED and linked to a product, increment stock cleanly
        if (po.status == PoStatus.RECEIVED.name && po.productId.isNotBlank()) {
            val prodRef = moduleCol(uid, businessId, "inventory_products").document(po.productId)
            val snap = prodRef.get().await()
            if (snap.exists()) {
                prodRef.update(
                    mapOf(
                        "stockQty" to FieldValue.increment(po.quantity.coerceAtLeast(1L)),
                        "lastMovementNote" to "Received +${po.quantity} via ${po.poNumber}",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        logActivity(businessId, actorName, actorRole, "INVENTORY", if (isCreate) "CREATE" else "UPDATE",
            "PO ${po.poNumber} for ${po.productName} (${po.statusEnum().label})")
    }
}

// --- SALES ORDERS ---
suspend fun NexoraRepository.upsertSalesOrder(
    businessId: String,
    order: SalesOrder,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "sales_orders")
    val isCreate = order.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(order.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(order.orderNumber.isNotBlank() && order.customerName.isNotBlank()) { "Order # and customer required" }
        val disc = order.discountPercent.coerceIn(0.0, 100.0)
        val total = (order.subtotal * (1.0 - disc / 100.0)).coerceAtLeast(0.0)
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "orderNumber" to order.orderNumber.trim().take(60),
                    "customerName" to order.customerName.trim().take(120),
                    "productName" to order.productName.trim().ifBlank { "Enterprise Suite" }.take(150),
                    "quantity" to order.quantity.coerceAtLeast(1L),
                    "subtotal" to order.subtotal.coerceAtLeast(0.0),
                    "discountPercent" to disc,
                    "totalAmount" to total,
                    "monthLabel" to order.monthLabel.ifBlank { "2026-11" }.take(20),
                    "status" to order.status
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "orderNumber" to order.orderNumber.trim().take(60),
                    "customerName" to order.customerName.trim().take(120),
                    "productName" to order.productName.trim().ifBlank { "Enterprise Suite" }.take(150),
                    "quantity" to order.quantity.coerceAtLeast(1L),
                    "subtotal" to order.subtotal.coerceAtLeast(0.0),
                    "discountPercent" to disc,
                    "totalAmount" to total,
                    "monthLabel" to order.monthLabel.ifBlank { "2026-11" }.take(20),
                    "status" to order.status
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "SALES", if (isCreate) "CREATE" else "UPDATE",
            "Order ${order.orderNumber} (${order.customerName}) - ${order.statusEnum().label}")
    }
}

// --- MARKETING ---
suspend fun NexoraRepository.upsertCampaign(
    businessId: String,
    campaign: MarketingCampaign,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "marketing_campaigns")
    val isCreate = campaign.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(campaign.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(campaign.name.isNotBlank()) { "Campaign name required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "name" to campaign.name.trim().take(150),
                    "channel" to campaign.channel,
                    "status" to campaign.status,
                    "budget" to campaign.budget.coerceAtLeast(0.0),
                    "impressions" to campaign.impressions.coerceAtLeast(0L),
                    "clicks" to campaign.clicks.coerceAtLeast(0L),
                    "conversions" to campaign.conversions.coerceAtLeast(0L),
                    "notes" to campaign.notes.trim().take(1000)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "name" to campaign.name.trim().take(150),
                    "channel" to campaign.channel,
                    "status" to campaign.status,
                    "budget" to campaign.budget.coerceAtLeast(0.0),
                    "impressions" to campaign.impressions.coerceAtLeast(0L),
                    "clicks" to campaign.clicks.coerceAtLeast(0L),
                    "conversions" to campaign.conversions.coerceAtLeast(0L),
                    "notes" to campaign.notes.trim().take(1000)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "MARKETING", if (isCreate) "CREATE" else "UPDATE",
            "Campaign '${campaign.name}' (${campaign.channelEnum().label})")
    }
}

// --- SUPPORT DESK ---
suspend fun NexoraRepository.upsertSupportTicket(
    businessId: String,
    ticket: SupportTicket,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val col = moduleCol(uid, businessId, "support_tickets")
    val isCreate = ticket.id.isBlank()
    val ref = if (isCreate) col.document() else col.document(ticket.id)
    return runWrite(ref.path, if (isCreate) OperationType.CREATE else OperationType.UPDATE) {
        require(ticket.ticketNumber.isNotBlank() && ticket.subject.isNotBlank()) { "Ticket # and subject required" }
        if (isCreate) {
            ref.set(
                timestampedCreateMap(
                    "userId" to uid,
                    "businessId" to businessId,
                    "ticketNumber" to ticket.ticketNumber.trim().take(60),
                    "customerName" to ticket.customerName.ifBlank { "Customer" }.trim().take(120),
                    "subject" to ticket.subject.trim().take(200),
                    "description" to ticket.description.trim().take(1500),
                    "priority" to ticket.priority,
                    "status" to ticket.status,
                    "assignedAgent" to ticket.assignedAgent.trim().take(120),
                    "resolutionNotes" to ticket.resolutionNotes.trim().take(1000)
                )
            ).await()
        } else {
            ref.update(
                timestampedUpdateMap(
                    "ticketNumber" to ticket.ticketNumber.trim().take(60),
                    "customerName" to ticket.customerName.ifBlank { "Customer" }.trim().take(120),
                    "subject" to ticket.subject.trim().take(200),
                    "description" to ticket.description.trim().take(1500),
                    "priority" to ticket.priority,
                    "status" to ticket.status,
                    "assignedAgent" to ticket.assignedAgent.trim().take(120),
                    "resolutionNotes" to ticket.resolutionNotes.trim().take(1000)
                )
            ).await()
        }
        logActivity(businessId, actorName, actorRole, "SUPPORT", if (isCreate) "CREATE" else "UPDATE",
            "Ticket ${ticket.ticketNumber}: ${ticket.statusEnum().label}")
    }
}

// --- LABELED DEMO DATA SEEDER ---
suspend fun NexoraRepository.seedEnterpriseDemoData(
    businessId: String,
    actorName: String,
    actorRole: String
): Result<Unit> {
    val uid = requireUserId()
    val bizRef = businessDoc(uid, businessId)
    return runWrite(bizRef.path, OperationType.WRITE) {
        val batch = db.batch()
        fun addDoc(col: String, data: Map<String, Any>) {
            batch.set(moduleCol(uid, businessId, col).document(), data)
        }

        // CRM Contacts
        addDoc("crm_contacts", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "Marcus Chen [Demo]", "company" to "Vertex Logistics",
            "roleTitle" to "VP Operations", "email" to "m.chen@vertex.example",
            "phone" to "+1 415-555-0142", "status" to ContactStatus.VIP.name,
            "activityHistory" to "Signed annual enterprise SLA; quarterly review scheduled"
        ))
        addDoc("crm_contacts", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "Clara Lindqvist [Demo]", "company" to "Nordic BioTech",
            "roleTitle" to "Head of Procurement", "email" to "clara@nordicbio.example",
            "phone" to "+1 415-555-0198", "status" to ContactStatus.ACTIVE.name,
            "activityHistory" to "Requested Q1 volume discount schedule"
        ))

        // CRM Leads
        addDoc("crm_leads", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "Elena Vance [Demo]", "company" to "Orbital Cloud",
            "email" to "elena@orbital.example", "phone" to "+1 650-555-0122",
            "source" to "Executive Referral", "stage" to LeadStage.PROPOSAL.name,
            "dealValue" to 68000.0, "nextFollowUp" to "2026-11-30",
            "notes" to "Security questionnaire approved; awaiting CFO sign-off",
            "assignedTo" to actorName
        ))
        addDoc("crm_leads", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "Devon Brooks [Demo]", "company" to "Apex Manufacturing",
            "email" to "devon@apexmfg.example", "phone" to "+1 312-555-0166",
            "source" to "Inbound Demo", "stage" to LeadStage.QUALIFIED.name,
            "dealValue" to 42500.0, "nextFollowUp" to "2026-12-03",
            "notes" to "Needs inventory + ERP module rollout for 3 warehouses",
            "assignedTo" to actorName
        ))

        // Finance Records
        addDoc("finance_records", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "title" to "Vertex Enterprise Subscription [Demo]", "type" to FinanceType.INCOME.name,
            "category" to "SaaS Subscriptions", "amount" to 28500.0,
            "dateStr" to "2026-11-18", "paymentMethod" to "ACH Wire",
            "notes" to "Annual platform license"
        ))
        addDoc("finance_records", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "title" to "Cloud Infrastructure & AI Compute [Demo]", "type" to FinanceType.EXPENSE.name,
            "category" to "Infrastructure", "amount" to 4850.0,
            "dateStr" to "2026-11-20", "paymentMethod" to "Corporate Card",
            "notes" to "Monthly production cluster"
        ))
        addDoc("finance_records", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "title" to "November Team Payroll [Demo]", "type" to FinanceType.EXPENSE.name,
            "category" to "Payroll", "amount" to 16200.0,
            "dateStr" to "2026-11-25", "paymentMethod" to "Direct Deposit",
            "notes" to "Bi-weekly engineering & ops payroll"
        ))

        // Invoices
        addDoc("invoices", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "invoiceNumber" to "INV-2026-101", "customerName" to "Nordic BioTech [Demo]",
            "docType" to InvoiceDocType.INVOICE.name, "amount" to 14200.0, "taxAmount" to 1136.0,
            "status" to InvoiceStatus.PENDING.name, "dueDate" to "2026-12-10",
            "notes" to "Net 30 enterprise onboarding"
        ))
        addDoc("invoices", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "invoiceNumber" to "EST-2026-044", "customerName" to "Orbital Cloud [Demo]",
            "docType" to InvoiceDocType.ESTIMATE.name, "amount" to 68000.0, "taxAmount" to 5440.0,
            "status" to InvoiceStatus.SENT.name, "dueDate" to "2026-12-18",
            "notes" to "Full 12-month enterprise bundle quotation"
        ))

        // HR Employees
        addDoc("employees", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "fullName" to "Priya Nair [Demo]", "email" to "priya@company.example",
            "department" to "Engineering", "jobTitle" to "Principal Architect",
            "role" to UserRole.MANAGER.name, "salary" to 158000.0,
            "attendanceStatus" to AttendanceStatus.PRESENT.name,
            "leaveBalanceDays" to 14L, "leaveRequestStatus" to LeaveRequestStatus.NONE.name,
            "performanceScore" to 96L, "payrollStatus" to PayrollStatus.PAID.name
        ))
        addDoc("employees", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "fullName" to "Jordan Alvarez [Demo]", "email" to "jordan@company.example",
            "department" to "Revenue", "jobTitle" to "Account Executive",
            "role" to UserRole.SALES_REP.name, "salary" to 105000.0,
            "attendanceStatus" to AttendanceStatus.REMOTE.name,
            "leaveBalanceDays" to 11L, "leaveRequestStatus" to LeaveRequestStatus.PENDING.name,
            "performanceScore" to 89L, "payrollStatus" to PayrollStatus.PENDING.name
        ))

        // Projects & Tasks
        val projRef = moduleCol(uid, businessId, "projects").document()
        batch.set(projRef, timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "Global ERP Migration [Demo]",
            "description" to "Unified finance, CRM, and warehouse telemetry launch",
            "status" to ProjectStatus.ACTIVE.name, "deadline" to "2026-12-22",
            "memberNames" to "Priya Nair, Jordan Alvarez", "progressPercent" to 68L
        ))
        addDoc("project_tasks", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "projectId" to projRef.id, "projectName" to "Global ERP Migration [Demo]",
            "title" to "Audit Q4 Tax Compliance Rules",
            "subtasksSummary" to "Verify VAT rates; Export audit trail",
            "assignee" to "Priya Nair", "priority" to TaskPriority.HIGH.name,
            "kanbanColumn" to KanbanColumn.IN_PROGRESS.name,
            "dueDate" to "2026-12-02", "reminderActive" to true
        ))
        addDoc("project_tasks", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "projectId" to projRef.id, "projectName" to "Global ERP Migration [Demo]",
            "title" to "Prepare Orbital Cloud Onboarding Deck",
            "subtasksSummary" to "Draft SLA; Review security posture",
            "assignee" to "Jordan Alvarez", "priority" to TaskPriority.URGENT.name,
            "kanbanColumn" to KanbanColumn.TODO.name,
            "dueDate" to "2026-11-29", "reminderActive" to true
        ))

        // Inventory Products & Purchase Order
        val prodRef = moduleCol(uid, businessId, "inventory_products").document()
        batch.set(prodRef, timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "sku" to "NX-EDGE-400", "name" to "Smart POS Terminal Pro [Demo]",
            "category" to "Hardware", "supplierName" to "Apex Silicon Supply",
            "unitPrice" to 599.0, "stockQty" to 4L, "reorderLevel" to 12L,
            "lastMovementNote" to "Dispatched 8 units to Vertex Logistics"
        ))
        addDoc("inventory_products", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "sku" to "NX-LIC-ENT", "name" to "Enterprise Gateway License [Demo]",
            "category" to "Software", "supplierName" to "Internal Cloud",
            "unitPrice" to 1200.0, "stockQty" to 45L, "reorderLevel" to 10L,
            "lastMovementNote" to "Replenished license pool"
        ))
        addDoc("purchase_orders", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "poNumber" to "PO-2026-088", "supplierName" to "Apex Silicon Supply",
            "productId" to prodRef.id, "productName" to "Smart POS Terminal Pro [Demo]",
            "quantity" to 25L, "totalCost" to 9500.0, "status" to PoStatus.ORDERED.name
        ))

        // Sales Orders (multi-month for live chart)
        addDoc("sales_orders", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "orderNumber" to "SO-2026-301", "customerName" to "Vertex Logistics [Demo]",
            "productName" to "Enterprise Gateway License [Demo]", "quantity" to 10L,
            "subtotal" to 12000.0, "discountPercent" to 5.0, "totalAmount" to 11400.0,
            "monthLabel" to "2026-09", "status" to OrderStatus.DELIVERED.name
        ))
        addDoc("sales_orders", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "orderNumber" to "SO-2026-308", "customerName" to "Nordic BioTech [Demo]",
            "productName" to "Smart POS Terminal Pro [Demo]", "quantity" to 30L,
            "subtotal" to 17970.0, "discountPercent" to 10.0, "totalAmount" to 16173.0,
            "monthLabel" to "2026-10", "status" to OrderStatus.DELIVERED.name
        ))
        addDoc("sales_orders", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "orderNumber" to "SO-2026-315", "customerName" to "Vertex Logistics [Demo]",
            "productName" to "Enterprise Gateway License [Demo]", "quantity" to 20L,
            "subtotal" to 24000.0, "discountPercent" to 8.0, "totalAmount" to 22080.0,
            "monthLabel" to "2026-11", "status" to OrderStatus.SHIPPED.name
        ))

        // Marketing Campaigns
        addDoc("marketing_campaigns", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "Q4 Enterprise SaaS Summit [Demo]",
            "channel" to MarketingChannel.EMAIL.name, "status" to CampaignStatus.ACTIVE.name,
            "budget" to 3200.0, "impressions" to 24500L, "clicks" to 3180L, "conversions" to 194L,
            "notes" to "High engagement from Operations VPs"
        ))
        addDoc("marketing_campaigns", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "name" to "LinkedIn Executive Automation [Demo]",
            "channel" to MarketingChannel.SOCIAL_MEDIA.name, "status" to CampaignStatus.ACTIVE.name,
            "budget" to 4500.0, "impressions" to 52000L, "clicks" to 2140L, "conversions" to 88L,
            "notes" to "Retargeting CFO & COO personas"
        ))

        // Support Tickets
        addDoc("support_tickets", timestampedCreateMap(
            "userId" to uid, "businessId" to businessId,
            "ticketNumber" to "TKT-904", "customerName" to "Vertex Logistics [Demo]",
            "subject" to "SSO SAML Certificate Rotation [Demo]",
            "description" to "Customer needs updated IdP metadata verification before Dec 1.",
            "priority" to TicketPriority.HIGH.name, "status" to TicketStatus.IN_PROGRESS.name,
            "assignedAgent" to "Priya Nair", "resolutionNotes" to "Staging cert verified"
        ))

        batch.update(bizRef, timestampedUpdateMap("demoSeeded" to true))
        batch.commit().await()

        logActivity(
            businessId, actorName, actorRole, "SYSTEM", "DEMO_SEED",
            "Loaded labeled sample enterprise demo data across all modules"
        )
    }
}
