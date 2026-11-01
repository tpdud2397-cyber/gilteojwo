package com.gilteojwo.alert

import com.gilteojwo.vehiclestate.Gear
import com.gilteojwo.vehiclestate.TurnSignal
import com.gilteojwo.vehiclestate.VehicleState

/** 서버에 보내는 양보 확인 상태. code는 서버 표(yield_acks)에 적는 글자 */
enum class YieldStatus(val code: String) {
    NOTIFIED("notified"), // 알림 받음
    YIELDED("yielded"), // 양보함
}

/**
 * 양보 감지 (사양서 R-16): 알림 중 내 차 상태를 보고 양보했는지 자동으로 판단한다. 버튼은 없다.
 * - 알림이 시작되면(관심 이상) "알림 받음"을 한 번 보낸다
 * - 알림 중 아래 중 하나면 "양보함"을 한 번 보낸다
 *   ① 우측 방향지시등 ② 알림이 시작될 때보다 시속 15km 넘게 감속 ③ 서서(시속 1km 미만) 브레이크를 밟음 (주차 P는 제외)
 * - 알림이 아니면(알림 없음·해제) 아무것도 보내지 않는다
 * 출동마다 한 번씩만 보낸다 (같은 구급차라도 새 출동이면 다시 보낸다). 기준값은 제안값이다.
 */
class YieldDetector {
    private var notifiedKey: String? = null // 알림 받음을 보낸 출동
    private var yieldedKey: String? = null // 양보함을 보낸 출동
    private var speedAtStartKmh: Double? = null

    /** 계산 결과가 새로 나올 때마다 부른다. 서버에 보낼 것이 있으면 그 상태, 없으면 null */
    fun update(alert: AlertState, car: VehicleState): YieldStatus? {
        val target = alert.target ?: return null
        val isAlerting = alert.stage == AlertStage.ATTENTION ||
            alert.stage == AlertStage.WARNING ||
            alert.stage == AlertStage.YIELD
        if (!isAlerting) return null

        // 출동 번호로 구분한다. 출동 번호를 모르면 차량 ID로
        val key = target.vehicle.dispatchId ?: target.vehicle.id
        if (key != notifiedKey) {
            notifiedKey = key
            speedAtStartKmh = car.speedKmh
            return YieldStatus.NOTIFIED
        }
        if (key != yieldedKey && hasYielded(car)) {
            yieldedKey = key
            return YieldStatus.YIELDED
        }
        return null
    }

    private fun hasYielded(car: VehicleState): Boolean {
        val speed = car.speedKmh
        val start = speedAtStartKmh
        val signaledRight = car.turnSignal == TurnSignal.RIGHT
        val slowedDown = speed != null && start != null && start - speed > SLOW_DOWN_KMH
        val stoppedWithBrake = speed != null && speed < STOPPED_KMH && car.brakePressed == true && car.gear != Gear.P
        return signaledRight || slowedDown || stoppedWithBrake
    }

    companion object {
        const val SLOW_DOWN_KMH = 15.0
        const val STOPPED_KMH = 1.0
    }
}
