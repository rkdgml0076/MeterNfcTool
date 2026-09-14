package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.nfc.A101Command
import com.example.myapplication.nfc.A102Command
import com.example.myapplication.nfc.A103Command
import com.example.myapplication.nfc.A105Command
import com.example.myapplication.nfc.A107Command
import com.example.myapplication.nfc.A109Command
import com.example.myapplication.nfc.NfcWriteState
import com.example.myapplication.ui.theme.MyApplicationTheme

enum class CommandType(val title: String, val prefix: String) {
    NFC_START("NFC 모드 진입 (A101)", "A101"),
    NFC_EXIT("NFC 모드 종료 (A102)", "A102"),
    METER_NUMBER("계량기 번호 설정 (A103)", "A103"),
    METER_VALUE("검침 값 설정 (A105)", "A105"),
    REPORT_CYCLE("검침 주기 설정 (A107)", "A107"),
    DEV_RESET("단말기 재부팅 (A109)", "A109"),
    NFC_READ("NFC 태그 읽기", "READ"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterSettingScreen(
    integerPart: String,
    decimalPart: String,
    singleValue: String,
    writeState: NfcWriteState,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
    showConfirmDialog: Boolean,
    isNfcSessionActive: Boolean = false, // 👈 추가된 세션 상태
    onIntegerChange: (String) -> Unit,
    onDecimalChange: (String) -> Unit,
    onSingleValueChange: (String) -> Unit,
    onCommandTypeChange: (CommandType) -> Unit = {},
    onWriteClick: (commandType: CommandType, payload: String) -> Unit,
    onConfirmWrite: (commandType: CommandType, payload: String) -> Unit,
    onDismissConfirm: () -> Unit,
    onCancelWrite: () -> Unit = {},
    isReadSession: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var selectedTypeName by rememberSaveable { mutableStateOf(CommandType.NFC_START.name) }
    val selectedType = CommandType.valueOf(selectedTypeName)
    var expanded by remember { mutableStateOf(false) }
    val availableCommands = CommandType.entries

    val isValid = when (selectedType) {
        CommandType.NFC_START -> A101Command.isValid()
        CommandType.NFC_EXIT -> A102Command.isValid()
        CommandType.METER_NUMBER -> A103Command.isValid(integerPart, decimalPart)
        CommandType.METER_VALUE -> A105Command.isValid(singleValue)
        CommandType.REPORT_CYCLE -> A107Command.isValid(singleValue)
        CommandType.DEV_RESET -> A109Command.isValid()
        CommandType.NFC_READ -> true
    }

    val displayValue = when (selectedType) {
        CommandType.NFC_START -> A101Command.formatDisplay()
        CommandType.NFC_EXIT -> A102Command.formatDisplay()
        CommandType.METER_NUMBER -> if (isValid) A103Command.formatDisplay(integerPart, decimalPart) else "--"
        CommandType.METER_VALUE -> if (isValid) A105Command.formatDisplay(singleValue) else "--"
        CommandType.REPORT_CYCLE -> if (isValid) A107Command.formatDisplay(singleValue) else "--"
        CommandType.DEV_RESET -> A109Command.formatDisplay()
        CommandType.NFC_READ -> when {
            writeState is NfcWriteState.Success && writeState.isRead -> writeState.displayValue
            writeState is NfcWriteState.AwaitingTag ||
                (writeState is NfcWriteState.Writing && isReadSession) -> "대기 중"
            else -> "--"
        }
    }

    val command = when (selectedType) {
        CommandType.NFC_START -> A101Command.formatPayload()
        CommandType.NFC_EXIT -> A102Command.formatPayload()
        CommandType.METER_NUMBER -> if (isValid) A103Command.formatPayload(integerPart, decimalPart) else "A103--------"
        CommandType.METER_VALUE -> if (isValid) A105Command.formatPayload(singleValue) else "A105--------"
        CommandType.REPORT_CYCLE -> if (isValid) A107Command.formatPayload(singleValue) else "A107--"
        CommandType.DEV_RESET -> A109Command.formatPayload()
        CommandType.NFC_READ -> "READ"
    }

    val isReadCommand = selectedType == CommandType.NFC_READ
    val inputEnabled = writeState !is NfcWriteState.AwaitingTag && writeState !is NfcWriteState.Writing
    val canWrite = nfcAvailable && nfcEnabled && isValid && inputEnabled

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("계량기 NFC 설정") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = selectedType.title,
                    onValueChange = {},
                    label = { Text("설정 메뉴 선택") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableCommands.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.title) },
                            onClick = {
                                if (type != selectedType) {
                                    onCommandTypeChange(type)
                                }
                                selectedTypeName = type.name
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (!nfcAvailable) {
                StatusCard(
                    message = "이 기기는 NFC를 지원하지 않습니다.",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else if (!nfcEnabled) {
                StatusCard(
                    message = "NFC가 꺼져 있습니다. 설정에서 NFC를 켜주세요.",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            // 입력 UI 분기
            when (selectedType) {
                CommandType.METER_NUMBER -> {
                    Text(
                        text = "계량기 번호 입력",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = integerPart,
                            onValueChange = { value ->
                                if (value.length <= 2 && value.all { it.isDigit() }) {
                                    onIntegerChange(value)
                                }
                            },
                            enabled = inputEnabled,
                            label = { Text("검정 연도") },
                            placeholder = { Text("12") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )

                        Text(
                            text = "-",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = decimalPart,
                            onValueChange = { value ->
                                if (value.length <= 6 && value.all { it.isDigit() }) {
                                    onDecimalChange(value)
                                }
                            },
                            enabled = inputEnabled,
                            label = { Text("계량기 번호 6자리") },
                            placeholder = { Text("345678") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                CommandType.METER_VALUE -> {
                    Text(
                        text = "검침 값 입력 (8자리)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = singleValue,
                        onValueChange = { value ->
                            if (value.length <= 8 && value.all { it.isDigit() }) {
                                onSingleValueChange(value)
                            }
                        },
                        enabled = inputEnabled,
                        label = { Text("검침 값 (Ton 단위: 5자리 + 소수점 3자리)") },
                        placeholder = { Text("12345678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                CommandType.REPORT_CYCLE -> {
                    Text(
                        text = "전송 주기 입력 (1byte)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = singleValue,
                        onValueChange = { value ->
                            val next = value.uppercase()
                            if (next.length <= 2 && next.all { it.isDigit() || it in 'A'..'F' }) {
                                onSingleValueChange(next)
                            }
                        },
                        enabled = inputEnabled,
                        label = { Text("전송 주기 설정값 (예: 06 = 6시간 주기보고)") },
                        placeholder = { Text("06") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    )
                }
                CommandType.NFC_READ -> {
                    Text(
                        text = "태그에 저장된 값을 HEX로 읽습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CommandType.NFC_START,
                CommandType.DEV_RESET,
                CommandType.NFC_EXIT -> { /* 입력 필드 없음 */ }
            }

            // 미리보기 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isReadCommand) "읽기 결과" else "미리보기",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayValue,
                        style = if (isReadCommand && displayValue.contains(" ")) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.displaySmall
                        },
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isReadCommand && displayValue.contains(" ")) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            !isReadCommand -> "전송 명령: $command"
                            writeState is NfcWriteState.Success && writeState.isRead -> "HEX"
                            writeState is NfcWriteState.AwaitingTag -> "태그에 폰을 대주세요"
                            else -> "NFC Read 후 태그를 대면 여기에 표시됩니다"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // 전송 버튼
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canWrite,
                onClick = { onWriteClick(selectedType, command) },
            ) {
                Text(
                    text = when {
                        writeState is NfcWriteState.AwaitingTag -> "태그 대기 중..."
                        writeState is NfcWriteState.Writing && isReadSession -> "읽는 중..."
                        writeState is NfcWriteState.Writing -> "전송 중..."
                        isReadCommand -> "NFC Read"
                        else -> "NFC Write"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (writeState is NfcWriteState.AwaitingTag) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancelWrite,
                ) {
                    Text("태그 대기 취소")
                }
            }

            WriteStatusCard(
                writeState = writeState,
                isReadCommand = isReadCommand,
                isReadSession = isReadSession,
            )
        }
    }

    if (showConfirmDialog && isValid) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(selectedType.title) },
            text = {
                Text(
                    if (isReadCommand) {
                        "태그에 저장된 내용을 읽어오시겠습니까?"
                    } else {
                        "다음 명령을 전송하시겠습니까?\n\n전송 명령: $command"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirmWrite(selectedType, command) }) {
                    Text(if (isReadCommand) "NFC Read" else "NFC Write")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirm) { Text("취소") }
            },
        )
    }
}

@Composable
private fun StatusCard(message: String, containerColor: Color, contentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = message,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WriteStatusCard(
    writeState: NfcWriteState,
    isReadCommand: Boolean,
    isReadSession: Boolean,
) {
    val (message, containerColor, contentColor) = when (writeState) {
        NfcWriteState.Idle -> Triple(
            if (isReadCommand) {
                "NFC Read를 누른 뒤 태그에 폰을 대주세요."
            } else {
                "메뉴 선택 후 NFC Write를 눌러주세요."
            },
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NfcWriteState.AwaitingTag -> Triple(
            "단말기에 모바일을 인접해주세요. 15초 동안 대기합니다.",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        NfcWriteState.Writing -> Triple(
            if (isReadSession) "태그를 읽는 중입니다..." else "명령을 전송하는 중입니다...",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is NfcWriteState.Success -> Triple(
            if (writeState.isRead) {
                "읽기 완료: ${writeState.displayValue}"
            } else {
                "설정 완료: ${writeState.displayValue}"
            },
            Color(0xFFDFF6DD),
            Color(0xFF1E4620),
        )
        is NfcWriteState.Error -> Triple(
            writeState.message,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = message,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MeterSettingScreenPreview() {
    MyApplicationTheme {
        MeterSettingScreen(
            integerPart = "12",
            decimalPart = "345678",
            singleValue = "12345678",
            writeState = NfcWriteState.Idle,
            nfcAvailable = true,
            nfcEnabled = true,
            showConfirmDialog = false,
            isNfcSessionActive = false,
            onIntegerChange = {},
            onDecimalChange = {},
            onSingleValueChange = {},
            onWriteClick = { _, _ -> },
            onConfirmWrite = { _, _ -> },
            onDismissConfirm = {}
        )
    }
}