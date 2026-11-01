package com.gilteojwo.screen

import com.gilteojwo.alert.AlertStage
import com.gilteojwo.alert.AlertState
import com.gilteojwo.alert.YieldAdvice
import kotlin.math.roundToInt

/**
 * 주행 중 알림 화면 (사양서 R-9 "주행 중에는 아이콘 하나와 문구 한 줄만"): 큰 그림 하나(icon)와 문구 한 줄(line).
 * - 양보 안내가 있으면(양보·해제) 그 그림과 문구. 예: → "우측으로 비켜주세요"
 * - 없으면(관심·경고) 긴급차량이 오는 쪽 화살표와 "좌측 후방 구급차 480m"
 * 레이더·도착 시간 같은 자세한 정보는 서 있을 때만 보여준다.
 */
data class DrivingView(val icon: String, val line: String) {
    companion object {
        fun of(alert: AlertState, advice: YieldAdvice?): DrivingView? {
            val target = alert.target ?: return null
            if (alert.stage == AlertStage.NONE) return null
            if (advice != null) return DrivingView(advice.action.icon, advice.text)
            val line = "${target.direction.koreanName} ${target.vehicle.type.koreanName} ${target.distanceMeters.roundToInt()}m"
            return DrivingView(target.direction.arrow, line)
        }
    }
}
