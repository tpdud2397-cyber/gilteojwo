package com.gilteojwo.approach

import com.gilteojwo.geo.GeoMath
import com.gilteojwo.mylocation.MyPosition
import com.gilteojwo.receive.EmergencyVehicle
import kotlin.math.roundToInt

/**
 * 긴급차량마다 거리·방향·도착 시간을 계산하고(R-3), 접근 중인지 판단한다(R-4).
 * 차마다 최근 5초 동안의 거리를 기억해 두고, 거리가 줄어드는 속도로 도착 시간을 계산한다.
 * 기준값(아래 companion object)은 제안값이며 시연 후 조정한다.
 */
class ApproachTracker {
    /** 어느 시각에 몇 m 떨어져 있었는지 */
    private class DistanceSample(val timeMillis: Long, val meters: Double)

    // 차량 ID별 최근 거리 기록
    private val history = mutableMapOf<String, MutableList<DistanceSample>>()

    /** 내 위치가 새로 오거나 긴급차량 목록이 바뀔 때마다 부른다. 가까운 차부터 돌려준다. */
    fun update(my: MyPosition, vehicles: List<EmergencyVehicle>): List<ApproachInfo> {
        // 목록에서 사라진 차의 기록은 지운다
        history.keys.retainAll(vehicles.map { it.id }.toSet())
        return vehicles.map { judge(my, it) }.sortedBy { it.distanceMeters }
    }

    private fun judge(my: MyPosition, vehicle: EmergencyVehicle): ApproachInfo {
        // R-3 거리와 방향
        val distance = GeoMath.distanceMeters(my.point, vehicle.point)
        val bearingToVehicle = GeoMath.bearingDegrees(my.point, vehicle.point)
        val direction = RelativeDirection.of(my.headingDegrees, bearingToVehicle)

        // 지금 거리를 기록하고, 5초보다 오래된 기록은 버린다
        val samples = history.getOrPut(vehicle.id) { mutableListOf() }
        samples.add(DistanceSample(my.timeMillis, distance))
        samples.removeAll { my.timeMillis - it.timeMillis > HISTORY_MILLIS }

        // R-3 도착 시간: 거리가 1초에 몇 m씩 줄고 있는지로 계산한다
        val closingSpeed = closingSpeedMetersPerSecond(samples)
        val isClosing = closingSpeed >= MIN_CLOSING_SPEED
        val arrivalSeconds = if (isClosing) (distance / closingSpeed).roundToInt() else null

        // R-4 접근 판정: 거리가 줄고 있고, 긴급차량이 내 쪽을 향해 달리고 있으면 '접근 중'
        val bearingToMe = GeoMath.bearingDegrees(vehicle.point, my.point)
        val headingTowardMe = GeoMath.angleDifference(vehicle.headingDegrees, bearingToMe) <= HEADING_TOLERANCE_DEGREES

        return ApproachInfo(vehicle, distance, direction, arrivalSeconds, isClosing && headingTowardMe)
    }

    /** 가장 오래된 기록과 가장 최근 기록을 비교해, 1초에 몇 m씩 가까워지는지. 기록이 하나뿐이면 0 */
    private fun closingSpeedMetersPerSecond(samples: List<DistanceSample>): Double {
        val oldest = samples.first()
        val newest = samples.last()
        val seconds = (newest.timeMillis - oldest.timeMillis) / 1000.0
        if (seconds <= 0) return 0.0
        return (oldest.meters - newest.meters) / seconds
    }

    companion object {
        const val HISTORY_MILLIS = 5_000L // 최근 5초 기록으로 가까워지는 속도를 잰다
        const val MIN_CLOSING_SPEED = 0.5 // 1초에 0.5m 넘게 가까워져야 '거리가 줄고 있다'로 본다 (GPS 흔들림 무시)
        const val HEADING_TOLERANCE_DEGREES = 60.0 // 긴급차량 진행 방향이 '나를 향하는 방향'과 60° 안이면 내 쪽으로 온다고 본다
    }
}
