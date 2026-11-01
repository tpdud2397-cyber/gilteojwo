package com.gilteojwo

import com.gilteojwo.geo.GeoPoint
import kotlin.math.cos

/** 시험용: 지금 점에서 북쪽으로 northMeters, 동쪽으로 eastMeters 옮긴 점 (남쪽·서쪽은 음수) */
fun GeoPoint.moved(northMeters: Double, eastMeters: Double): GeoPoint {
    val metersPerDegree = 111_195.0 // 위도 1도의 길이 (지구 반지름 6,371 km 기준)
    return GeoPoint(
        latitude + northMeters / metersPerDegree,
        longitude + eastMeters / (metersPerDegree * cos(Math.toRadians(latitude))),
    )
}

/** 시험 기준점: 서울시청 */
val CITY_HALL = GeoPoint(37.5665, 126.9780)
