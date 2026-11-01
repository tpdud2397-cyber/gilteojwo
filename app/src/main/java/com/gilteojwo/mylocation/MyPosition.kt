package com.gilteojwo.mylocation

import com.gilteojwo.geo.GeoPoint

/** 내 차의 현재 위치와 진행 방향 (0° = 북쪽, 90° = 동쪽, 180° = 남쪽, 270° = 서쪽) */
data class MyPosition(
    val point: GeoPoint,
    val headingDegrees: Double,
    val timeMillis: Long,
)
