package com.example.myapplication.nfc

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef

object NfcReader {
    fun readText(tag: Tag): Result<String> {
        val ndef = Ndef.get(tag)
            ?: return Result.failure(IllegalStateException("NDEF를 지원하지 않는 태그입니다."))

        return try {
            ndef.connect()
            val message = ndef.ndefMessage
                ?: ndef.cachedNdefMessage
                ?: return Result.failure(IllegalStateException("태그에 저장된 데이터가 없습니다."))

            val hexValues = message.records.mapNotNull { decodeRecordAsHex(it) }
            if (hexValues.isEmpty()) {
                Result.failure(IllegalStateException("읽을 수 있는 데이터가 없습니다."))
            } else {
                Result.success(hexValues.joinToString("\n"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun decodeRecordAsHex(record: NdefRecord): String? {
        val bytes = if (
            record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)
        ) {
            extractTextBytes(record.payload) ?: record.payload
        } else {
            record.payload
        }

        if (bytes.isEmpty()) return null
        return toHex(bytes)
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
