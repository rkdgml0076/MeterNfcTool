package com.example.myapplication.nfc

object A103Command {
    const val PREFIX = "A103"

    fun formatDisplay(integerPart: String, decimalPart: String): String {
        val integer = integerPart.padStart(2, '0').takeLast(2)
        val decimal = decimalPart.padStart(6, '0').takeLast(6)
        return "$integer-$decimal"
    }

    fun formatPayload(integerPart: String, decimalPart: String): String {
        val integer = integerPart.padStart(2, '0').takeLast(2)
        val decimal = decimalPart.padStart(6, '0').takeLast(6)
        return "$PREFIX$integer$decimal"
    }

    fun isValid(integerPart: String, decimalPart: String): Boolean {
        if (integerPart.isEmpty() || decimalPart.isEmpty()) return false
        if (!integerPart.all { it.isDigit() } || integerPart.length > 2) return false
        if (!decimalPart.all { it.isDigit() } || decimalPart.length > 6) return false
        return true
    }
}
