package com.example.myapplication.nfc

sealed interface NfcWriteState {
    data object Idle : NfcWriteState

    data object AwaitingTag : NfcWriteState

    data object Writing : NfcWriteState

    data class Success(
        val displayValue: String,
        val command: String,
    ) : NfcWriteState

    data class Error(val message: String) : NfcWriteState
}
