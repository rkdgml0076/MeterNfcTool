package com.example.myapplication.nfc

object A105Command {
    const val PREFIX = "A105"

    fun formatDisplay(value: String): String {
        return value.padStart(8, '0').takeLast(8)
    }

    fun formatPayload(value: String): String {
        val padded = value.padStart(8, '0').takeLast(8)
        return "$PREFIX$padded"
    }

    fun isValid(value: String): Boolean {
        if (value.isEmpty()) return false
        if (!value.all { it.isDigit() }) return false
        if (value.length > 8) return false
        return true
    }
}