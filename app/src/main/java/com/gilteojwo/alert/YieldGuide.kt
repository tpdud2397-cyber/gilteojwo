package com.gilteojwo.alert

import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.RelativeDirection
import com.gilteojwo.geo.GeoMath
import com.gilteojwo.vehiclestate.TurnSignal
import com.gilteojwo.vehiclestate.VehicleState

/** 양보 안내의 종류. 화면에 크게 보여줄 그림 하나를 고르는 데 쓴다 (사양서: 큰 아이콘 1개 + 한 줄 문구) */
enum class YieldAction {
    MOVE_RIGHT, // 우측으로 비켜주세요
    SLOW_DOWN, // 속도를 줄이고 진로를 비켜주세요
    STOP_BEFORE_CROSSING, // 녹색 신호여도 교차로에 진입하지 마세요
    CONFIRMED, // 확인했습니다 (양보 행동을 함)
    PASSED, // 지나갔습니다
}

/** 양보 안내 한 줄과 그 종류. isConfirmed = 운전자가 양보 행동(우측 방향지시등)을 해서 반복 안내를 멈출 때 */
data class YieldAdvice(val text: String, val action: YieldAction) {
    val isConfirmed: Boolean get() = action == YieldAction.CONFIRMED
}

/**
 * 양보 안내 문구 고르기 (사양서 '양보 안내' 표, 도로교통법 제29조).
 * 양보 단계에서는 내 차 상태에 맞는 행동 하나만, 해제 단계에서는 통과 안내를 고른다. 관심·경고 단계에는 없다.
 * 고르는 순서:
 *   ① 우측 방향지시등이 켜져 있으면 "확인했습니다"
 *   ② 서서 브레이크를 밟고 있고, 긴급차량이 옆에서 가로질러 오면(진행 방향이 45°~135° 어긋남) 교차로 문구
 *   ③ 긴급차량이 뒤쪽(후방·좌측 후방·우측 후방)이면 "우측으로 비켜주세요"
 *   ④ 그 밖(앞·옆)이면 "속도를 줄이고 진로를 비켜주세요"
 * 사양서에 없는 경우(서 있는데 뒤에서 옴 등)는 ③·④처럼 방향으로만 고른다 (11/01 계획에서 정함).
 * 긴급차량이 막 지나가 '접근 아님'이면 해제될 때까지 새 안내를 하지 않는다.
 */
object YieldGuide {
    private val REAR_DIRECTIONS = setOf(RelativeDirection.REAR, RelativeDirection.REAR_LEFT, RelativeDirection.REAR_RIGHT)
    private const val STOPPED_KMH = 1.0 // 시속 1km 아래면 서 있는 것으로 본다
    private const val CROSSING_MIN_DEGREES = 45.0
    private const val CROSSING_MAX_DEGREES = 135.0

    fun choose(alert: AlertState, myHeadingDegrees: Double, car: VehicleState): YieldAdvice? {
        val target = alert.target ?: return null
        return when (alert.stage) {
            // 막 지나가서 해제를 기다리는 3초 동안은 새 안내를 하지 않는다 (지나간 차에 비키라고 하지 않게)
            AlertStage.YIELD -> if (target.isApproaching) duringYield(target, myHeadingDegrees, car) else null
            AlertStage.CLEARED ->
                YieldAdvice(if (isStopped(car)) "지나갔습니다. 출발하셔도 됩니다" else "지나갔습니다", YieldAction.PASSED)
            else -> null
        }
    }

    private fun duringYield(target: ApproachInfo, myHeadingDegrees: Double, car: VehicleState): YieldAdvice {
        if (car.turnSignal == TurnSignal.RIGHT) return YieldAdvice("확인했습니다", YieldAction.CONFIRMED)

        val headingGap = GeoMath.angleDifference(target.vehicle.headingDegrees, myHeadingDegrees)
        val isCrossing = headingGap in CROSSING_MIN_DEGREES..CROSSING_MAX_DEGREES
        if (isStopped(car) && car.brakePressed == true && isCrossing) {
            return YieldAdvice("녹색 신호여도 교차로에 진입하지 마세요", YieldAction.STOP_BEFORE_CROSSING)
        }

        if (target.direction in REAR_DIRECTIONS) return YieldAdvice("우측으로 비켜주세요", YieldAction.MOVE_RIGHT)
        return YieldAdvice("속도를 줄이고 진로를 비켜주세요", YieldAction.SLOW_DOWN)
    }

    private fun isStopped(car: VehicleState): Boolean {
        val speed = car.speedKmh ?: return false
        return speed < STOPPED_KMH
    }
}
