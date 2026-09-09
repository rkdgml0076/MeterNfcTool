package com.example.myapplication.nfc

object A107Command {
    const val PREFIX = "A107"

    fun formatDisplay(value: String): String {
        val intValue = value.toIntOrNull() ?: value.toIntOrNull(16) ?: return value
        return "${intValue}시간"
    }

    fun formatPayload(value: String): String = "$PREFIX${formatByte(value)}"

    fun isValid(value: String): Boolean {
        if (value.isEmpty()) return false
        if (value.length > 2) return false
        return value.all { it.isHexDigit() }
    }

    // 1byte = ASCII 2글자 (예: "6" -> "06", "0A" -> "0A")
    private fun formatByte(value: String): String {
        return value.uppercase().padStart(2, '0').takeLast(2)
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
