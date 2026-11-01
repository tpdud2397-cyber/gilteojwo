package com.gilteojwo.send

/**
 * 출동 확인 (사양서 S-7): 긴급출동 모드가 60분 넘게 켜져 있으면 "아직 출동 중인가요?"를 묻는다.
 * "출동 중"을 누르면 60분 연장하고, 2분 안에 답이 없으면 자동으로 끄고 '자동 종료'로 기록한다.
 * 출동이 끝났는데 깜빡 잊고 켜 둬서 위치가 계속 나가는 것을 막기 위해서다. 시간은 제안값이다.
 */
sealed class DispatchCheck {
    /** 아직 물을 때가 아님 */
    object Ok : DispatchCheck()

    /** 묻는 중. secondsLeft초 안에 답해야 한다 */
    data class Asking(val secondsLeft: Int) : DispatchCheck()

    /** 답이 없어 자동 종료할 때 */
    object Expired : DispatchCheck()

    companion object {
        const val CHECK_AFTER_MILLIS = 60 * 60_000L // 60분
        const val ANSWER_WITHIN_MILLIS = 2 * 60_000L // 2분

        /** lastConfirmedMillis: 출동을 시작했거나 마지막으로 "출동 중"을 누른 시각 */
        fun of(
            lastConfirmedMillis: Long,
            nowMillis: Long,
            checkAfterMillis: Long = CHECK_AFTER_MILLIS,
            answerWithinMillis: Long = ANSWER_WITHIN_MILLIS,
        ): DispatchCheck {
            val elapsed = nowMillis - lastConfirmedMillis
            val deadline = checkAfterMillis + answerWithinMillis
            return when {
                elapsed < checkAfterMillis -> Ok
                elapsed <= deadline -> Asking(((deadline - elapsed + 999) / 1000).toInt()) // 올림
                else -> Expired
            }
        }
    }
}
