package com.gilteojwo.approach

import com.gilteojwo.receive.EmergencyVehicle

/** 긴급차량 하나에 대한 계산 결과 (R-3 거리·방향·도착 시간, R-4 접근 판정) */
data class ApproachInfo(
    val vehicle: EmergencyVehicle,
    val distanceMeters: Double, // 직선 거리
    val direction: RelativeDirection, // 내 차 기준 어느 쪽에 있는지
    val arrivalSeconds: Int?, // 도착 예상 시간(초). 접근 중이 아니면 null
    val isApproaching: Boolean, // 접근 중인지
) {
    /**
     * 화면·음성용 도착 시간. 예: "약 12초", "약 45초". 도착 시간이 없으면 null.
     * 30초가 넘으면 가까운 5초 단위로 반올림한다. 먼 거리에서는 몇 초 차이가 중요하지 않고, 숫자가 덜 흔들린다.
     */
    val arrivalText: String?
        get() {
            val seconds = arrivalSeconds ?: return null
            val shown = if (seconds > 30) (seconds + 2) / 5 * 5 else seconds
            return "약 ${shown}초"
        }
}
