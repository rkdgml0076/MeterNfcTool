package com.example.myapplication.ui

import com.example.myapplication.nfc.NfcWriteState

data class MeterSettingUiState(
    val integerPart: String = "",
    val decimalPart: String = "",
    val singleValue: String = "",
    val writeState: NfcWriteState = NfcWriteState.Idle,
    val nfcAvailable: Boolean = true,
    val nfcEnabled: Boolean = true,
    val showConfirmDialog: Boolean = false,
    val isNfcSessionActive: Boolean = false, // A101 성공 시 true, A102 성공 시 false
    val pendingCommandType: CommandType? = null,
    val pendingPayload: String = ""
)