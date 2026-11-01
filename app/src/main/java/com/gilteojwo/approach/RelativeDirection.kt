package com.gilteojwo.approach

import com.gilteojwo.geo.GeoMath

/**
 * 내 차 기준으로 긴급차량이 있는 쪽. 시계처럼 8방향으로 나눈다 (12시 = 내 진행 방향).
 * 순서가 중요하다: 12시부터 시계 방향으로 45°씩이다.
 */
enum class RelativeDirection(val koreanName: String, val clock: String) {
    FRONT("전방", "12시"),
    FRONT_RIGHT("우측 전방", "1~2시"),
    RIGHT("우측", "3시"),
    REAR_RIGHT("우측 후방", "4~5시"),
    REAR("후방", "6시"),
    REAR_LEFT("좌측 후방", "7~8시"),
    LEFT("좌측", "9시"),
    FRONT_LEFT("좌측 전방", "10~11시");

    companion object {
        /**
         * 내 진행 방향을 0°로 놓고 긴급차량이 몇 도 쪽에 있는지 본 뒤, 45°씩 여덟 쪽으로 나눈다.
         * 방향마다 가운데에서 ±22.5°까지다. 예: 북쪽으로 가는데 차가 남동쪽(135°)에 있으면 우측 후방
         */
        fun of(myHeadingDegrees: Double, bearingToVehicleDegrees: Double): RelativeDirection {
            val angle = GeoMath.normalize(bearingToVehicleDegrees - myHeadingDegrees)
            val index = ((angle + 22.5) / 45).toInt() % 8
            return entries[index]
        }
    }
}
