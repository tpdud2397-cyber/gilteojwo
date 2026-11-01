package com.gilteojwo.approach

import com.gilteojwo.CITY_HALL
import com.gilteojwo.geo.GeoPoint
import com.gilteojwo.moved
import com.gilteojwo.mylocation.MyPosition
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내 차는 서울시청에 서서 북쪽(0도)을 보고 있다 */
class ApproachTrackerTest {
    private val tracker = ApproachTracker()

    private fun me(timeMillis: Long) = MyPosition(CITY_HALL, 0.0, timeMillis)

    private fun ambulance(point: GeoPoint, headingDegrees: Double, timeMillis: Long, id: String = "구급차1") =
        EmergencyVehicle(id, VehicleType.AMBULANCE, point, headingDegrees, 72.0, timeMillis)

    @Test
    fun `처음 본 차는 거리와 방향만 알고 아직 접근 중이라고 하지 않는다`() {
        val result = tracker.update(me(0), listOf(ambulance(CITY_HALL.moved(-500.0, 0.0), 0.0, 0))).single()

        assertEquals(500.0, result.distanceMeters, 2.0)
        assertEquals(RelativeDirection.REAR, result.direction)
        assertFalse(result.isApproaching)
        assertNull(result.arrivalSeconds)
    }

    @Test
    fun `뒤에서 1초에 20m씩 다가오면 접근 중이고 약 24초 뒤 도착`() {
        tracker.update(me(0), listOf(ambulance(CITY_HALL.moved(-500.0, 0.0), 0.0, 0)))
        val result = tracker.update(me(1000), listOf(ambulance(CITY_HALL.moved(-480.0, 0.0), 0.0, 1000))).single()

        assertEquals(RelativeDirection.REAR, result.direction)
        assertTrue(result.isApproaching)
        assertEquals(24, result.arrivalSeconds)
    }

    @Test
    fun `멀어지는 차는 접근 중이 아니고 도착 시간도 없다`() {
        tracker.update(me(0), listOf(ambulance(CITY_HALL.moved(-500.0, 0.0), 180.0, 0)))
        val result = tracker.update(me(1000), listOf(ambulance(CITY_HALL.moved(-520.0, 0.0), 180.0, 1000))).single()

        assertFalse(result.isApproaching)
        assertNull(result.arrivalSeconds)
    }

    @Test
    fun `거리가 줄어도 내 쪽을 향해 오지 않으면 접근 중이 아니다`() {
        // 동쪽 300m, 남쪽 100m에서 북쪽으로 달리는 차: 거리는 조금 줄지만 나를 향하지 않는다 (옆 길)
        tracker.update(me(0), listOf(ambulance(CITY_HALL.moved(-100.0, 300.0), 0.0, 0)))
        val result = tracker.update(me(1000), listOf(ambulance(CITY_HALL.moved(-80.0, 300.0), 0.0, 1000))).single()

        assertFalse(result.isApproaching)
    }

    @Test
    fun `가까운 차가 목록 앞에 온다`() {
        val far = ambulance(CITY_HALL.moved(-900.0, 0.0), 0.0, 0, id = "먼 차")
        val near = ambulance(CITY_HALL.moved(200.0, 0.0), 180.0, 0, id = "가까운 차")

        val results = tracker.update(me(0), listOf(far, near))

        assertEquals(listOf("가까운 차", "먼 차"), results.map { it.vehicle.id })
    }
}
