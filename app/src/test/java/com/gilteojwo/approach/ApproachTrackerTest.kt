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
    fun `지나간 직후에는 5초 전보다 거리가 짧아도 도착 시간이 없다`() {
        // 남쪽 50m에서 북쪽으로 1초에 20m씩 달려 내 차를 지나간다: 50m 뒤 → 30m 뒤 → 10m 뒤 → 10m 앞 → 30m 앞
        listOf(-50.0, -30.0, -10.0, 10.0).forEachIndexed { second, north ->
            tracker.update(me(second * 1000L), listOf(ambulance(CITY_HALL.moved(north, 0.0), 0.0, second * 1000L)))
        }
        val result = tracker.update(me(4000), listOf(ambulance(CITY_HALL.moved(30.0, 0.0), 0.0, 4000))).single()

        assertEquals(RelativeDirection.FRONT, result.direction)
        assertFalse(result.isApproaching)
        assertNull(result.arrivalSeconds)
    }

    @Test
    fun `내 위치와 구급차 위치가 오는 주기가 달라도 도착 시간은 실제 속도대로 나온다`() {
        // 구급차 위치는 1초마다, 내 위치는 0.5~1.2초 간격으로 온다. 내 위치가 올 때는 가장 최근에 받은 구급차 위치로 계산한다.
        // 구급차는 남쪽 500m에서 1초에 20m씩 다가온다 → 4초 뒤 420m, 남은 시간 420 ÷ 20 = 21초
        var result: ApproachInfo? = null
        for (myTime in listOf(0L, 500L, 1200L, 1700L, 2400L, 3600L, 4100L, 4800L)) {
            val sentTime = myTime / 1000 * 1000 // 이 시각까지 받은 가장 최근 구급차 위치의 시각
            val north = -(500.0 - 20.0 * sentTime / 1000)
            result = tracker.update(me(myTime), listOf(ambulance(CITY_HALL.moved(north, 0.0), 0.0, sentTime))).single()
        }

        assertTrue(result!!.isApproaching)
        assertEquals(21, result.arrivalSeconds)
    }

    @Test
    fun `구급차 위치가 늦게 도착해도 서버에 기록된 시각으로 계산해 도착 시간이 흔들리지 않는다`() {
        // 구급차 위치는 정확히 1초마다 서버에 기록되지만(남쪽 500m에서 1초에 20m씩), 내 차에는 0~0.5초씩 늦게 들어온다
        val delays = listOf(0L, 400L, 100L, 300L, 500L)
        var result: ApproachInfo? = null
        delays.forEachIndexed { second, delay ->
            val recordedTime = second * 1000L
            val vehicle = EmergencyVehicle(
                "구급차1", VehicleType.AMBULANCE, CITY_HALL.moved(-(500.0 - 20.0 * second), 0.0), 0.0, 72.0,
                updatedMillis = recordedTime + delay, // 내 차가 받은 시각
                recordedMillis = recordedTime, // 서버에 기록된 시각
            )
            result = tracker.update(me(recordedTime + delay), listOf(vehicle)).single()
        }

        // 4초 뒤 420m, 1초에 20m씩 → 21초
        assertEquals(21, result!!.arrivalSeconds)
    }

    @Test
    fun `내 진행 방향 기준 긴급차량의 정확한 각도도 알려준다 (레이더용)`() {
        // 북쪽을 보고 있을 때 남동쪽(동쪽 300m, 남쪽 300m)에 있으면 135도
        val southEast = tracker.update(me(0), listOf(ambulance(CITY_HALL.moved(-300.0, 300.0), 0.0, 0))).single()
        // 동쪽을 보고 있을 때 북쪽에 있으면 270도 (왼쪽)
        val headingEast = ApproachTracker()
            .update(MyPosition(CITY_HALL, 90.0, 0), listOf(ambulance(CITY_HALL.moved(400.0, 0.0), 0.0, 0)))
            .single()

        assertEquals(135.0, southEast.relativeAngleDegrees, 1.0)
        assertEquals(270.0, headingEast.relativeAngleDegrees, 1.0)
    }

    @Test
    fun `가까운 차가 목록 앞에 온다`() {
        val far = ambulance(CITY_HALL.moved(-900.0, 0.0), 0.0, 0, id = "먼 차")
        val near = ambulance(CITY_HALL.moved(200.0, 0.0), 180.0, 0, id = "가까운 차")

        val results = tracker.update(me(0), listOf(far, near))

        assertEquals(listOf("가까운 차", "먼 차"), results.map { it.vehicle.id })
    }
}
