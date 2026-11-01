package com.gilteojwo.receive

import com.gilteojwo.geo.GeoPoint

/** 긴급차량 종류 */
enum class VehicleType(val koreanName: String) {
    AMBULANCE("구급차"),
    FIRE_TRUCK("소방차"),
    POLICE("경찰차"),
}

/** 서버에서 받은 긴급차량 하나의 현재 상태 (서버 표 positions_live의 한 줄) */
data class EmergencyVehicle(
    val id: String,
    val type: VehicleType,
    val point: GeoPoint,
    val headingDegrees: Double, // 긴급차량의 진행 방향 (0° = 북쪽)
    val speedKmh: Double,
    val updatedMillis: Long, // 서버에 마지막으로 올라온 시각
)
