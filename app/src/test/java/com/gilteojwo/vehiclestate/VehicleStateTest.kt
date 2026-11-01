package com.gilteojwo.vehiclestate

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleStateTest {
    @Test
    fun `안드로이드 기어 숫자를 P·R·N·D로 바꾼다`() {
        assertEquals(Gear.P, Gear.fromAndroid(4))
        assertEquals(Gear.R, Gear.fromAndroid(2))
        assertEquals(Gear.N, Gear.fromAndroid(1))
        assertEquals(Gear.D, Gear.fromAndroid(8))
    }

    @Test
    fun `차가 주행 제한을 걸면 주행 중, 풀면 서 있음 (R-9)`() {
        // 에뮬레이터: 기어가 P를 벗어나면 주행 제한이 걸린다 (11/01 시험)
        assertEquals(true, VehicleState(gear = Gear.D, drivingRestricted = true).isDriving)
        assertEquals(false, VehicleState(gear = Gear.P, drivingRestricted = false).isDriving)
    }

    @Test
    fun `주행 제한을 못 읽으면 기어 D·R이거나 움직이면 주행 중`() {
        assertEquals(true, VehicleState(gear = Gear.D).isDriving)
        assertEquals(true, VehicleState(speedKmh = 12.0).isDriving)
        assertEquals(false, VehicleState(gear = Gear.P, speedKmh = 0.0).isDriving)
    }

    @Test
    fun `모르는 기어 값은 확인 중으로 둔다`() {
        assertEquals(Gear.UNKNOWN, Gear.fromAndroid(null))
        assertEquals(Gear.UNKNOWN, Gear.fromAndroid(16))
    }

    @Test
    fun `내 차 상태를 한 줄로 보여준다`() {
        val state = VehicleState(speedKmh = 14.4, gear = Gear.D, brakePressed = false, turnSignal = TurnSignal.RIGHT)

        assertEquals("14 km/h · 기어 D · 브레이크 안 밟음 · 방향지시등 오른쪽", state.describe())
    }

    @Test
    fun `아직 받지 못한 값은 확인 중으로 보여준다`() {
        assertEquals("속도 확인 중 · 기어 확인 중 · 브레이크 확인 중 · 방향지시등 확인 중", VehicleState().describe())
    }
}
