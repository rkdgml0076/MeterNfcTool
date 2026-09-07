package com.example.myapplication.nfc

object A109Command {
    const val PREFIX = "A109"

    fun formatDisplay(): String = "A109 (단말 RESET)"

    fun formatPayload(): String = PREFIX

    fun isValid(): Boolean = true
}
