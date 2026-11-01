package com.gilteojwo.receive

import com.gilteojwo.CITY_HALL
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pleos 송신 모드: 내 차가 실제로 출동 중이면 내 차는 수신 목록에서 뺀다 (자기 자신에게 알리지 않게) */
class OwnDispatchFilterTest {
    private fun vehicle(id: String, dispatchId: String?) =
        EmergencyVehicle(id, VehicleType.AMBULANCE, CITY_HALL, 0.0, 60.0, 0, dispatchId = dispatchId)

    private val mine = vehicle("내 구급차", "내 출동")
    private val other = vehicle("다른 소방차", "다른 출동")

    @Test
    fun `내 차가 실제 출동 중이면 내 출동은 빼고 다른 차만 남긴다`() {
        assertEquals(listOf(other), OwnDispatchFilter.apply(listOf(mine, other), ownRealDispatchId = "내 출동"))
    }

    @Test
    fun `출동 중이 아니거나 시연 중이면 아무것도 빼지 않는다`() {
        // 시연 경로는 가상의 긴급차량이라 그대로 받아 알린다 (차량 앱 한 대로 시연)
        assertEquals(listOf(mine, other), OwnDispatchFilter.apply(listOf(mine, other), ownRealDispatchId = null))
    }

    @Test
    fun `출동 번호를 모르는 차는 빼지 않는다`() {
        val unknown = vehicle("공공 GPS 차", null)

        assertEquals(listOf(unknown), OwnDispatchFilter.apply(listOf(unknown), ownRealDispatchId = "내 출동"))
    }
}
