package com.example.myapplication.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

object NfcWriter {
    fun writeText(tag: Tag, text: String): Result<Unit> {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return writeNdef(ndef, text)
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return formatAndWrite(formatable, text)
        }

        return Result.failure(IllegalStateException("NDEF를 지원하지 않는 태그입니다."))
    }

    private fun writeNdef(ndef: Ndef, text: String): Result<Unit> {
        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                return Result.failure(IllegalStateException("태그가 쓰기 보호되어 있습니다."))
            }

            val message = createTextMessage(text)
            ndef.writeNdefMessage(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun formatAndWrite(formatable: NdefFormatable, text: String): Result<Unit> {
        return try {
            formatable.connect()
            val message = createTextMessage(text)
            formatable.format(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { formatable.close() }
        }
    }

    private fun createTextMessage(text: String): NdefMessage {
        val record = NdefRecord.createTextRecord("en", text)
        return NdefMessage(arrayOf(record))
    }
}
