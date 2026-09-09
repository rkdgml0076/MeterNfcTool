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

    private var integerPart by mutableStateOf("")
    private var decimalPart by mutableStateOf("")
    private var singleValue by mutableStateOf("")
    private var writeState by mutableStateOf<NfcWriteState>(NfcWriteState.Idle)
    private var nfcAvailable by mutableStateOf(false)
    private var nfcEnabled by mutableStateOf(false)
    private var showConfirmDialog by mutableStateOf(false)

    private var isLoggedIn by mutableStateOf(false)
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

                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                        },
                    )
                } else if (showSplash) {
                    BootSplashScreen(
                        modifier = Modifier.fillMaxSize(),
                        onFinished = { showSplash = false },
                    )
                } else {
                    MeterSettingScreen(
                        modifier = Modifier.fillMaxSize(),
                        integerPart = integerPart,
                        decimalPart = decimalPart,
                        singleValue = singleValue,
                        writeState = writeState,
                        nfcAvailable = nfcAvailable,
                        nfcEnabled = nfcEnabled,
                        showConfirmDialog = showConfirmDialog,
                        onIntegerChange = ::onIntegerChanged,
                        onDecimalChange = ::onDecimalChanged,
                        onSingleValueChange = ::onSingleValueChanged,
                        onCommandTypeChange = ::onCommandTypeChanged,
                        onWriteClick = { commandType, payload ->
                            preparePendingData(commandType, payload)
                            showConfirmDialog = true
                        },
                        onConfirmWrite = { commandType, payload ->
                            startWrite(commandType, payload)
                        },
                        onDismissConfirm = { showConfirmDialog = false },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcEnabled = nfcAdapter?.isEnabled == true
        enableNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun onIntegerChanged(value: String) {
        integerPart = value
        resetResultStateIfNeeded()
    }

    private fun onDecimalChanged(value: String) {
        decimalPart = value
        resetResultStateIfNeeded()
    }

    private fun onSingleValueChanged(value: String) {
        singleValue = value
        resetResultStateIfNeeded()
    }

    private fun onCommandTypeChanged(@Suppress("UNUSED_PARAMETER") commandType: CommandType) {
        // A105와 A107이 같은 입력 상태를 공유하므로, 메뉴 전환 시 이전 값을 비운다.
        singleValue = ""
        showConfirmDialog = false
        pendingCommand = null
        pendingDisplayValue = null
        resetResultStateIfNeeded()
    }

    private fun resetResultStateIfNeeded() {
        if (writeState is NfcWriteState.Success || writeState is NfcWriteState.Error) {
            writeState = NfcWriteState.Idle
        }
    }

    private fun preparePendingData(commandType: CommandType, payload: String) {
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

    private fun startWrite(commandType: CommandType, payload: String) {
        showConfirmDialog = false
        preparePendingData(commandType, payload)
        writeState = NfcWriteState.AwaitingTag
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