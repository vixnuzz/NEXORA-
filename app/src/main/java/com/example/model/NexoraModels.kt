package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}

enum class UserRole(val label: String, val badgeShort: String) {
    SUPER_ADMIN("Super Admin", "S-ADMIN"),
    BUSINESS_OWNER("Business Owner", "OWNER"),
    MANAGER("Manager", "MGR"),
    EMPLOYEE("Employee", "EMP"),
    ACCOUNTANT("Accountant", "ACCT"),
    SALES_REP("Sales Representative", "SALES")
}

enum class LeadStage(val label: String) {
    NEW("New"),
    CONTACTED("Contacted"),
    QUALIFIED("Qualified"),
    PROPOSAL("Proposal"),
    WON("Won"),
    LOST("Lost")
}

enum class ContactStatus(val label: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    VIP("VIP")
}

enum class FinanceType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class InvoiceDocType(val label: String) {
    INVOICE("Invoice"),
    ESTIMATE("Estimate")
}

enum class InvoiceStatus(val label: String) {
    DRAFT("Draft"),
    SENT("Sent"),
    PENDING("Pending"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    ACCEPTED("Accepted")
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    REMOTE("Remote"),
    ON_LEAVE("On Leave"),
    ABSENT("Absent")
}

enum class LeaveRequestStatus(val label: String) {
    NONE("None"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class PayrollStatus(val label: String) {
    PAID("Paid"),
    PENDING("Pending")
}

enum class ProjectStatus(val label: String) {
    PLANNING("Planning"),
    ACTIVE("Active"),
    ON_HOLD("On Hold"),
    COMPLETED("Completed")
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class KanbanColumn(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    REVIEW("Review"),
    DONE("Done")
}

enum class PoStatus(val label: String) {
    ORDERED("Ordered"),
    RECEIVED("Received"),
    CANCELLED("Cancelled")
}

enum class OrderStatus(val label: String) {
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

enum class MarketingChannel(val label: String) {
    EMAIL("Email"),
    SOCIAL_MEDIA("Social Media"),
    PAID_ADS("Paid Ads"),
    SEO("SEO")
}

enum class CampaignStatus(val label: String) {
    DRAFT("Draft"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed")
}

enum class TicketPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

enum class TicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    WAITING("Waiting"),
    RESOLVED("Resolved"),
    CLOSED("Closed")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val jobTitle: String = "",
    val phone: String = "",
    val activeBusinessId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Business(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val industry: String = "",
    val currency: String = "USD",
    val taxRate: Double = 0.0,
    val address: String = "",
    val email: String = "",
    val activeRole: String = UserRole.BUSINESS_OWNER.name,
    val demoSeeded: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = enumOrDefault(activeRole, UserRole.BUSINESS_OWNER)
}

@Keep
@IgnoreExtraProperties
data class CrmLead(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val company: String = "",
    val email: String = "",
    val phone: String = "",
    val source: String = "Inbound",
    val stage: String = LeadStage.NEW.name,
    val dealValue: Double = 0.0,
    val nextFollowUp: String = "",
    val notes: String = "",
    val assignedTo: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun stageEnum(): LeadStage = enumOrDefault(stage, LeadStage.NEW)
}

@Keep
@IgnoreExtraProperties
data class CrmContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val company: String = "",
    val roleTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = ContactStatus.ACTIVE.name,
    val activityHistory: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ContactStatus = enumOrDefault(status, ContactStatus.ACTIVE)
}

@Keep
@IgnoreExtraProperties
data class FinanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val title: String = "",
    val type: String = FinanceType.INCOME.name,
    val category: String = "Operations",
    val amount: Double = 0.0,
    val dateStr: String = "",
    val paymentMethod: String = "Bank Transfer",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun typeEnum(): FinanceType = enumOrDefault(type, FinanceType.INCOME)
}

@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val invoiceNumber: String = "",
    val customerName: String = "",
    val docType: String = InvoiceDocType.INVOICE.name,
    val amount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val status: String = InvoiceStatus.PENDING.name,
    val dueDate: String = "",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun docTypeEnum(): InvoiceDocType = enumOrDefault(docType, InvoiceDocType.INVOICE)
    fun statusEnum(): InvoiceStatus = enumOrDefault(status, InvoiceStatus.PENDING)
}

@Keep
@IgnoreExtraProperties
data class Employee(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val fullName: String = "",
    val email: String = "",
    val department: String = "Operations",
    val jobTitle: String = "",
    val role: String = UserRole.EMPLOYEE.name,
    val salary: Double = 0.0,
    val attendanceStatus: String = AttendanceStatus.PRESENT.name,
    val leaveBalanceDays: Long = 15L,
    val leaveRequestStatus: String = LeaveRequestStatus.NONE.name,
    val performanceScore: Long = 85L,
    val payrollStatus: String = PayrollStatus.PENDING.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = enumOrDefault(role, UserRole.EMPLOYEE)
    fun attendanceEnum(): AttendanceStatus = enumOrDefault(attendanceStatus, AttendanceStatus.PRESENT)
    fun leaveStatusEnum(): LeaveRequestStatus = enumOrDefault(leaveRequestStatus, LeaveRequestStatus.NONE)
    fun payrollEnum(): PayrollStatus = enumOrDefault(payrollStatus, PayrollStatus.PENDING)
}

@Keep
@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = ProjectStatus.ACTIVE.name,
    val deadline: String = "",
    val memberNames: String = "",
    val progressPercent: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ProjectStatus = enumOrDefault(status, ProjectStatus.ACTIVE)
}

@Keep
@IgnoreExtraProperties
data class ProjectTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val title: String = "",
    val subtasksSummary: String = "",
    val assignee: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val kanbanColumn: String = KanbanColumn.TODO.name,
    val dueDate: String = "",
    val reminderActive: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun priorityEnum(): TaskPriority = enumOrDefault(priority, TaskPriority.MEDIUM)
    fun columnEnum(): KanbanColumn = enumOrDefault(kanbanColumn, KanbanColumn.TODO)
}

@Keep
@IgnoreExtraProperties
data class InventoryProduct(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val sku: String = "",
    val name: String = "",
    val category: String = "",
    val supplierName: String = "",
    val unitPrice: Double = 0.0,
    val stockQty: Long = 0L,
    val reorderLevel: Long = 10L,
    val lastMovementNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val needsRestock: Boolean get() = stockQty <= reorderLevel
}

@Keep
@IgnoreExtraProperties
data class PurchaseOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val poNumber: String = "",
    val supplierName: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Long = 1L,
    val totalCost: Double = 0.0,
    val status: String = PoStatus.ORDERED.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun statusEnum(): PoStatus = enumOrDefault(status, PoStatus.ORDERED)
}

@Keep
@IgnoreExtraProperties
data class SalesOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val productName: String = "",
    val quantity: Long = 1L,
    val subtotal: Double = 0.0,
    val discountPercent: Double = 0.0,
    val totalAmount: Double = 0.0,
    val monthLabel: String = "",
    val status: String = OrderStatus.PROCESSING.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun statusEnum(): OrderStatus = enumOrDefault(status, OrderStatus.PROCESSING)
}

@Keep
@IgnoreExtraProperties
data class MarketingCampaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val name: String = "",
    val channel: String = MarketingChannel.EMAIL.name,
    val status: String = CampaignStatus.ACTIVE.name,
    val budget: Double = 0.0,
    val impressions: Long = 0L,
    val clicks: Long = 0L,
    val conversions: Long = 0L,
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun channelEnum(): MarketingChannel = enumOrDefault(channel, MarketingChannel.EMAIL)
    fun statusEnum(): CampaignStatus = enumOrDefault(status, CampaignStatus.ACTIVE)
    val ctrPercent: Double get() = if (impressions > 0) (clicks * 100.0 / impressions) else 0.0
    val conversionRatePercent: Double get() = if (clicks > 0) (conversions * 100.0 / clicks) else 0.0
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val ticketNumber: String = "",
    val customerName: String = "",
    val subject: String = "",
    val description: String = "",
    val priority: String = TicketPriority.MEDIUM.name,
    val status: String = TicketStatus.OPEN.name,
    val assignedAgent: String = "",
    val resolutionNotes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun priorityEnum(): TicketPriority = enumOrDefault(priority, TicketPriority.MEDIUM)
    fun statusEnum(): TicketStatus = enumOrDefault(status, TicketStatus.OPEN)
}

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessId: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val module: String = "",
    val action: String = "",
    val summary: String = "",
    val unread: Boolean = true,
    val createdAt: Timestamp? = null
)

fun serverTimestampMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    val result = linkedMapOf<String, Any>()
    for ((k, v) in pairs) {
        if (v != null) result[k] = v
    }
    return result
}

fun timestampedCreateMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    val result = serverTimestampMap(*pairs).toMutableMap()
    result["createdAt"] = FieldValue.serverTimestamp()
    result["updatedAt"] = FieldValue.serverTimestamp()
    return result
}

fun timestampedUpdateMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    val result = serverTimestampMap(*pairs).toMutableMap()
    result["updatedAt"] = FieldValue.serverTimestamp()
    return result
}
