package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiBusinessAssistantRepository
import com.example.model.Business
import com.example.model.DashboardMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String
)

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val partialText: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiAssistantViewModel(
    private val aiRepository: AiBusinessAssistantRepository = AiBusinessAssistantRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun sendPrompt(
        prompt: String,
        business: Business?,
        metrics: DashboardMetrics,
        workspace: BusinessWorkspaceData
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return
        if (_uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) return

        val userMsg = AiChatMessage(
            id = "u_${System.currentTimeMillis()}",
            isUser = true,
            text = cleanPrompt
        )
        val historySnapshot = _messages.value.map {
            (if (it.isUser) "User" else "Copilot") to it.text
        }
        _messages.value = _messages.value + userMsg
        _uiState.value = AiUiState.Loading

        val contextSnapshot = aiRepository.buildBusinessContextSnapshot(
            business = business,
            metrics = metrics,
            products = workspace.products,
            leads = workspace.leads,
            invoices = workspace.invoices,
            tickets = workspace.tickets,
            campaigns = workspace.campaigns
        )

        viewModelScope.launch {
            var latestText = ""
            try {
                aiRepository.streamAssistantReply(cleanPrompt, contextSnapshot, historySnapshot)
                    .collect { partial ->
                        latestText = partial
                        _uiState.value = AiUiState.Streaming(partial)
                    }
                if (latestText.isNotBlank()) {
                    _messages.value = _messages.value + AiChatMessage(
                        id = "a_${System.currentTimeMillis()}",
                        isUser = false,
                        text = latestText
                    )
                    _uiState.value = AiUiState.Idle
                } else {
                    _uiState.value = AiUiState.Error("Copilot returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("AiAssistantVM", "AI request timed out", e)
                _uiState.value = AiUiState.Error("Request timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AiAssistantVM", "AI request failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "AI request failed.")
            }
        }
    }

    fun clearConversation() {
        _messages.value = emptyList()
        _uiState.value = AiUiState.Idle
    }
}
