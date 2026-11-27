package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NexoraRepository(
    val db: FirebaseFirestore,
    val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    internal fun userDoc(uid: String) = db.collection("users").document(uid)

    internal fun businessesCol(uid: String) = userDoc(uid).collection("businesses")

    internal fun businessDoc(uid: String, businessId: String) =
        businessesCol(uid).document(businessId)

    internal fun moduleCol(uid: String, businessId: String, collection: String): CollectionReference =
        businessDoc(uid, businessId).collection(collection)

    internal inline fun <reified T> observeSubcollection(
        businessId: String,
        collectionName: String,
        orderField: String = "createdAt"
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        if (businessId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/businesses/$businessId/$collectionName"
        emitAll(
            moduleCol(uid, businessId, collectionName)
                .orderBy(orderField, Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        T::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            userDoc(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeBusinesses(): Flow<List<Business>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/businesses"
        emitAll(
            businessesCol(uid)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Business::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCrmLeads(businessId: String) = observeSubcollection<CrmLead>(businessId, "crm_leads")
    fun observeCrmContacts(businessId: String) = observeSubcollection<CrmContact>(businessId, "crm_contacts")
    fun observeFinanceRecords(businessId: String) = observeSubcollection<FinanceRecord>(businessId, "finance_records")
    fun observeInvoices(businessId: String) = observeSubcollection<Invoice>(businessId, "invoices")
    fun observeEmployees(businessId: String) = observeSubcollection<Employee>(businessId, "employees")
    fun observeProjects(businessId: String) = observeSubcollection<Project>(businessId, "projects")
    fun observeProjectTasks(businessId: String) = observeSubcollection<ProjectTask>(businessId, "project_tasks")
    fun observeInventoryProducts(businessId: String) = observeSubcollection<InventoryProduct>(businessId, "inventory_products")
    fun observePurchaseOrders(businessId: String) = observeSubcollection<PurchaseOrder>(businessId, "purchase_orders")
    fun observeSalesOrders(businessId: String) = observeSubcollection<SalesOrder>(businessId, "sales_orders")
    fun observeMarketingCampaigns(businessId: String) = observeSubcollection<MarketingCampaign>(businessId, "marketing_campaigns")
    fun observeSupportTickets(businessId: String) = observeSubcollection<SupportTicket>(businessId, "support_tickets")
    fun observeActivityLogs(businessId: String) = observeSubcollection<ActivityLog>(businessId, "activity_logs")

    suspend fun ensureUserProfile(
        displayName: String,
        email: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val snap = userDoc(uid).get().await()
            if (!snap.exists()) {
                val safeName = displayName.ifBlank { email.substringBefore("@").ifBlank { "Executive User" } }
                val safeEmail = email.ifBlank { "user@nexora.local" }
                val payload = timestampedCreateMap(
                    "userId" to uid,
                    "displayName" to safeName.take(100),
                    "email" to safeEmail.take(150),
                    "jobTitle" to "Business Administrator",
                    "phone" to "",
                    "activeBusinessId" to ""
                )
                userDoc(uid).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        jobTitle: String,
        phone: String,
        activeBusinessId: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            require(displayName.isNotBlank()) { "Display name is required" }
            val payload = timestampedUpdateMap(
                "displayName" to displayName.trim().take(100),
                "jobTitle" to jobTitle.trim().take(100),
                "phone" to phone.trim().take(40),
                "activeBusinessId" to activeBusinessId?.take(128)
            )
            userDoc(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setActiveBusinessOnProfile(businessId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            userDoc(uid).update(
                timestampedUpdateMap("activeBusinessId" to businessId.take(128))
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createBusiness(
        name: String,
        industry: String,
        currency: String,
        taxRate: Double,
        address: String,
        email: String,
        activeRole: UserRole = UserRole.BUSINESS_OWNER
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = businessesCol(uid).document()
        return try {
            require(name.isNotBlank()) { "Business name is required" }
            require(industry.isNotBlank()) { "Industry is required" }
            val payload = timestampedCreateMap(
                "ownerId" to uid,
                "name" to name.trim().take(100),
                "industry" to industry.trim().take(80),
                "currency" to currency.trim().ifBlank { "USD" }.take(10),
                "taxRate" to taxRate.coerceIn(0.0, 100.0),
                "address" to address.trim().take(200),
                "email" to email.trim().take(150),
                "activeRole" to activeRole.name,
                "demoSeeded" to false
            )
            ref.set(payload).await()
            setActiveBusinessOnProfile(ref.id)
            logActivity(
                businessId = ref.id,
                actorName = auth.currentUser?.displayName ?: "Owner",
                actorRole = activeRole.label,
                module = "ORGANIZATION",
                action = "CREATE",
                summary = "Created workspace ${name.trim()}"
            )
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateBusiness(
        businessId: String,
        name: String,
        industry: String,
        currency: String,
        taxRate: Double,
        address: String,
        email: String,
        activeRole: UserRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = businessDoc(uid, businessId)
        return try {
            require(name.isNotBlank()) { "Business name is required" }
            val payload = timestampedUpdateMap(
                "name" to name.trim().take(100),
                "industry" to industry.trim().take(80),
                "currency" to currency.trim().ifBlank { "USD" }.take(10),
                "taxRate" to taxRate.coerceIn(0.0, 100.0),
                "address" to address.trim().take(200),
                "email" to email.trim().take(150),
                "activeRole" to activeRole.name
            )
            ref.update(payload).await()
            logActivity(
                businessId = businessId,
                actorName = auth.currentUser?.displayName ?: "User",
                actorRole = activeRole.label,
                module = "SETTINGS",
                action = "UPDATE",
                summary = "Updated organization settings & role (${activeRole.label})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateBusinessRole(businessId: String, role: UserRole): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = businessDoc(uid, businessId)
        return try {
            ref.update(timestampedUpdateMap("activeRole" to role.name)).await()
            logActivity(
                businessId = businessId,
                actorName = auth.currentUser?.displayName ?: "User",
                actorRole = role.label,
                module = "SECURITY",
                action = "ROLE_SWITCH",
                summary = "Switched active role to ${role.label}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun logActivity(
        businessId: String,
        actorName: String,
        actorRole: String,
        module: String,
        action: String,
        summary: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        if (businessId.isBlank()) return
        val ref = moduleCol(uid, businessId, "activity_logs").document()
        try {
            val payload = serverTimestampMap(
                "userId" to uid,
                "businessId" to businessId,
                "actorName" to actorName.ifBlank { "System" }.take(120),
                "actorRole" to actorRole.ifBlank { "Owner" }.take(60),
                "module" to module.take(60),
                "action" to action.take(60),
                "summary" to summary.take(300),
                "unread" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }

    suspend fun markNotificationRead(businessId: String, logId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val ref = moduleCol(uid, businessId, "activity_logs").document(logId)
        return try {
            ref.update(mapOf("unread" to false)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(businessId: String, logIds: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val batch = db.batch()
            logIds.take(400).forEach { id ->
                batch.update(moduleCol(uid, businessId, "activity_logs").document(id), mapOf("unread" to false))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/businesses/$businessId/activity_logs")
            Result.failure(e)
        }
    }
}
