package com.gilteojwo.send

import com.gilteojwo.CITY_HALL
import com.gilteojwo.mylocation.MyPosition
import com.gilteojwo.vehiclestate.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 S-3 Pleos 송신 모드: 차가 받은 내 위치(FusedLocation)와 계기판 속도로 보낼 위치를 만든다 */
class SenderFixTest {
    private val position = MyPosition(CITY_HALL, headingDegrees = 90.0, timeMillis = 0)

    @Test
    fun `내 차 위치·진행 방향과 계기판 속도를 그대로 쓴다`() {
        val fix = SenderFix.fromCar(position, VehicleState(speedKmh = 42.0))

        assertEquals(CITY_HALL.latitude, fix.latitude, 0.0)
        assertEquals(CITY_HALL.longitude, fix.longitude, 0.0)
        assertEquals(90.0, fix.headingDegrees, 0.0)
        assertEquals(42.0, fix.speedKmh, 0.0)
    }

    @Test
    fun `속도를 아직 못 받았으면 0으로, 정확도는 모르니 비워 보낸다`() {
        val fix = SenderFix.fromCar(position, VehicleState())

        assertEquals(0.0, fix.speedKmh, 0.0)
        assertNull(fix.accuracyMeters)
    }
}
