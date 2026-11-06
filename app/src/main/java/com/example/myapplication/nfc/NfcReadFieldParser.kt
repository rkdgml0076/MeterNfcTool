package com.example.myapplication.nfc

/**
 * NFC Read를 필드 단위로 자른다.
 * 위치와 길이는 16진수 글자 기준이다. 16진수 2글자가 1바이트다.
 * 전체 규격은 80글자(40바이트)이고, 68글자(34바이트)처럼 짧으면 있는 만큼만 채운다.
 */
object NfcReadFieldParser {

    /** @param offset 1-based 시작 글자(hex), @param length hex 글자 수, @param label 화면 이름 */
    data class FieldSpec(
        val name: String,
        val offset: Int,
        val length: Int,
        val label: String = name,
    )

    data class Field(val name: String, val value: String, val complete: Boolean)

    const val TOTAL_HEX_CHARS = 80

    // [길이, 시작위치, 필드명]
    val FIELDS: List<FieldSpec> = listOf(
        FieldSpec("cmdByte", 1, 4, "저장정보"),
        FieldSpec("meterNo", 5, 8, "계량기번호"),
        FieldSpec("FinalReport", 13, 12, "최종전송시간"),
        FieldSpec("Finalmeter", 25, 12, "최종검침시간"),
        FieldSpec("msrCnt", 37, 2, "데이터 개수"),
        FieldSpec("msrStdValue", 39, 8, "검침값"),
        FieldSpec("meterCaliber", 47, 2, "구경"),
        FieldSpec("meterStatus", 49, 2, "경보상태"),
        FieldSpec("rsrp", 51, 4, "RSRP"),
        FieldSpec("NoAck", 55, 2, "ACK 미수신 횟수"),
        FieldSpec("mno", 57, 2, "통신사"),
        FieldSpec("modem", 59, 2, "모뎀상태"),
        FieldSpec("devVolt", 61, 2, "배터리"),
        FieldSpec("comOnOff", 63, 2, "통신 ON/OFF상태"),
        FieldSpec("ctnNO", 65, 8, "전화번호"),
        FieldSpec("devFw", 73, 4, "F/W"),
        FieldSpec("meterCode", 77, 2, "기물코드"),
        FieldSpec("format", 79, 2, "포맷 버전"),
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
                name = spec.label,
                value = displayValue(spec.name, slice),
                complete = slice.length == spec.length,
            )
        }
    }

    /** 화면 표시용 문자열. 필드명 다음 줄에 값을 둔다. */
    fun format(data: NfcReadData): String {
        val bytes = data.bytes
        if (bytes.isEmpty()) return data.hex

        val hex = toFieldHex(bytes)
        val byteCount = hex.length / 2
        val lines = parse(bytes).map { field ->
            val shown = if (field.value.isEmpty()) "--" else field.value
            val mark = if (field.complete) "" else " (일부)"
            "${field.name}\n$shown$mark"
        }

        val header = if (hex.length == TOTAL_HEX_CHARS) {
            "$byteCount Byte"
        } else {
            "$byteCount Byte (${hex.length} hex / 전체 ${TOTAL_HEX_CHARS / 2} Byte)"
        }

        return listOf(header).plus(lines).joinToString("\n\n")
    }

    private fun displayValue(name: String, hexSlice: String): String {
        if (hexSlice.isEmpty()) return ""
        return when (name) {
            "meterNo" -> parseMeterNo(hexSlice)
            "msrStdValue" -> littleEndianScaled(hexSlice)
            "FinalReport", "Finalmeter" -> parseDateTime(hexSlice)
            "devFw" -> parseFirmware(hexSlice)
            "cmdByte" -> fixedOrHex(hexSlice, mapOf("D500" to "STOR_RES"))
            "meterCaliber" -> fixedOrHex(hexSlice, mapOf("13" to "15mm"))
            "mno" -> fixedOrHex(hexSlice, mapOf("98" to "LGU+"))
            "comOnOff" -> fixedOrHex(hexSlice, mapOf("01" to "ON", "0F" to "OFF"))
            "modem" -> fixedOrHex(hexSlice, mapOf("00" to "정상"))
            "devVolt" -> parseBattery(hexSlice)
            "ctnNO" -> "01$hexSlice"
            else -> spacedHex(hexSlice)
        }
    }

    private fun fixedOrHex(hexSlice: String, table: Map<String, String>): String {
        return table[hexSlice.uppercase()] ?: spacedHex(hexSlice)
    }

    private fun spacedHex(hexSlice: String): String {
        return hexSlice.chunked(2).joinToString(" ")
    }

    /** 8글자를 앞 2글자-뒤 6글자로 나눈다. 12345678 → 12-345678 */
    private fun parseMeterNo(hexSlice: String): String {
        if (hexSlice.length < 3) return hexSlice
        return "${hexSlice.take(2)}-${hexSlice.drop(2)}"
    }

    /** hex 바이트를 10으로 나눈 전압. 23 → 3.5V, 24 → 3.6V, 22 → 3.4V */
    private fun parseBattery(hexSlice: String): String {
        val value = hexSlice.toIntOrNull(16) ?: return spacedHex(hexSlice)
        return "${value / 10}.${value % 10}V"
    }

    /** 바이트 두 개의 십진수를 붙여 V버전으로 보여 준다. 0102 → V12, 0002 → V02 */
    private fun parseFirmware(hexSlice: String): String {
        if (hexSlice.length % 2 != 0) return spacedHex(hexSlice)
        val bytes = hexSlice.chunked(2).map { it.toIntOrNull(16) }
        if (bytes.size < 2 || bytes.any { it == null }) return spacedHex(hexSlice)
        val values = bytes.map { it!! }
        return "V${values[0]}${values[1]}"
    }

    /**
     * 6바이트를 순서대로 년(2000+), 월, 일, 시, 분, 초로 읽는다.
     * 예: 1A0B06141400 → 2026-11-06 20:20:00
     */
    private fun parseDateTime(hexSlice: String): String {
        if (hexSlice.length % 2 != 0) return spacedHex(hexSlice)
        val bytes = hexSlice.chunked(2).map { it.toIntOrNull(16) }
        if (bytes.size < 6 || bytes.any { it == null }) return spacedHex(hexSlice)
        val values = bytes.map { it!! }
        return "%04d-%02d-%02d %02d:%02d:%02d".format(
            2000 + values[0],
            values[1],
            values[2],
            values[3],
            values[4],
            values[5],
        )
    }

    /**
     * 페이로드 앞 바이트를 하위 바이트로 보는 리틀엔디안 정수.
     * 정수 뒤에 소수점 3자리와 ton을 붙인다.
     * CD6D0D00 → 880077 → 880.077 ton
     */
    private fun littleEndianScaled(hexSlice: String): String {
        var value = 0L
        hexSlice.chunked(2).forEachIndexed { index, pair ->
            if (pair.length < 2) return@forEachIndexed
            val byte = pair.toInt(16)
            value += byte.toLong() shl (8 * index)
        }
        val whole = value / 1000
        val fraction = (value % 1000).toInt()
        return "%d.%03d ton".format(whole, fraction)
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
