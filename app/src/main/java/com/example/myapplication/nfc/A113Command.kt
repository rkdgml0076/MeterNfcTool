package com.example.myapplication.nfc

object A113Command {
    const val PREFIX = "A113"
    private const val MAX_RAW = 0xFFFF

    fun formatDisplay(positive: String, negative: String): String {
        return "${formatSigned(positive, "+")}, ${formatSigned(negative, "-")}"
    }

    fun formatDisplay(raw: String, positiveDirection: Boolean): String {
        return if (positiveDirection) {
            formatDisplay(raw, "0")
        } else {
            formatDisplay("0", raw)
        }
    }

    fun formatPayload(positive: String, negative: String): String {
        return "$PREFIX${toHex(positive)}${toHex(negative)}"
    }

    fun formatPayload(raw: String, positiveDirection: Boolean): String {
        return if (positiveDirection) {
            formatPayload(raw, "0")
        } else {
            formatPayload("0", raw)
        }
    }

    fun isValid(raw: String, positiveDirection: Boolean, meterDigits: String): Boolean {
        if (!isRaw(raw)) return false
        val value = raw.toIntOrNull() ?: 0
        val max = if (positiveDirection) MAX_RAW else maxNegativeRaw(meterDigits)
        return value <= max
    }

    /** A105 검침값(0.001 ton 단위)보다 더 내리지 못하는 보정 정수. 1은 0.1이다. */
    fun maxNegativeRaw(meterDigits: String): Int {
        val digits = meterDigits.filter { it.isDigit() }.take(8)
        if (digits.isEmpty()) return 0
        return (digits.toLong() / 100L).coerceIn(0L, MAX_RAW.toLong()).toInt()
    }

    private fun formatSigned(raw: String, sign: String): String {
        val value = raw.toIntOrNull() ?: 0
        return "$sign${value / 10}.${value % 10}"
    }

    private fun toHex(raw: String): String {
        val value = (raw.toIntOrNull() ?: 0).coerceIn(0, MAX_RAW)
        return "%04X".format(value)
    }

    private fun isRaw(raw: String): Boolean {
        if (raw.isEmpty()) return true
        val value = raw.toIntOrNull() ?: return false
        return value in 0..MAX_RAW
    }
}
