package com.example.myapplication.nfc

sealed interface NfcWriteState {
    object Idle : NfcWriteState
    object AwaitingTag : NfcWriteState
    object Writing : NfcWriteState
    data class Success(val displayValue: String) : NfcWriteState // 👈 String 인자 필요
    data class Error(val message: String) : NfcWriteState
}