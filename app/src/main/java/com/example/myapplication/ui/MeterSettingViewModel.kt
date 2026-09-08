package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.nfc.NfcWriteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeterSettingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MeterSettingUiState())
    val uiState: StateFlow<MeterSettingUiState> = _uiState.asStateFlow()

    fun onIntegerChange(value: String) {
        _uiState.update { it.copy(integerPart = value) }
    }

    fun onDecimalChange(value: String) {
        _uiState.update { it.copy(decimalPart = value) }
    }

    fun onSingleValueChange(value: String) {
        _uiState.update { it.copy(singleValue = value) }
    }

    fun onCommandTypeChange(@Suppress("UNUSED_PARAMETER") commandType: CommandType) {
        _uiState.update {
            it.copy(
                singleValue = "",
                showConfirmDialog = false,
                pendingCommandType = null,
                pendingPayload = "",
            )
        }
    }

    fun onWriteClick(commandType: CommandType, payload: String) {
        _uiState.update {
            it.copy(
                showConfirmDialog = true,
                pendingCommandType = commandType,
                pendingPayload = payload
            )
        }
    }

    fun onDismissConfirm() {
        _uiState.update {
            it.copy(
                showConfirmDialog = false,
                pendingCommandType = null,
                pendingPayload = ""
            )
        }
    }

    fun onConfirmWrite(commandType: CommandType, payload: String) {
        onDismissConfirm()

        viewModelScope.launch {
            _uiState.update { it.copy(writeState = NfcWriteState.AwaitingTag) }
            // 실제 NFC 쓰기 처리 로직 연결부
            executeNfcWrite(commandType, payload)
        }
    }

    private suspend fun executeNfcWrite(commandType: CommandType, payload: String) {
        try {
            _uiState.update { it.copy(writeState = NfcWriteState.Writing) }

            // 테스트/실제 NFC 성공 시 호출
            onNfcWriteSuccess(commandType, payload)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(writeState = NfcWriteState.Error(e.message ?: "NFC 전송 실패"))
            }
        }
    }

    // MeterSettingViewModel.kt
    fun onNfcWriteSuccess(commandType: CommandType, displayValue: String) {
        _uiState.update { currentState ->
            val newSessionState = when (commandType) {
                CommandType.NFC_START -> true
                CommandType.NFC_EXIT -> false
                else -> currentState.isNfcSessionActive
            }

            currentState.copy(
                writeState = NfcWriteState.Success(displayValue), // 👈 (displayValue) 삭제
                isNfcSessionActive = newSessionState
            )
        }
    }
}