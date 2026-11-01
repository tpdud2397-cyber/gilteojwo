package com.gilteojwo.vehiclestate

/** 기어 (P·R·N·D). 아직 모르면 UNKNOWN */
enum class Gear {
    P, R, N, D, UNKNOWN;

    companion object {
        /** 안드로이드 표준 차량 속성 GEAR_SELECTION의 숫자를 바꾼다: 1 = N, 2 = R, 4 = P, 8 = D */
        fun fromAndroid(value: Int?): Gear = when (value) {
            1 -> N
            2 -> R
            4 -> P
            8 -> D
            else -> UNKNOWN
        }
    }
}

/** 방향지시등 */
enum class TurnSignal(val koreanName: String) {
    OFF("꺼짐"),
    LEFT("왼쪽"),
    RIGHT("오른쪽"),
    UNKNOWN("확인 중"),
}

/**
 * 내 차 상태: R-5 속도, R-6 브레이크, R-7 방향지시등, R-8 기어.
 * 양보 안내 문구를 고를 때(달리는 중인지, 서 있는지)와 양보 감지(우측 방향지시등)에 쓴다.
 * 아직 값을 받지 못한 항목은 null 또는 UNKNOWN이다.
 */
data class VehicleState(
    val speedKmh: Double? = null,
    val gear: Gear = Gear.UNKNOWN,
    val brakePressed: Boolean? = null,
    val turnSignal: TurnSignal = TurnSignal.UNKNOWN,
    val drivingRestricted: Boolean? = null, // R-9 차의 주행 제한 상태 (CarUxRestrictions)
) {
    /**
     * 주행 중인가 (R-9): 화면을 단순하게 할지 정한다.
     * 차가 알려주는 주행 제한 상태를 먼저 믿는다 (에뮬레이터: 기어가 P를 벗어나면 걸림).
     * 그 값을 못 읽으면 기어 D·R이거나 움직이고 있으면 주행 중으로 본다.
     */
    val isDriving: Boolean
        get() = drivingRestricted ?: (gear == Gear.D || gear == Gear.R || (speedKmh ?: 0.0) > 0.0)

    /** 예: "14 km/h · 기어 D · 브레이크 안 밟음 · 방향지시등 오른쪽" */
    fun describe(): String {
        val speed = speedKmh?.let { "%.0f km/h".format(it) } ?: "속도 확인 중"
        val gearText = if (gear == Gear.UNKNOWN) "기어 확인 중" else "기어 ${gear.name}"
        val brake = when (brakePressed) {
            true -> "브레이크 밟음"
            false -> "브레이크 안 밟음"
            null -> "브레이크 확인 중"
        }
        return "$speed · $gearText · $brake · 방향지시등 ${turnSignal.koreanName}"
    }
}
