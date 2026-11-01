package com.gilteojwo.approach

import com.gilteojwo.CITY_HALL
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 화면과 음성에 쓰는 도착 시간 글자 */
class ApproachInfoTest {
    private fun infoWithArrival(seconds: Int?) = ApproachInfo(
        vehicle = EmergencyVehicle("구급차1", VehicleType.AMBULANCE, CITY_HALL, 0.0, 60.0, 0),
        distanceMeters = 300.0,
        direction = RelativeDirection.REAR,
        arrivalSeconds = seconds,
        isApproaching = seconds != null,
    )

    @Test
    fun `도착 시간 30초까지는 1초 단위로 보여준다`() {
        assertEquals("약 12초", infoWithArrival(12).arrivalText)
        assertEquals("약 30초", infoWithArrival(30).arrivalText)
    }

    @Test
    fun `도착 시간이 30초를 넘으면 가까운 5초 단위로 보여준다`() {
        assertEquals("약 30초", infoWithArrival(31).arrivalText)
        assertEquals("약 45초", infoWithArrival(47).arrivalText)
        assertEquals("약 50초", infoWithArrival(48).arrivalText)
    }

    @Test
    fun `도착 시간이 없으면 글자도 없다`() {
        assertNull(infoWithArrival(null).arrivalText)
    }
}
