package com.example.myapplication.nfc

object A109Command {
    const val PREFIX = "A109"

    fun formatDisplay(): String = "단말기 재부팅"

    fun formatPayload(): String = PREFIX

    fun isValid(): Boolean = true
}
