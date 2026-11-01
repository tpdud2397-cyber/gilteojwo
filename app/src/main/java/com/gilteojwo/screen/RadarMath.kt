package com.gilteojwo.screen

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 레이더 그림 계산 (사양서 '접근 알림' 화면: 내 차 기준 레이더).
 * 가운데가 내 차, 화면 위쪽이 내 진행 방향, 바깥 동그라미가 1,000m다.
 * 1,000m보다 먼 차는 바깥 동그라미에 붙여서 보여준다.
 */
object RadarMath {
    const val RANGE_METERS = 1000.0

    /**
     * 레이더 가운데(내 차)에서 긴급차량 점까지 얼마나 떨어져 그릴지 (x: 오른쪽 +, y: 아래쪽 +).
     * relativeAngleDegrees: 내 진행 방향 기준 각도 (0° 앞, 90° 오른쪽, 180° 뒤, 270° 왼쪽)
     */
    fun offset(relativeAngleDegrees: Double, distanceMeters: Double, radius: Float): Pair<Float, Float> {
        val fromCenter = min(distanceMeters, RANGE_METERS) / RANGE_METERS * radius
        val radians = Math.toRadians(relativeAngleDegrees)
        val x = fromCenter * sin(radians)
        val y = -fromCenter * cos(radians) // 화면은 아래쪽이 +라서 앞(위)이 -
        return x.toFloat() to y.toFloat()
    }
}
