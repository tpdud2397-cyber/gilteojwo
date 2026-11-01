package com.gilteojwo.receive

import com.gilteojwo.CITY_HALL
import com.gilteojwo.geo.GeoMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 사양서 R-14: 송신이 끊기면 마지막 위치와 속도로 5초간 추정한 뒤 해제 */
class DeadReckoningTest {
    // 북쪽으로 시속 60km(1초에 약 16.7m)로 달리는 구급차를 0초에 받았다
    private val received = EmergencyVehicle(
        "구급차1", VehicleType.AMBULANCE, CITY_HALL, 0.0, 60.0, updatedMillis = 0, recordedMillis = 100_000,
    )

    @Test
    fun `방금 받은 위치는 그대로 쓴다`() {
        val result = DeadReckoning.estimate(received, nowMillis = 1000)!!

        assertEquals(CITY_HALL, result.point)
        assertFalse(result.isEstimated)
    }

    @Test
    fun `3초 동안 소식이 없으면 진행 방향으로 속도만큼 옮겨 추정한다`() {
        val result = DeadReckoning.estimate(received, nowMillis = 3000)!!

        assertEquals(50.0, GeoMath.distanceMeters(CITY_HALL, result.point), 0.5) // 16.7m × 3초
        assertEquals(0.0, GeoMath.bearingDegrees(CITY_HALL, result.point), 1.0) // 북쪽
        assertEquals(103_000L, result.recordedMillis) // 추정한 위치의 시각도 3초 뒤로
        assertTrue(result.isEstimated)
    }

    @Test
    fun `5초 넘게 소식이 없으면 목록에서 뺀다`() {
        assertNull(DeadReckoning.estimate(received, nowMillis = 5001))
    }
}
