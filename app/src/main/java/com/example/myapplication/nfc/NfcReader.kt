package com.example.myapplication.nfc

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef

/** 태그 본문 바이트. 화면에는 HEX로만 보여 준다. */
data class NfcReadData(val bytes: ByteArray, val hex: String)

object NfcReader {
    fun read(tag: Tag): Result<NfcReadData> {
        val ndef = Ndef.get(tag)
            ?: return Result.failure(IllegalStateException("NDEF를 지원하지 않는 태그입니다."))

        return try {
            ndef.connect()
            val message = ndef.ndefMessage
                ?: ndef.cachedNdefMessage
                ?: return Result.failure(IllegalStateException("태그에 저장된 데이터가 없습니다."))

            val chunks = message.records.mapNotNull { decodeRecord(it) }
            if (chunks.isEmpty()) {
                Result.failure(IllegalStateException("읽을 수 있는 데이터가 없습니다."))
            } else {
                val bytes = chunks.reduce { acc, chunk -> acc + chunk }
                Result.success(NfcReadData(bytes = bytes, hex = toHex(bytes)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { ndef.close() }
        }
    }

    /** 기존 호환용: HEX 문자열만 필요할 때 */
    fun readText(tag: Tag): Result<String> = read(tag).map { it.hex }

    private fun decodeRecord(record: NdefRecord): ByteArray? {
        val isText = record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)
        val bytes = if (isText) {
            extractTextBytes(record.payload) ?: record.payload
        } else {
            record.payload
        }
        if (bytes.isEmpty()) return null
        return bytes
    }

    private fun extractTextBytes(payload: ByteArray): ByteArray? {
        if (payload.isEmpty()) return null
        val status = payload[0].toInt() and 0xFF
        val languageCodeLength = status and 0x3F
        val textOffset = 1 + languageCodeLength
        if (textOffset > payload.size) return null
        return payload.copyOfRange(textOffset, payload.size)
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}
