package com.example.myapplication.nfc

object NfcPayloadParser {
    fun describe(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) return "(빈 데이터)"

        return when {
            text == A101Command.PREFIX -> "${A101Command.PREFIX} (${A101Command.formatDisplay()})"
            text == A102Command.PREFIX -> "${A102Command.PREFIX} (${A102Command.formatDisplay()})"
            text == A109Command.PREFIX -> "${A109Command.PREFIX} (${A109Command.formatDisplay()})"
            text.startsWith(A103Command.PREFIX) && text.length >= 12 -> {
                val body = text.removePrefix(A103Command.PREFIX)
                val year = body.take(2)
                val number = body.drop(2).take(6)
                "${A103Command.PREFIX} 계량기 번호 $year-$number"
            }
            text.startsWith(A105Command.PREFIX) && text.length >= 12 -> {
                val body = text.removePrefix(A105Command.PREFIX).take(8)
                "${A105Command.PREFIX} 검침 값 ${A105Command.formatDisplay(body)}"
            }
            text.startsWith(A107Command.PREFIX) && text.length >= 6 -> {
                val body = text.removePrefix(A107Command.PREFIX).take(2)
                "${A107Command.PREFIX} 검침 주기 ${A107Command.formatDisplay(body)}"
            }
            else -> text
        }
    }
}
