package com.example.myapplication.nfc

object A102Command {
    const val PREFIX = "A102"
    fun formatDisplay(): String = "NFC 모드 종료"
    fun formatPayload(): String = PREFIX
    fun isValid(): Boolean = true
}