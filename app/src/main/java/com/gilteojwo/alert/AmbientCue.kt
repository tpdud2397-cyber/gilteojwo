package com.gilteojwo.alert

/**
 * 앰비언트 조명 (사양서 R-15): 알림 단계를 실내 조명 색으로 보여준다.
 * 조명 구역이 하나뿐이라(좌우 구분 불가, 11/01 시험) 오는 방향은 못 나누고 전체 색으로 단계만 표시한다.
 * 관심 주황, 경고 빨강, 양보 빨강 깜빡임. 알림 없음·해제는 null = 처음 조명으로 되돌린다.
 */
data class AmbientCue(val color: Int, val blinking: Boolean) {
    companion object {
        const val ORANGE = 0xFFFF8000.toInt() // 32비트 ARGB 숫자 (맨 앞 FF = 불투명)
        const val RED = 0xFFFF0000.toInt()

        fun of(stage: AlertStage): AmbientCue? = when (stage) {
            AlertStage.ATTENTION -> AmbientCue(ORANGE, blinking = false)
            AlertStage.WARNING -> AmbientCue(RED, blinking = false)
            AlertStage.YIELD -> AmbientCue(RED, blinking = true)
            AlertStage.NONE, AlertStage.CLEARED -> null
        }
    }
}
