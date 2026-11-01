package com.gilteojwo.receive

import com.gilteojwo.geo.GeoMath

/**
 * 데이터 끊김 처리 (사양서 R-14): 송신이 끊기면 마지막 위치와 속도로 5초간 추정한 뒤 해제한다.
 * - 마지막 소식이 1.5초 안이면: 받은 위치 그대로
 * - 1.5초~5초: 마지막 진행 방향으로 (속도 × 지난 시간)만큼 옮긴 위치로 추정한다 (터널 등에서 잠깐 끊겨도 알림이 이어지게)
 * - 5초가 넘으면: 목록에서 뺀다 → 알리던 차면 '해제'가 된다
 */
object DeadReckoning {
    const val STALE_MILLIS = 1_500L // 이보다 오래 소식이 없으면 추정을 시작한다 (보통 1초마다 온다)
    const val LOST_MILLIS = 5_000L // 이보다 오래 소식이 없으면 뺀다

    /** 지금 시각에 맞는 긴급차량 위치. 빼야 하면 null */
    fun estimate(vehicle: EmergencyVehicle, nowMillis: Long): EmergencyVehicle? {
        val silentMillis = nowMillis - vehicle.updatedMillis
        if (silentMillis > LOST_MILLIS) return null
        if (silentMillis <= STALE_MILLIS) return vehicle

        val meters = vehicle.speedKmh / 3.6 * silentMillis / 1000.0
        return vehicle.copy(
            point = GeoMath.destination(vehicle.point, vehicle.headingDegrees, meters),
            recordedMillis = vehicle.recordedMillis + silentMillis, // 거리 기록이 '그 시각의 위치'로 쌓이게
            isEstimated = true,
        )
    }
}
