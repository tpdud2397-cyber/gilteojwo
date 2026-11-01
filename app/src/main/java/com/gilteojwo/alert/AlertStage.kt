package com.gilteojwo.alert

import com.gilteojwo.approach.ApproachInfo

/** 알림 단계 (사양서 '알림 단계와 양보 안내'). 관심 → 경고 → 양보로 올라가고, 지나가면 해제 */
enum class AlertStage(val koreanName: String) {
    NONE("알림 없음"),
    ATTENTION("관심"),
    WARNING("경고"),
    YIELD("양보"),
    CLEARED("해제"),
}

/**
 * 지금 알림 단계와 알리고 있는 긴급차량. 알림 없음이면 target은 null.
 * signalLost: 해제된 까닭이 '지나감'이 아니라 '정보가 끊김(목록에서 사라짐)'일 때 true
 */
data class AlertState(val stage: AlertStage, val target: ApproachInfo?, val signalLost: Boolean = false)
