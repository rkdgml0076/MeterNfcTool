### 2026-09-09 GitHub Commit
#### 본 어플리케이션을 개발하기 위한 기본 작업 환경과 코드 프레임
## 작업 환경 설정
- 개발 환경(Code Editer) Android Studio 사용 <br>
- MyApplication 기본 양식대로 Project 생성
- Github 와 연동 및 git 활용을 위하여 git Download
  Android Studio는 기본적으로 Git과 GitHub가 Plugins 되어있음
- 코드 결과물 확인은 Clean and Assemble Project with Tests로 매번 빌드하여 확인중
별도의 동작 결과 방법을 찾을 시 추후 기술
- APK 파일 경로: 사용자디렉토리\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk


## Loging
<br>
메인 화면 접속전 로그인 기능추가

### LogingScreen(kt)
```kt
package com.example.myapplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
) {
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "NFC Meter Tool",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "서비스 이용을 위해 로그인해주세요.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = id,
                onValueChange = {
                    id = it
                    loginError = false
                },
                label = { Text("아이디") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    loginError = false
                },
                label = { Text("비밀번호") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if ((id == "admin" && password == "admin") || (id == "ntmore" && password == "ntmore09")) {
                        onLoginSuccess()
                    } else {
                        loginError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (loginError) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "아이디 또는 비밀번호가 틀렸습니다.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
        }
    }
}
```

### A101Command(kt)
```kt
package com.example.myapplication.nfc

object A101Command {
    const val PREFIX = "A101"
    fun formatDisplay(): String = "NFC 모드 진입"
    fun formatPayload(): String = PREFIX
    fun isValid(): Boolean = true
}
```

### A102Command(kt)
```kt
package com.example.myapplication.nfc

object A102Command {
    const val PREFIX = "A102"
    fun formatDisplay(): String = "NFC 모드 종료"
    fun formatPayload(): String = PREFIX
    fun isValid(): Boolean = true
}
```

### A107Command(kt)
```kt
package com.example.myapplication.nfc

object A107Command {
    const val PREFIX = "A107"

    fun formatDisplay(value: String): String {
        val intValue = value.toIntOrNull() ?: value.toIntOrNull(16) ?: return value
        return "${intValue}시간"
    }

    fun formatPayload(value: String): String = "$PREFIX${formatByte(value)}"

    fun isValid(value: String): Boolean {
        if (value.isEmpty()) return false
        if (value.length > 2) return false
        return value.all { it.isHexDigit() }
    }

    // 1byte = ASCII 2글자 (예: "6" -> "06", "0A" -> "0A")
    private fun formatByte(value: String): String {
        return value.uppercase().padStart(2, '0').takeLast(2)
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
```

### A109Command(kt)
```kt
package com.example.myapplication.nfc

object A109Command {
    const val PREFIX = "A109"
    fun formatDisplay(): String = "단말기 재부팅"
    fun formatPayload(): String = PREFIX
    fun isValid(): Boolean = true
}
```

### BootSplashScreen(kt)
```kt
enum class CommandType(val title: String, val prefix: String) {
    NFC_START("NFC 모드 진입 (A101)", "A101"),
    NFC_EXIT("NFC 모드 종료 (A102)", "A102"),
    METER_NUMBER("계량기 번호 설정 (A103)", "A103"),
    METER_VALUE("검침 값 설정 (A105)", "A105"),
    REPORT_CYCLE("검침 주기 설정 (A107)", "A107"),
    DEV_RESET("단말기 재부팅 (A109)", "A109"),
}

 var selectedType by remember { mutableStateOf(CommandType.NFC_START) }

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
CommandType.NFC_START,
CommandType.DEV_RESET,
```


<br>

### 진행 내용
**icon 변경 및 로그인 페이지 추가**
1. 로그인 페이지 추가
 - ID/PW는 DB 없이 하드 코딩하여 계정 생성
--Image 참고-- <br>
<img width="648" height="1404" alt="Image" src="https://github.com/user-attachments/assets/30216187-48cc-46ad-98da-cc4467bbe575" /><br>

2. 엔티모아 어플리케이션 아이콘 변경
 - 아이콘 어셉션으로 아이콘 이미지 지정 후 전체 적용
--Image 참고-- <br>
<img width="1254" height="1254" alt="Image" src="https://github.com/user-attachments/assets/c111b7ca-281c-47ef-8b1a-ebd20e37a92f" /><br>

3. 검침주기 양식 일부 변경
 - 검침주기 => 전송주기
 - 06입력 시, 6시간으로 출력
4. 미리보기 하단 텍스트에서 0xA1 관련 Hex 텍스트 삭제(A101, A102, A109 반영)
5. A109: 단말 RESET 텍스트 => 단말기 재부팅 일괄 변경(미리보기 하단, 메뉴 선택)
--Image 참고-- <br>
<img width="648" height="1404" alt="Image" src="https://github.com/user-attachments/assets/ca760b31-7aac-4d57-b0a8-52028e92f2f4" /><br>

6. 어플 접속 메인 이미지 글로벌성장사다리 슬로건 피드백 받아 삭제처리
--Image 참고-- <br>
<img width="768" height="1376" alt="Image" src="https://github.com/user-attachments/assets/1c4e9236-4e3e-459d-b7ae-2a6d48a12bf4" />
<br>
---

### 2026-09-08 GitHub Commit
#### 본 어플리케이션을 개발하기 위한 기본 작업 환경과 코드 프레임
## 작업 환경 설정
- 개발 환경(Code Editer) Android Studio 사용 <br>
- MyApplication 기본 양식대로 Project 생성
- Github 와 연동 및 git 활용을 위하여 git Download
  Android Studio는 기본적으로 Git과 GitHub가 Plugins 되어있음
- 코드 결과물 확인은 Clean and Assemble Project with Tests로 매번 빌드하여 확인중
별도의 동작 결과 방법을 찾을 시 추후 기술
- APK 파일 경로: 사용자디렉토리\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk


## BugFix
<br>
A105, A107 메뉴 변경 시에도 입력 값 유지되던 버그 FIX

### MeterSettingScreen(kt)
```kt
onCommandTypeChange: (CommandType) -> Unit = {},

if (type != selectedType) {
    onCommandTypeChange(type)
}
```

### MeterSettingViewModel(kt)
```kt
fun onCommandTypeChange(@Suppress("UNUSED_PARAMETER") commandType: CommandType) {
    _uiState.update {
        it.copy(
            singleValue = "",
            showConfirmDialog = false,
            pendingCommandType = null,
            pendingPayload = "",
        )
    }
}
```

### MainActivity(kt)
```kt
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.myapplication.ui.BootSplashScreen

var showSplash by rememberSaveable { mutableStateOf(true) }

if (showSplash) {
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

private fun onCommandTypeChanged(@Suppress("UNUSED_PARAMETER") commandType: CommandType) {
    // A105와 A107이 같은 입력 상태를 공유하므로, 메뉴 전환 시 이전 값을 비운다.
    singleValue = ""
    showConfirmDialog = false
    pendingCommand = null
    pendingDisplayValue = null
    resetResultStateIfNeeded()
}
```

### BootSplashScreen(kt)
```kt
package com.example.myapplication.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R
import kotlinx.coroutines.delay

private val SplashBackground = Color(0xFFF7F7F7)
private const val SplashDurationMs = 2_500L
private const val SplashFadeMs = 200

@Composable
fun BootSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = SplashFadeMs),
        label = "bootSplashAlpha",
    )

    LaunchedEffect(Unit) {
        delay(SplashDurationMs)
        visible = false
        delay(SplashFadeMs.toLong())
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.boot_img),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
```

### themes(xml)
```xml
    <style name="Theme.MyApplication" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@drawable/boot_img</item>
    </style>
```

<br>

### 진행 내용
**Test Box Insert Bug Fix**
1. A105(검침 값)와 A107(검침 주기)이 같은 입력값(singleValue)을 쓰고, 메뉴만 바꿔도 그 값을 비우지 않았음
 - 설정 메뉴를 다른 항목으로 바꾸면 공유 입력을 비우도록 변경
 - 설정 메뉴를 다른 항목으로 바꾸면 확인 다이얼로그와 대기 중인 NFC 명령도 취소
2. 어플 접속 메인 이미지 추가
--Image 참고-- <br>
<img width="768" height="1376" alt="Image" src="https://github.com/user-attachments/assets/e394ee34-7b6d-49c2-a9a6-8f6b3f5d4a5c" />
<br>

---

### 2026-09-07 GitHub Commit
#### 본 어플리케이션을 개발하기 위한 기본 작업 환경과 코드 프레임
## 작업 환경 설정
- 개발 환경(Code Editer) Android Studio 사용 <br>
- MyApplication 기본 양식대로 Project 생성
- Github 와 연동 및 git 활용을 위하여 git Download
  Android Studio는 기본적으로 Git과 GitHub가 Plugins 되어있음
- 코드 결과물 확인은 Clean and Assemble Project with Tests로 매번 빌드하여 확인중
별도의 동작 결과 방법을 찾을 시 추후 기술
- APK 파일 경로: 사용자디렉토리\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk


## 단말 주기보고와 리셋 NFC Write 구현
<br>

A107, A109 Write 기능 구현
### A107Command(kt)
```kt
package com.example.myapplication.nfc

object A107Command {
    const val PREFIX = "A107"

    fun formatDisplay(value: String): String = formatByte(value)

    fun formatPayload(value: String): String = "$PREFIX${formatByte(value)}"

    fun isValid(value: String): Boolean {
        if (value.isEmpty()) return false
        if (value.length > 2) return false
        return value.all { it.isHexDigit() }
    }

    // 1byte = ASCII 2글자 (예: "6" -> "06", "0A" -> "0A")
    private fun formatByte(value: String): String {
        return value.uppercase().padStart(2, '0').takeLast(2)
    }

    private fun Char.isHexDigit(): Boolean {
        return this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
```

### A107Command(kt)
```kt
package com.example.myapplication.nfc

object A109Command {
    const val PREFIX = "A109"

    fun formatDisplay(): String = "A109 (단말 RESET)"

    fun formatPayload(): String = PREFIX

    fun isValid(): Boolean = true
}
```

### MainActivity(kt)
```kt
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
```

### MeterSettingScreen(kt)
```kt
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    DEV_RESET("단말 RESET (A109)", "A109"),
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
    onWriteClick: (commandType: CommandType, payload: String) -> Unit,
    onConfirmWrite: (commandType: CommandType, payload: String) -> Unit,
    onDismissConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(CommandType.METER_NUMBER) }
    var expanded by remember { mutableStateOf(false) }
    val availableCommands = CommandType.entries

    val isValid = when (selectedType) {
        CommandType.NFC_START -> A101Command.isValid()
        CommandType.NFC_EXIT -> A102Command.isValid()
        CommandType.METER_NUMBER -> A103Command.isValid(integerPart, decimalPart)
        CommandType.METER_VALUE -> A105Command.isValid(singleValue)
        CommandType.REPORT_CYCLE -> A107Command.isValid(singleValue)
        CommandType.DEV_RESET -> A109Command.isValid()
    }

    val displayValue = when (selectedType) {
        CommandType.NFC_START -> A101Command.formatDisplay()
        CommandType.NFC_EXIT -> A102Command.formatDisplay()
        CommandType.METER_NUMBER -> if (isValid) A103Command.formatDisplay(integerPart, decimalPart) else "--"
        CommandType.METER_VALUE -> if (isValid) A105Command.formatDisplay(singleValue) else "--"
        CommandType.REPORT_CYCLE -> if (isValid) A107Command.formatDisplay(singleValue) else "--"
        CommandType.DEV_RESET -> A109Command.formatDisplay()
    }

    val command = when (selectedType) {
        CommandType.NFC_START -> A101Command.formatPayload()
        CommandType.NFC_EXIT -> A102Command.formatPayload()
        CommandType.METER_NUMBER -> if (isValid) A103Command.formatPayload(integerPart, decimalPart) else "A103--------"
        CommandType.METER_VALUE -> if (isValid) A105Command.formatPayload(singleValue) else "A105--------"
        CommandType.REPORT_CYCLE -> if (isValid) A107Command.formatPayload(singleValue) else "A107--"
        CommandType.DEV_RESET -> A109Command.formatPayload()
    }

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
                                selectedType = type
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
                        text = "검침 주기 입력 (1byte)",
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
                CommandType.DEV_RESET,
                CommandType.NFC_START,
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
                        text = "미리보기",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "전송 명령: $command",
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
                    text = when (writeState) {
                        NfcWriteState.AwaitingTag -> "태그 대기 중..."
                        NfcWriteState.Writing -> "전송 중..."
                        else -> "NFC Write"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            WriteStatusCard(writeState = writeState)
        }
    }

    if (showConfirmDialog && isValid) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(selectedType.title) },
            text = {
                Text("다음 명령을 전송하시겠습니까?\n\n전송 명령: $command")
            },
            confirmButton = {
                TextButton(onClick = { onConfirmWrite(selectedType, command) }) { Text("NFC Write") }
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
private fun WriteStatusCard(writeState: NfcWriteState) {
    val (message, containerColor, contentColor) = when (writeState) {
        NfcWriteState.Idle -> Triple(
            "메뉴 선택 후 NFC Write를 눌러주세요.",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NfcWriteState.AwaitingTag -> Triple(
            "계량기에 폰을 대주세요.",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        NfcWriteState.Writing -> Triple(
            "명령을 전송하는 중입니다...",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is NfcWriteState.Success -> Triple(
            "설정 완료: ${writeState.displayValue}",
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
```

### NfcWriteState(kt)
```kt
package com.example.myapplication.nfc

sealed interface NfcWriteState {
    object Idle : NfcWriteState
    object AwaitingTag : NfcWriteState
    object Writing : NfcWriteState
    data class Success(val displayValue: String) : NfcWriteState // 👈 String 인자 필요
    data class Error(val message: String) : NfcWriteState
}
```

<br>

### 진행 내용
**A107, A109 NFC Write 내용 적용**
1. 단말 전송 주기 Write는 하위 4bit가 전송 주기로 설정되어있어 앞에 상위 4bit는 0으로 채워 넣어서 기입해야함
 - 예) A107(기본) + 상위 4bit: 0 + 하위 4bit: 6(6시간 전송 주기)
2. 단말 RESET은 Write 기능상 구현되어 있으나, 정확한 기능 확인을 위하여 MCU 코드 확인 필요
3. 기존 Write하였던 텍스트 박스가 다른 메뉴 선택시에도 유지되는 버그 확인: 추후 Fix 예정
--Image 참고-- <br>
<img width="648" height="1404" alt="Image" src="https://github.com/user-attachments/assets/b5c2691f-eb0f-40bc-af52-acf2aa23e49d" /><br>
<br>

---

### 2026-08-11 GitHub First Commit
#### 본 어플리케이션을 개발하기 위한 기본 작업 환경과 코드 프레임
## 작업 환경 설정
- 개발 환경(Code Editer) Android Studio 사용 <br>
- MyApplication 기본 양식대로 Project 생성
- Github 와 연동 및 git 활용을 위하여 git Download
  Android Studio는 기본적으로 Git과 GitHub가 Plugins 되어있음
- 코드 결과물 확인은 Clean and Assemble Project with Tests로 매번 빌드하여 확인중
별도의 동작 결과 방법을 찾을 시 추후 기술
- APK 파일 경로: 사용자디렉토리\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk


## NFC 어플리케이션 Write 기능 구현
<br>

A101, A102, A103, A105 Write 기능 초기 구현
### A101Command(kt)
```kt
package com.example.myapplication.nfc

object A101Command {
  const val PREFIX = "A101"
  fun formatDisplay(): String = "A101 (NFC 모드 진입)"
  fun formatPayload(): String = PREFIX
  fun isValid(): Boolean = true
}
```

### A102Command(kt)
```kt
package com.example.myapplication.nfc

object A102Command {
  const val PREFIX = "A102"
  fun formatDisplay(): String = "A102 (NFC 모드 종료)"
  fun formatPayload(): String = PREFIX
  fun isValid(): Boolean = true
}
```

### A103Command(kt)
```kt
package com.example.myapplication.nfc

object A103Command {
  const val PREFIX = "A103"

  fun formatDisplay(integerPart: String, decimalPart: String): String {
    val integer = integerPart.padStart(2, '0').takeLast(2)
    val decimal = decimalPart.padStart(6, '0').takeLast(6)
    return "$integer-$decimal"
  }

  fun formatPayload(integerPart: String, decimalPart: String): String {
    val integer = integerPart.padStart(2, '0').takeLast(2)
    val decimal = decimalPart.padStart(6, '0').takeLast(6)
    return "$PREFIX$integer$decimal"
  }

  fun isValid(integerPart: String, decimalPart: String): Boolean {
    if (integerPart.isEmpty() || decimalPart.isEmpty()) return false
    if (!integerPart.all { it.isDigit() } || integerPart.length > 2) return false
    if (!decimalPart.all { it.isDigit() } || decimalPart.length > 6) return false
    return true
  }
}

```

### A105Command(kt)
```kt
package com.example.myapplication.nfc

object A105Command {
  const val PREFIX = "A105"
  fun formatDisplay(value: String): String {
    val padded = value.padStart(8, '0').takeLast(8)
    val integerPart = padded.substring(0, 5)
    val decimalPart = padded.substring(5)
    return "$integerPart.$decimalPart"
  }
  fun formatPayload(value: String): String {
    val padded = value.padStart(8, '0').takeLast(8)
    return "$PREFIX$padded"
  }

  fun isValid(value: String): Boolean {
    if (value.isEmpty()) return false
    if (!value.all { it.isDigit() }) return false
    if (value.length > 8) return false
    return true
  }
}
```

### NfcWriteState(kt)
```kt
package com.example.myapplication.nfc

sealed interface NfcWriteState {
  data object Idle : NfcWriteState

  data object AwaitingTag : NfcWriteState

  data object Writing : NfcWriteState

  data class Success(
    val displayValue: String,
    val command: String,
  ) : NfcWriteState

  data class Error(val message: String) : NfcWriteState
}
```

### NfcWrite(kt)
```kt
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
```

### MainActivity(kt)
```kt
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.nfc.A101Command
import com.example.myapplication.nfc.A102Command
import com.example.myapplication.nfc.A103Command
import com.example.myapplication.nfc.A105Command
import com.example.myapplication.nfc.NfcWriteState
import com.example.myapplication.nfc.NfcWriter
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
        NfcWriteState.Success(
          displayValue = displayValue,
          command = command,
        )
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
```

### MeterSettingScreen(kt)
```kt
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.nfc.A101Command
import com.example.myapplication.nfc.A102Command
import com.example.myapplication.nfc.A103Command
import com.example.myapplication.nfc.A105Command
import com.example.myapplication.nfc.NfcWriteState
import com.example.myapplication.ui.theme.MyApplicationTheme

enum class CommandType(val title: String, val prefix: String) {
  NFC_START("NFC 모드 진입 (A101)", "A101"),
  NFC_EXIT("NFC 모드 종료 (A102)", "A102"),
  METER_NUMBER("계량기 번호 설정 (A103)", "A103"),
  METER_VALUE("검침 값 설정 (A105)", "A105")
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
  onIntegerChange: (String) -> Unit,
  onDecimalChange: (String) -> Unit,
  onSingleValueChange: (String) -> Unit,
  onWriteClick: (commandType: CommandType, payload: String) -> Unit,
  onConfirmWrite: (commandType: CommandType, payload: String) -> Unit,
  onDismissConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedType by remember { mutableStateOf(CommandType.METER_NUMBER) }
  var expanded by remember { mutableStateOf(false) }

  val isValid = when (selectedType) {
    CommandType.NFC_START -> A101Command.isValid()
    CommandType.NFC_EXIT -> A102Command.isValid()
    CommandType.METER_NUMBER -> A103Command.isValid(integerPart, decimalPart)
    CommandType.METER_VALUE -> A105Command.isValid(singleValue)
  }

  val displayValue = when (selectedType) {
    CommandType.NFC_START -> A101Command.formatDisplay()
    CommandType.NFC_EXIT -> A102Command.formatDisplay()
    CommandType.METER_NUMBER -> if (isValid) A103Command.formatDisplay(integerPart, decimalPart) else "--"
    CommandType.METER_VALUE -> if (isValid) A105Command.formatDisplay(singleValue) else "--"
  }

  val command = when (selectedType) {
    CommandType.NFC_START -> A101Command.formatPayload()
    CommandType.NFC_EXIT -> A102Command.formatPayload()
    CommandType.METER_NUMBER -> if (isValid) A103Command.formatPayload(integerPart, decimalPart) else "A103--------"
    CommandType.METER_VALUE -> if (isValid) A105Command.formatPayload(singleValue) else "A105--------"
  }

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
          CommandType.values().forEach { type ->
            DropdownMenuItem(
              text = { Text(type.title) },
              onClick = {
                selectedType = type
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

      // 2. 입력 UI 분기
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
        CommandType.NFC_START -> { /* 입력 필드 없음 */ }
        CommandType.NFC_EXIT -> { /* 입력 필드 없음 */ }
      }

      // 3. 미리보기
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
            text = "미리보기",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = displayValue,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "전송 명령: $command",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
          )
        }
      }

      // 4. 전송 버튼
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        enabled = canWrite,
        onClick = { onWriteClick(selectedType, command) },
      ) {
        Text(
          text = when (writeState) {
            NfcWriteState.AwaitingTag -> "태그 대기 중..."
            NfcWriteState.Writing -> "전송 중..."
            else -> "NFC Write"
          },
          style = MaterialTheme.typography.titleMedium,
        )
      }

      WriteStatusCard(writeState = writeState)
    }
  }

  if (showConfirmDialog && isValid) {
    AlertDialog(
      onDismissRequest = onDismissConfirm,
      title = { Text(selectedType.title) },
      text = {
        Text("다음 명령을 전송하시겠습니까?\n\n전송 명령: $command")
      },
      confirmButton = {
        TextButton(onClick = { onConfirmWrite(selectedType, command) }) { Text("NFC Write") }
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
private fun WriteStatusCard(writeState: NfcWriteState) {
  val (message, containerColor, contentColor) = when (writeState) {
    NfcWriteState.Idle -> Triple(
      "메뉴 선택 후 NFC Write를 눌러주세요.",
      MaterialTheme.colorScheme.surfaceVariant,
      MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NfcWriteState.AwaitingTag -> Triple(
      "계량기에 폰을 대주세요.",
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
    )
    NfcWriteState.Writing -> Triple(
      "명령을 전송하는 중입니다...",
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
    )
    is NfcWriteState.Success -> Triple(
      "설정 완료: ${writeState.displayValue}",
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
      onIntegerChange = {},
      onDecimalChange = {},
      onSingleValueChange = {},
      onWriteClick = { _, _ -> },
      onConfirmWrite = { _, _ -> },
      onDismissConfirm = {}
    )
  }
}
```

### AndroidManifest(xml)
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">
  <uses-permission android:name="android.permission.NFC"/>
  <uses-permission android:name="android.permission.VIBRATE"/>
  <uses-feature
          android:name="android.hardware.nfc"
          android:required="true"/>

  <application
          android:allowBackup="true"
          android:dataExtractionRules="@xml/data_extraction_rules"
          android:fullBackupContent="@xml/backup_rules"
          android:icon="@mipmap/ic_launcher"
          android:label="@string/app_name"
          android:roundIcon="@mipmap/ic_launcher_round"
          android:supportsRtl="true"
          android:theme="@style/Theme.MyApplication">
    <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MyApplication">

      <!-- 앱 실행 -->
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>

        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>

      <!-- NFC -->
      <intent-filter>
        <action android:name="android.nfc.action.NDEF_DISCOVERED"/>

        <category android:name="android.intent.category.DEFAULT"/>

        <data android:mimeType="text/plain"/>
      </intent-filter>

    </activity>
  </application>

</manifest>
```

<br>

### 진행 내용
**NFC 어플리케이션 기본 프레임 완성**
1. 어플리케이션 다운로드는 apk파일을 zip파일로 변환 후 안드로이드로 파일 전송 후 다운로드 및 업데이트 진행 
2. 입력 하고싶은 NFC 메뉴 설정은 설정 메뉴 선택에서 변경 가능
- 기본 값은 계량기 번호 입력란인 A103으로 설정
3. A101 NFC모드로 진입하는 명령
- 입력 텍스트 없이 메뉴 선택 후 바로 Write
4. A102 NFC모드에서 시나리오 모드로 변경하는 명령
- 입력 텍스트 없이 메뉴 선택 후 바로 Write
5. A103 계량기 번호를 Write 하기위한 입력란
- UI 편의상 향상을 위하여 검정 연도와 계량기 번호 6자리 12-345678 양식으로 구현
- 검정월 앞에 2자리 입력 후 자동적으로 계량기 번호 뒷자리 부터 입력되도록 변경
- 전송되는 명령은 "전송 명령: A103 계량기번호 8자리" 메인화면에서 확인 가능
6. A105 검침값을 Write 하기위한 입력란
- 소수점 ton 단위 입력은 계량기 구경 및 지자체마다 기준값이 상이하여, 편의상 계량기번호 8자리 양식으로 구현
- 계량기 번호와 마찬가지로 "전송 명령: A105 검침값 8자리" 전송명령 메인화면에 확인 가능
7. Write 진행 시 NFC Write 버튼을 터치 후 Write 팝업 생성
- 팝업에서 마지막으로 전송 명령 확인 후 NFC Write 클릭
- NFC 태그 대기 상태로 변경
- 상기 내용은 어플 최하단 가이드 텍스트로 작성

### 해당 프로젝트 최초 커밋이며, 미흡한 사항 변경 및 추가 기술될 예정
--Image 참고-- <br>
<img width="501" height="857" alt="Image" src="https://github.com/user-attachments/assets/62f26afe-0470-41ff-b0b4-d60d6be938c0" /><br>
<br>

---