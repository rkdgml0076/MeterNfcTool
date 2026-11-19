package com.example.myapplication.nfc

object A111Command {
    const val PREFIX = "A111"

    fun formatDisplay(): String = "단말기 재부팅"

    fun formatPayload(): String = PREFIX

    fun isValid(): Boolean = true
}
