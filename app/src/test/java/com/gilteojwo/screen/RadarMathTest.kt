package com.gilteojwo.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/** 레이더: 가운데가 내 차(위쪽이 내 진행 방향), 바깥 동그라미가 1,000m */
class RadarMathTest {
    @Test
    fun `뒤에서 500m면 내 차 바로 아래 반지름의 절반 거리`() {
        val (x, y) = RadarMath.offset(relativeAngleDegrees = 180.0, distanceMeters = 500.0, radius = 400f)

        assertEquals(0f, x, 0.5f)
        assertEquals(200f, y, 0.5f) // 화면은 아래쪽이 +
    }

    @Test
    fun `오른쪽 1,000m보다 멀면 바깥 동그라미에 붙인다`() {
        val (x, y) = RadarMath.offset(relativeAngleDegrees = 90.0, distanceMeters = 1500.0, radius = 400f)

        assertEquals(400f, x, 0.5f)
        assertEquals(0f, y, 0.5f)
    }

    @Test
    fun `앞에서 200m면 위쪽 5분의 1 거리`() {
        val (x, y) = RadarMath.offset(relativeAngleDegrees = 0.0, distanceMeters = 200.0, radius = 400f)

        assertEquals(0f, x, 0.5f)
        assertEquals(-80f, y, 0.5f)
    }
}
