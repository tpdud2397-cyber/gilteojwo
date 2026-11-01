package com.gilteojwo.approach

import com.gilteojwo.receive.EmergencyVehicle

/** 긴급차량 하나에 대한 계산 결과 (R-3 거리·방향·도착 시간, R-4 접근 판정) */
data class ApproachInfo(
    val vehicle: EmergencyVehicle,
    val distanceMeters: Double, // 직선 거리
    val direction: RelativeDirection, // 내 차 기준 어느 쪽에 있는지
    val arrivalSeconds: Int?, // 도착 예상 시간(초). 거리가 줄고 있지 않으면 null
    val isApproaching: Boolean, // 접근 중인지
)
