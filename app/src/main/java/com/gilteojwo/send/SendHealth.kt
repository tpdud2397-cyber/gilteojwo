package com.gilteojwo.send

/** 송신 상태 한 줄과 경고 여부 */
data class SendHealthState(val isWarning: Boolean, val text: String)

/**
 * 송신 상태 표시 (사양서 S-6): 마지막으로 서버에 보낸 시각을 보여주고, 5초 넘게 실패하면 경고한다.
 * 수신 앱은 10초 넘게 소식이 없는 긴급차량을 목록에서 빼므로, 그 전에 운전자가 알 수 있게 5초에서 경고한다.
 */
object SendHealth {
    const val WARNING_MILLIS = 5_000L

    /** lastSuccessMillis: 마지막으로 보내기에 성공한 시각 (아직 없으면 null), startedMillis: 긴급출동을 시작한 시각 */
    fun of(lastSuccessMillis: Long?, startedMillis: Long, nowMillis: Long): SendHealthState {
        val since = lastSuccessMillis ?: startedMillis
        val silentSeconds = ((nowMillis - since) / 1000).toInt()
        return when {
            nowMillis - since > WARNING_MILLIS ->
                SendHealthState(true, "${silentSeconds}초째 송신 실패 · 인터넷과 위치를 확인하세요")
            lastSuccessMillis == null -> SendHealthState(false, "송신 준비 중")
            else -> SendHealthState(false, "정상 송신 중 · ${silentSeconds}초 전")
        }
    }
}

/** 등록 코드 입력 정리 (사양서 S-1) */
object RegCode {
    /** 앞뒤 빈칸을 없앤다. 비어 있으면 null */
    fun normalize(input: String): String? = input.trim().ifEmpty { null }
}
