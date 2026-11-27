package com.example.data

import android.util.Log
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val AI_TAG = "NexoraAiRepo"

class AiBusinessAssistantRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun buildBusinessContextSnapshot(
        business: Business?,
        metrics: DashboardMetrics,
        products: List<InventoryProduct>,
        leads: List<CrmLead>,
        invoices: List<Invoice>,
        tickets: List<SupportTicket>,
        campaigns: List<MarketingCampaign>
    ): String {
        val currency = business?.currency ?: "USD"
        val lowStockList = products.filter { it.needsRestock }
            .joinToString("; ") { "${it.name} (${it.sku}): ${it.stockQty} left, reorder <= ${it.reorderLevel}" }
            .ifBlank { "None" }
        val openLeads = leads.filter { it.stageEnum() != LeadStage.WON && it.stageEnum() != LeadStage.LOST }
            .joinToString("; ") { "${it.company} (${it.name}): ${it.stageEnum().label}, $currency ${it.dealValue}" }
            .ifBlank { "None" }
        val pendingInvoices = invoices.filter {
            it.statusEnum() == InvoiceStatus.PENDING || it.statusEnum() == InvoiceStatus.OVERDUE || it.statusEnum() == InvoiceStatus.SENT
        }.joinToString("; ") { "${it.invoiceNumber} ${it.customerName}: $currency ${it.amount} (${it.statusEnum().label}, due ${it.dueDate})" }
            .ifBlank { "None" }
        val openTickets = tickets.filter { it.statusEnum() != TicketStatus.RESOLVED && it.statusEnum() != TicketStatus.CLOSED }
            .joinToString("; ") { "${it.ticketNumber} [${it.priorityEnum().label}] ${it.subject} (${it.customerName})" }
            .ifBlank { "None" }
        val campaignSummary = campaigns.joinToString("; ") {
            "${it.name} [${it.channelEnum().label}]: ${it.conversions} conversions, ${String.format("%.1f", it.ctrPercent)}% CTR"
        }.ifBlank { "None" }

        return """
            |Business: ${business?.name ?: "Workspace"} (${business?.industry ?: "General"}, Currency: $currency, Active Role: ${business?.roleEnum()?.label ?: "Owner"})
            |Live KPIs:
            |- Total Revenue: $currency ${String.format("%.2f", metrics.totalRevenue)}
            |- Total Expenses: $currency ${String.format("%.2f", metrics.totalExpenses)}
            |- Net Profit: $currency ${String.format("%.2f", metrics.netProfit)}
            |- Active Customers: ${metrics.activeCustomers}
            |- Total Employees: ${metrics.totalEmployees}
            |- Pending Invoices: ${metrics.pendingInvoicesCount} (Total $currency ${String.format("%.2f", metrics.pendingInvoicesTotal)})
            |- Active Projects: ${metrics.activeProjects}
            |- Open Pipeline Value: $currency ${String.format("%.2f", metrics.pipelineValue)}
            |Low-Stock Inventory Alerts: $lowStockList
            |Open CRM Pipeline: $openLeads
            |Pending/Overdue Invoices: $pendingInvoices
            |Open Support Tickets: $openTickets
            |Marketing Campaigns: $campaignSummary
        """.trimMargin()
    }

    fun streamAssistantReply(
        userPrompt: String,
        businessContext: String,
        recentHistory: List<Pair<String, String>>
    ): Flow<String> = flow {
        val historyText = if (recentHistory.isEmpty()) "" else {
            recentHistory.takeLast(6).joinToString("\n") { (role, text) -> "$role: $text" }
        }
        val fullPrompt = """
            |You are NEXORA ONE Copilot, an executive enterprise business intelligence assistant.
            |Base every number, entity name, and recommendation strictly on the live business data below.
            |If a metric is zero or a section is empty, clearly say so and suggest concrete next steps.
            |Keep responses structured, concise, actionable, and executive-ready.
            |
            |=== LIVE BUSINESS SNAPSHOT ===
            |$businessContext
            |
            |=== RECENT CONVERSATION ===
            |$historyText
            |
            |=== USER REQUEST ===
            |$userPrompt
        """.trimMargin()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "AI generation timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "AI generation failed", e)
            throw e
        }
    }
}
