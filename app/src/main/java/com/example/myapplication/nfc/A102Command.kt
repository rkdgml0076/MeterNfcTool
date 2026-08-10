package com.example.myapplication.nfc

object A102Command {
    const val PREFIX = "A102"

    /**
     * 화면 표시용 문자열 (A102는 고정값이므로 단순 명령어명 반환)
     */
    fun formatDisplay(): String = "A102 (NFC 탈출)"

    /**
     * 기기 전송용 패킷 문자열 ("A102" 4자리)
     */
    fun formatPayload(): String = PREFIX

    /**
     * 인자가 따로 없으므로 항상 유효함
     */
    fun isValid(): Boolean = true
}