package com.example.myapplication.nfc

object A101Command {
    const val PREFIX = "A101"
    fun formatDisplay(): String = "NFC 모드 진입"
    fun formatPayload(): String = PREFIX
    fun isValid(): Boolean = true
}