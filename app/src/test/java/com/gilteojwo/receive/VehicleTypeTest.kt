package com.gilteojwo.receive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleTypeTest {
    @Test
    fun `서버의 ambulance는 구급차`() {
        assertEquals(VehicleType.AMBULANCE, VehicleType.fromCode("ambulance"))
    }

    @Test
    fun `서버의 fire_truck은 소방차`() {
        assertEquals(VehicleType.FIRE_TRUCK, VehicleType.fromCode("fire_truck"))
    }

    @Test
    fun `모르는 종류는 null`() {
        assertNull(VehicleType.fromCode("taxi"))
    }
}
