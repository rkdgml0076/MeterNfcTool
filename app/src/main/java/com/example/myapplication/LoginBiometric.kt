package com.example.myapplication

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object LoginBiometric {
    private const val PREFS = "login_prefs"
    private const val KEY_ENABLED = "fingerprint_login_enabled"
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** 이 기기에 지문 센서가 있고, 등록된 지문이 있을 때 true. */
    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val showPrompt = Runnable {
            try {
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            runCatching { onSuccess() }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                errorCode == BiometricPrompt.ERROR_CANCELED
                            ) {
                                return
                            }
                            runCatching { onError(errString.toString()) }
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("지문으로 로그인")
                    .setSubtitle("이 기기에 등록된 지문으로 로그인합니다.")
                    .setNegativeButtonText("취소")
                    .setAllowedAuthenticators(AUTHENTICATORS)
                    .build()
                prompt.authenticate(info)
            } catch (e: Exception) {
                runCatching {
                    onError(e.message?.takeIf { it.isNotBlank() } ?: "지문 센서를 사용할 수 없습니다. 아이디와 비밀번호로 로그인하세요.")
                }
            }
        }
        val decorView = activity.window?.decorView
        if (decorView != null) {
            decorView.post(showPrompt)
        } else {
            showPrompt.run()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
