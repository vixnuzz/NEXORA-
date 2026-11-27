package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Business
import com.example.model.DashboardMetrics
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.AiUiState
import com.example.viewmodel.BusinessWorkspaceData

@Composable
fun AiAssistantScreen(
    aiViewModel: AiAssistantViewModel,
    business: Business?,
    metrics: DashboardMetrics,
    workspace: BusinessWorkspaceData
) {
    val messages by aiViewModel.messages.collectAsState()
    val uiState by aiViewModel.uiState.collectAsState()
    var promptInput by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "Executive Performance" to "Analyze our overall business performance right now including revenue, expenses, net profit, active projects, and key operational risks.",
            "Sales & Expense Summary" to "Summarize our current sales orders, invoices, and expense breakdown with specific figures.",
            "Low-Stock Risk Alert" to "Identify all low-stock or at-risk inventory products and recommend immediate purchase order quantities.",
            "Revenue Growth Ideas" to "Based on our CRM pipeline, marketing campaigns, and sales data, suggest 4 high-impact ways to grow revenue this quarter.",
            "Draft Client Email" to "Draft a professional executive business follow-up email to our top CRM lead or pending invoice customer.",
            "Board Business Report" to "Generate a structured board-ready executive business report covering Finance, CRM, HR, Inventory, and Support."
        )
    }

    val isGenerating = uiState is AiUiState.Loading || uiState is AiUiState.Streaming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("NEXORA AI Copilot", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Live Gemini intelligence grounded in ${business?.name ?: "your business"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (messages.isNotEmpty()) {
                IconButton(onClick = { aiViewModel.clearConversation() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat")
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Quick executive prompts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { (label, prompt) ->
                AssistChip(
                    onClick = {
                        if (!isGenerating) {
                            aiViewModel.sendPrompt(prompt, business, metrics, workspace)
                        }
                    },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && uiState is AiUiState.Idle) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Ask anything about your live business data", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "NEXORA AI Copilot securely analyzes your real-time Firestore revenue, expenses, CRM pipeline, inventory stock levels, HR metrics, and support tickets via Firebase AI Logic.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.widthIn(max = 560.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            when (val st = uiState) {
                is AiUiState.Loading -> {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Analyzing live business metrics...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is AiUiState.Streaming -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.widthIn(max = 560.dp)
                        ) {
                            Text(
                                text = st.partialText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is AiUiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AI Copilot Error: ${st.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                AiUiState.Idle -> {}
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask AI Copilot or request a report/email...") },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            FilledIconButton(
                onClick = {
                    val text = promptInput.trim()
                    if (text.isNotBlank() && !isGenerating) {
                        promptInput = ""
                        aiViewModel.sendPrompt(text, business, metrics, workspace)
                    }
                },
                enabled = promptInput.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt")
            }
        }
    }
}
