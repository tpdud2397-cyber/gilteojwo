package com.gilteojwo.mylocation

import com.gilteojwo.CITY_HALL
import com.gilteojwo.moved
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingFromMovementTest {
    private val heading = HeadingFromMovement()

    @Test
    fun `아직 움직이지 않았으면 SDK가 준 진행 방향을 쓴다`() {
        assertEquals(45.0, heading.update(CITY_HALL, 45.0), 0.001)
    }

    @Test
    fun `동쪽으로 10m 움직이면 SDK가 0도라고 해도 90도`() {
        heading.update(CITY_HALL, 0.0)
        assertEquals(90.0, heading.update(CITY_HALL.moved(0.0, 10.0), 0.0), 1.0)
    }

    @Test
    fun `3m보다 적게 움직이면 앞의 방향을 그대로 쓴다`() {
        heading.update(CITY_HALL, 0.0)
        heading.update(CITY_HALL.moved(0.0, 10.0), 0.0) // 동쪽 90도
        // 북쪽으로 1m, 동쪽으로 1m 더 (약 1.4m): 흔들림으로 보고 방향을 바꾸지 않는다
        assertEquals(90.0, heading.update(CITY_HALL.moved(1.0, 11.0), 0.0), 1.0)
    }
}
