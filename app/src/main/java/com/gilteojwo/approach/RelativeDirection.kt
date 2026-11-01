package com.gilteojwo.approach

import com.gilteojwo.geo.GeoMath

/** 내 차 기준으로 긴급차량이 있는 쪽 */
enum class RelativeDirection(val koreanName: String) {
    FRONT("전방"),
    RIGHT("우측"),
    REAR("후방"),
    LEFT("좌측");

    companion object {
        /**
         * 내 진행 방향을 0°로 놓고 긴급차량이 몇 도 쪽에 있는지 본 뒤, 90°씩 네 쪽으로 나눈다.
         * 예: 북쪽으로 가는데 차가 동쪽에 있으면 90° → 우측
         */
        fun of(myHeadingDegrees: Double, bearingToVehicleDegrees: Double): RelativeDirection {
            val angle = GeoMath.normalize(bearingToVehicleDegrees - myHeadingDegrees)
            return when {
                angle < 45 || angle >= 315 -> FRONT
                angle < 135 -> RIGHT
                angle < 225 -> REAR
                else -> LEFT
            }
        }
    }
}
