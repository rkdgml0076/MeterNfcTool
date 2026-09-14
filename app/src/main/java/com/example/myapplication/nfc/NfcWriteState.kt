package com.example.myapplication.nfc

sealed interface NfcWriteState {
    object Idle : NfcWriteState
    object AwaitingTag : NfcWriteState
    object Writing : NfcWriteState
    data class Success(val displayValue: String, val isRead: Boolean = false) : NfcWriteState
    data class Error(val message: String) : NfcWriteState
}