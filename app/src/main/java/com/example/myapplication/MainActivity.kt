package com.example.myapplication

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import com.example.myapplication.nfc.NfcReader
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
    private var pendingRead by mutableStateOf(false)
    private var awaitDeadlineElapsedRealtime: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val awaitTagTimeout = Runnable {
        if (writeState is NfcWriteState.AwaitingTag) {
            val retryLabel = if (pendingRead) "NFC Read" else "NFC Write"
            pendingCommand = null
            pendingDisplayValue = null
            pendingRead = false
            writeState = NfcWriteState.Error("태그 대기 시간이 초과되었습니다. 다시 $retryLabel 를 눌러주세요.")
            syncNfcReaderMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAvailable = nfcAdapter != null
        nfcEnabled = nfcAdapter?.isEnabled == true
        restoreWriteSession(savedInstanceState)

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
                            isReadSession = pendingRead,
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
                                if (commandType == CommandType.NFC_READ) {
                                    startRead()
                                } else {
                                    startWrite(
                                        commandType,
                                        payload,
                                        integerPart,
                                        decimalPart,
                                        singleValue,
                                    )
                                }
                            },
                            onDismissConfirm = {
                                showConfirmDialog = false
                                if (writeState !is NfcWriteState.AwaitingTag &&
                                    writeState !is NfcWriteState.Writing
                                ) {
                                    pendingCommand = null
                                    pendingDisplayValue = null
                                }
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

    override fun onDestroy() {
        mainHandler.removeCallbacks(awaitTagTimeout)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING_COMMAND, pendingCommand)
        outState.putString(KEY_PENDING_DISPLAY, pendingDisplayValue)
        outState.putBoolean(KEY_PENDING_READ, pendingRead)
        outState.putLong(KEY_AWAIT_DEADLINE, awaitDeadlineElapsedRealtime)
        when (val state = writeState) {
            NfcWriteState.Idle -> outState.putString(KEY_WRITE_STATE, STATE_IDLE)
            NfcWriteState.AwaitingTag -> outState.putString(KEY_WRITE_STATE, STATE_AWAITING)
            NfcWriteState.Writing -> outState.putString(KEY_WRITE_STATE, STATE_WRITING)
            is NfcWriteState.Success -> {
                outState.putString(KEY_WRITE_STATE, STATE_SUCCESS)
                outState.putString(KEY_WRITE_DETAIL, state.displayValue)
                outState.putBoolean(KEY_SUCCESS_IS_READ, state.isRead)
            }
            is NfcWriteState.Error -> {
                outState.putString(KEY_WRITE_STATE, STATE_ERROR)
                outState.putString(KEY_WRITE_DETAIL, state.message)
            }
        }
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
            CommandType.NFC_READ -> "태그 읽기"
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
        pendingRead = false
        writeState = NfcWriteState.AwaitingTag
        syncNfcReaderMode()
        armAwaitTagTimeout(AWAIT_TAG_TIMEOUT_MS)
    }

    private fun startRead() {
        pendingCommand = READ_SESSION_MARKER
        pendingDisplayValue = "태그 읽기"
        pendingRead = true
        writeState = NfcWriteState.AwaitingTag
        syncNfcReaderMode()
        armAwaitTagTimeout(AWAIT_TAG_TIMEOUT_MS)
    }

    private fun cancelWrite() {
        clearAwaitTagTimeout()
        pendingCommand = null
        pendingDisplayValue = null
        pendingRead = false
        if (writeState is NfcWriteState.AwaitingTag || writeState is NfcWriteState.Writing) {
            writeState = NfcWriteState.Idle
        }
        syncNfcReaderMode()
    }

    private fun restoreWriteSession(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        pendingCommand = savedInstanceState.getString(KEY_PENDING_COMMAND)
        pendingDisplayValue = savedInstanceState.getString(KEY_PENDING_DISPLAY)
        pendingRead = savedInstanceState.getBoolean(KEY_PENDING_READ, false)
        awaitDeadlineElapsedRealtime = savedInstanceState.getLong(KEY_AWAIT_DEADLINE, 0L)

        writeState = when (savedInstanceState.getString(KEY_WRITE_STATE)) {
            STATE_AWAITING, STATE_WRITING -> {
                if (pendingRead || !pendingCommand.isNullOrEmpty()) {
                    NfcWriteState.AwaitingTag
                } else {
                    NfcWriteState.Idle
                }
            }
            STATE_SUCCESS -> NfcWriteState.Success(
                displayValue = savedInstanceState.getString(KEY_WRITE_DETAIL).orEmpty(),
                isRead = savedInstanceState.getBoolean(KEY_SUCCESS_IS_READ, false),
            )
            STATE_ERROR -> NfcWriteState.Error(
                message = savedInstanceState.getString(KEY_WRITE_DETAIL)
                    ?: "NFC Write에 실패했습니다.",
            )
            else -> NfcWriteState.Idle
        }

        if (writeState is NfcWriteState.AwaitingTag) {
            resumeAwaitTagTimeout()
        }
    }

    private fun armAwaitTagTimeout(timeoutMs: Long) {
        awaitDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + timeoutMs
        resumeAwaitTagTimeout()
    }

    private fun resumeAwaitTagTimeout() {
        mainHandler.removeCallbacks(awaitTagTimeout)
        val remainingMs = awaitDeadlineElapsedRealtime - SystemClock.elapsedRealtime()
        if (remainingMs <= 0L) {
            awaitTagTimeout.run()
        } else {
            mainHandler.postDelayed(awaitTagTimeout, remainingMs)
        }
    }

    private fun clearAwaitTagTimeout() {
        mainHandler.removeCallbacks(awaitTagTimeout)
        awaitDeadlineElapsedRealtime = 0L
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
        if (pendingRead) {
            handleTagRead(tag)
            return
        }

        val command = pendingCommand ?: return

        runOnUiThread {
            clearAwaitTagTimeout()
            writeState = NfcWriteState.Writing
        }

        val result = NfcWriter.writeText(tag, command)
        val displayValue = pendingDisplayValue.orEmpty()

        runOnUiThread {
            clearAwaitTagTimeout()
            pendingCommand = null
            pendingDisplayValue = null
            pendingRead = false

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

    private fun handleTagRead(tag: Tag) {
        runOnUiThread {
            clearAwaitTagTimeout()
            writeState = NfcWriteState.Writing
        }

        val result = NfcReader.readText(tag)

        runOnUiThread {
            clearAwaitTagTimeout()
            pendingCommand = null
            pendingDisplayValue = null
            pendingRead = false

            writeState = if (result.isSuccess) {
                vibrateSuccess()
                NfcWriteState.Success(
                    displayValue = result.getOrDefault(""),
                    isRead = true,
                )
            } else {
                NfcWriteState.Error(
                    message = result.exceptionOrNull()?.message ?: "NFC Read에 실패했습니다.",
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

    companion object {
        private const val AWAIT_TAG_TIMEOUT_MS = 15_000L
        private const val KEY_PENDING_COMMAND = "pending_command"
        private const val KEY_PENDING_DISPLAY = "pending_display"
        private const val KEY_PENDING_READ = "pending_read"
        private const val KEY_WRITE_STATE = "write_state"
        private const val KEY_WRITE_DETAIL = "write_detail"
        private const val KEY_SUCCESS_IS_READ = "success_is_read"
        private const val KEY_AWAIT_DEADLINE = "await_deadline"
        private const val READ_SESSION_MARKER = "READ"
        private const val STATE_IDLE = "idle"
        private const val STATE_AWAITING = "awaiting"
        private const val STATE_WRITING = "writing"
        private const val STATE_SUCCESS = "success"
        private const val STATE_ERROR = "error"
    }
}
