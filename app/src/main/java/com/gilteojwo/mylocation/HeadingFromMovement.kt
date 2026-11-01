package com.gilteojwo.mylocation

import com.gilteojwo.geo.GeoMath
import com.gilteojwo.geo.GeoPoint

/**
 * 움직인 방향으로 내 차의 진행 방향을 계산한다.
 * 에뮬레이터에서는 Pleos 위치의 진행 방향이 늘 0°로 와서(11/01 시험), 직접 계산한 값을 먼저 쓴다.
 * 3m 이상 움직였을 때만 새로 계산한다. 그보다 적게 움직이면 GPS 흔들림일 수 있어서 앞의 방향을 그대로 둔다.
 * 아직 한 번도 움직이지 않았으면 SDK가 준 방향을 쓴다.
 */
class HeadingFromMovement {
    private var lastPoint: GeoPoint? = null // 마지막으로 방향을 계산한 곳
    private var movementHeading: Double? = null // 움직임으로 계산한 방향 (아직 없으면 null)

    fun update(point: GeoPoint, sdkHeadingDegrees: Double): Double {
        val from = lastPoint
        if (from == null) {
            lastPoint = point
        } else if (GeoMath.distanceMeters(from, point) >= MIN_MOVE_METERS) {
            movementHeading = GeoMath.bearingDegrees(from, point)
            lastPoint = point
        }
        return movementHeading ?: sdkHeadingDegrees
    }

    companion object {
        const val MIN_MOVE_METERS = 3.0 // 제안값
    }
}
