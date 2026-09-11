package com.example.myapplication

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.nfc.A101Command
import com.example.myapplication.nfc.A102Command
import com.example.myapplication.nfc.A103Command
import com.example.myapplication.nfc.A105Command
import com.example.myapplication.nfc.A107Command
import com.example.myapplication.nfc.A109Command
import com.example.myapplication.nfc.NfcWriteState
import com.example.myapplication.nfc.NfcWriter
import com.example.myapplication.ui.BootSplashScreen
import com.example.myapplication.ui.CommandType
import com.example.myapplication.ui.MeterSettingScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null

    private var writeState by mutableStateOf<NfcWriteState>(NfcWriteState.Idle)
    private var nfcAvailable by mutableStateOf(false)
    private var nfcEnabled by mutableStateOf(false)

    private var pendingCommand: String? = null
    private var pendingDisplayValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAvailable = nfcAdapter != null
        nfcEnabled = nfcAdapter?.isEnabled == true

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var integerPart by rememberSaveable { mutableStateOf("") }
                var decimalPart by rememberSaveable { mutableStateOf("") }
                var singleValue by rememberSaveable { mutableStateOf("") }
                var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

                when {
                    showSplash -> {
                        BootSplashScreen(
                            modifier = Modifier.fillMaxSize(),
                            onFinished = { showSplash = false },
                        )
                    }
                    !isLoggedIn -> {
                        LoginScreen(
                            onLoginSuccess = { isLoggedIn = true },
                        )
                    }
                    else -> {
                        MeterSettingScreen(
                            modifier = Modifier.fillMaxSize(),
                            integerPart = integerPart,
                            decimalPart = decimalPart,
                            singleValue = singleValue,
                            writeState = writeState,
                            nfcAvailable = nfcAvailable,
                            nfcEnabled = nfcEnabled,
                            showConfirmDialog = showConfirmDialog,
                            onIntegerChange = { value ->
                                integerPart = value
                                resetResultStateIfNeeded()
                            },
                            onDecimalChange = { value ->
                                decimalPart = value
                                resetResultStateIfNeeded()
                            },
                            onSingleValueChange = { value ->
                                singleValue = value
                                resetResultStateIfNeeded()
                            },
                            onCommandTypeChange = {
                                singleValue = ""
                                showConfirmDialog = false
                                cancelWrite()
                                resetResultStateIfNeeded()
                            },
                            onWriteClick = { commandType, payload ->
                                preparePendingData(
                                    commandType,
                                    payload,
                                    integerPart,
                                    decimalPart,
                                    singleValue,
                                )
                                showConfirmDialog = true
                            },
                            onConfirmWrite = { commandType, payload ->
                                showConfirmDialog = false
                                startWrite(
                                    commandType,
                                    payload,
                                    integerPart,
                                    decimalPart,
                                    singleValue,
                                )
                            },
                            onDismissConfirm = {
                                showConfirmDialog = false
                                pendingCommand = null
                                pendingDisplayValue = null
                            },
                            onCancelWrite = ::cancelWrite,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcEnabled = nfcAdapter?.isEnabled == true
        syncNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun resetResultStateIfNeeded() {
        if (writeState is NfcWriteState.Success || writeState is NfcWriteState.Error) {
            writeState = NfcWriteState.Idle
        }
    }

    private fun preparePendingData(
        commandType: CommandType,
        payload: String,
        integerPart: String,
        decimalPart: String,
        singleValue: String,
    ) {
        pendingCommand = payload
        pendingDisplayValue = when (commandType) {
            CommandType.NFC_START -> A101Command.formatDisplay()
            CommandType.NFC_EXIT -> A102Command.formatDisplay()
            CommandType.METER_NUMBER -> A103Command.formatDisplay(integerPart, decimalPart)
            CommandType.METER_VALUE -> A105Command.formatDisplay(singleValue)
            CommandType.REPORT_CYCLE -> A107Command.formatDisplay(singleValue)
            CommandType.DEV_RESET -> A109Command.formatDisplay()
        }
    }

    private fun startWrite(
        commandType: CommandType,
        payload: String,
        integerPart: String,
        decimalPart: String,
        singleValue: String,
    ) {
        preparePendingData(commandType, payload, integerPart, decimalPart, singleValue)
        writeState = NfcWriteState.AwaitingTag
        syncNfcReaderMode()
    }

    private fun cancelWrite() {
        pendingCommand = null
        pendingDisplayValue = null
        if (writeState is NfcWriteState.AwaitingTag || writeState is NfcWriteState.Writing) {
            writeState = NfcWriteState.Idle
        }
        syncNfcReaderMode()
    }

    private fun syncNfcReaderMode() {
        if (writeState is NfcWriteState.AwaitingTag) {
            enableNfcReaderMode()
        } else if (writeState !is NfcWriteState.Writing) {
            nfcAdapter?.disableReaderMode(this)
        }
    }

    private fun enableNfcReaderMode() {
        nfcAdapter?.enableReaderMode(
            this,
            { tag -> handleTagDiscovered(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    private fun handleTagDiscovered(tag: Tag) {
        val command = pendingCommand ?: return

        runOnUiThread {
            writeState = NfcWriteState.Writing
        }

        val result = NfcWriter.writeText(tag, command)
        val displayValue = pendingDisplayValue.orEmpty()

        runOnUiThread {
            pendingCommand = null
            pendingDisplayValue = null

            writeState = if (result.isSuccess) {
                vibrateSuccess()
                NfcWriteState.Success(displayValue = displayValue)
            } else {
                NfcWriteState.Error(
                    message = result.exceptionOrNull()?.message ?: "NFC Write에 실패했습니다.",
                )
            }
            syncNfcReaderMode()
        }
    }

    private fun vibrateSuccess() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    200,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
