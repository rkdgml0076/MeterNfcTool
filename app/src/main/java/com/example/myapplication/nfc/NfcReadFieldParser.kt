package com.example.myapplication.nfc

/**
 * NFC Read를 필드 단위로 자른다.
 * 위치와 길이는 16진수 글자 기준이다. 16진수 2글자가 1바이트다.
 * 전체 규격은 80글자(40바이트)이고, 68글자(34바이트)처럼 짧으면 있는 만큼만 채운다.
 */
object NfcReadFieldParser {

    /** @param offset 1-based 시작 글자(hex), @param length hex 글자 수 */
    data class FieldSpec(val name: String, val offset: Int, val length: Int)

    data class Field(val name: String, val value: String, val complete: Boolean)

    const val TOTAL_HEX_CHARS = 80

    // [길이, 시작위치, 필드명]
    val FIELDS: List<FieldSpec> = listOf(
        FieldSpec("cmdByte", 1, 4),
        FieldSpec("meterNo", 5, 8),
        FieldSpec("FinalReport", 13, 12),
        FieldSpec("Finalmeter", 25, 12),
        FieldSpec("msrCnt", 37, 2),
        FieldSpec("msrStdValue", 39, 8),
        FieldSpec("meterCaliber", 47, 2),
        FieldSpec("meterStatus", 49, 2),
        FieldSpec("rsrp", 51, 4),
        FieldSpec("NoAck", 55, 2),
        FieldSpec("mno", 57, 2),
        FieldSpec("modem", 59, 2),
        FieldSpec("devVolt", 61, 2),
        FieldSpec("comOnOff", 63, 2),
        FieldSpec("ctnNO", 65, 8),
        FieldSpec("devFw", 73, 4),
        FieldSpec("meterCode", 77, 2),
        FieldSpec("format", 79, 2),
    )

    /** hex 글자열을 필드 목록으로 분류한다. 값은 2글자당 1바이트로 띄워 보여 준다. */
    fun parse(bytes: ByteArray): List<Field> {
        val hex = toFieldHex(bytes)
        return FIELDS.map { spec ->
            val start = spec.offset - 1
            val end = start + spec.length
            val slice = when {
                start >= hex.length -> ""
                else -> hex.substring(start, minOf(end, hex.length))
            }
            Field(
                name = spec.name,
                value = slice.chunked(2).joinToString(" "),
                complete = slice.length == spec.length,
            )
        }
    }

    /** 화면 표시용 문자열. 한 줄에 `필드명 : HEX` 형태로 나열한다. */
    fun format(data: NfcReadData): String {
        val bytes = data.bytes
        if (bytes.isEmpty()) return data.hex

        val hex = toFieldHex(bytes)
        val byteCount = hex.length / 2
        val nameWidth = FIELDS.maxOf { it.name.length }
        val lines = parse(bytes).map { field ->
            val shown = if (field.value.isEmpty()) "--" else field.value
            val mark = if (field.complete) "" else " (!)"
            "${field.name.padEnd(nameWidth)} : $shown$mark"
        }

        val header = if (hex.length == TOTAL_HEX_CHARS) {
            "$byteCount Byte"
        } else {
            "$byteCount Byte (${hex.length} hex / 전체 ${TOTAL_HEX_CHARS / 2} Byte)"
        }

        return listOf(header).plus(lines).joinToString("\n")
    }

    /**
     * 태그가 "A1B2..." 같은 hex 글자면 그 글자를 그대로 쓴다.
     * 이미 바이너리면 바이트를 hex 글자로 바꾼 뒤 같은 위치로 자른다.
     */
    private fun toFieldHex(bytes: ByteArray): String {
        val text = String(bytes, Charsets.ISO_8859_1).filter { !it.isWhitespace() }
        return if (text.isNotEmpty() && text.all { it.isHexDigit() }) {
            text.uppercase()
        } else {
            bytes.joinToString("") { "%02X".format(it) }
        }
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
