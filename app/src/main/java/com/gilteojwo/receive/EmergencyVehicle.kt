package com.gilteojwo.receive

import com.gilteojwo.geo.GeoPoint

/** 긴급차량 종류. code는 서버 표에 적힌 이름이다. */
enum class VehicleType(val code: String, val koreanName: String) {
    AMBULANCE("ambulance", "구급차"),
    FIRE_TRUCK("fire_truck", "소방차"),
    POLICE("police", "경찰차");

    companion object {
        /** 서버 이름으로 종류를 찾는다. 모르는 이름이면 null */
        fun fromCode(code: String): VehicleType? = entries.find { it.code == code }
    }
}

/** 서버에서 받은 긴급차량 하나의 현재 상태 (서버 표 positions_live의 한 줄) */
data class EmergencyVehicle(
    val id: String,
    val type: VehicleType,
    val point: GeoPoint,
    val headingDegrees: Double, // 긴급차량의 진행 방향 (0° = 북쪽)
    val speedKmh: Double,
    val updatedMillis: Long, // 이 앱이 마지막으로 받은 시각. 10초 넘게 새 소식이 없으면 목록에서 뺀다
    // 이 위치가 서버에 기록된 시각(서버 시계, 표의 updated_at). 도착 시간 계산에 쓴다.
    // 실시간 전달은 0.1~0.5초씩 들쭉날쭉 늦게 오므로, 받은 시각보다 이 시각이 실제 움직임과 맞다. 모르면 받은 시각
    val recordedMillis: Long = updatedMillis,
    val dispatchId: String? = null, // 출동 번호 (양보 확인을 보낼 때 쓴다, R-16)
    val isEstimated: Boolean = false, // 송신이 끊겨 마지막 위치와 속도로 추정한 위치인지 (R-14)
)
