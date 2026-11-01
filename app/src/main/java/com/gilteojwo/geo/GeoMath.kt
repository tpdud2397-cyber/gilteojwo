package com.gilteojwo.geo

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 지도 계산 도구: 두 점 사이 거리, 방위, 두 방향의 차이 */
object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** 두 점 사이의 직선 거리(m). 지구를 공으로 보고 계산한다 (하버사인 공식) */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }

    /** from에서 to를 바라보는 방위 (0° = 북쪽, 90° = 동쪽, 180° = 남쪽, 270° = 서쪽) */
    fun bearingDegrees(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return normalize(Math.toDegrees(atan2(y, x)))
    }

    /** from에서 bearingDegrees 방향으로 meters만큼 간 점 (끊긴 긴급차량 위치 추정에 쓴다) */
    fun destination(from: GeoPoint, bearingDegrees: Double, meters: Double): GeoPoint {
        val angle = meters / EARTH_RADIUS_METERS // 지구 중심에서 본 이동 각도
        val bearing = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = asin(sin(lat1) * cos(angle) + cos(lat1) * sin(angle) * cos(bearing))
        val lon2 = lon1 + atan2(sin(bearing) * sin(angle) * cos(lat1), cos(angle) - sin(lat1) * sin(lat2))
        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    /** 두 방향이 몇 도 차이 나는지 (0~180°). 예: 350°와 10°는 20° */
    fun angleDifference(a: Double, b: Double): Double {
        val diff = normalize(a - b)
        return if (diff > 180) 360 - diff else diff
    }

    /** 각도를 0° 이상 360° 미만으로 맞춘다. 예: -90° → 270° */
    fun normalize(degrees: Double): Double = ((degrees % 360) + 360) % 360
}
