package com.gilteojwo.alert

import com.gilteojwo.vehiclestate.Gear
import com.gilteojwo.vehiclestate.TurnSignal
import com.gilteojwo.vehiclestate.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 R-16: 알림 중 방향지시등·감속·정차를 보고 양보를 자동 판단한다. 버튼은 없다 */
class YieldDetectorTest {
    private val detector = YieldDetector()
    private val driving50 = VehicleState(speedKmh = 50.0, gear = Gear.D, brakePressed = false, turnSignal = TurnSignal.OFF)

    @Test
    fun `알림이 시작되면 알림 받음을 한 번만 보낸다`() {
        assertEquals(YieldStatus.NOTIFIED, detector.update(alertOf(AlertStage.ATTENTION), driving50))
        assertNull(detector.update(alertOf(AlertStage.WARNING), driving50))
    }

    @Test
    fun `알림 중 우측 방향지시등을 켜면 양보함을 한 번만 보낸다`() {
        detector.update(alertOf(AlertStage.WARNING), driving50)
        val signaling = driving50.copy(turnSignal = TurnSignal.RIGHT)

        assertEquals(YieldStatus.YIELDED, detector.update(alertOf(AlertStage.YIELD), signaling))
        assertNull(detector.update(alertOf(AlertStage.YIELD), signaling))
    }

    @Test
    fun `알림이 시작될 때보다 시속 15km 넘게 줄이면 양보함`() {
        detector.update(alertOf(AlertStage.WARNING), driving50)

        assertNull(detector.update(alertOf(AlertStage.WARNING), driving50.copy(speedKmh = 40.0)))
        assertEquals(YieldStatus.YIELDED, detector.update(alertOf(AlertStage.YIELD), driving50.copy(speedKmh = 30.0)))
    }

    @Test
    fun `서서 브레이크를 밟고 있으면 양보함`() {
        detector.update(alertOf(AlertStage.WARNING), driving50)
        val stopped = driving50.copy(speedKmh = 0.0, brakePressed = true)

        assertEquals(YieldStatus.YIELDED, detector.update(alertOf(AlertStage.YIELD), stopped))
    }

    @Test
    fun `주차(P) 중에 브레이크를 밟고 있는 것은 양보로 세지 않는다`() {
        val parked = driving50.copy(speedKmh = 0.0, gear = Gear.P, brakePressed = true)
        detector.update(alertOf(AlertStage.WARNING), parked)

        assertNull(detector.update(alertOf(AlertStage.YIELD), parked))
    }

    @Test
    fun `같은 구급차라도 새 출동이면 다시 알림 받음과 양보함을 보낸다`() {
        val signaling = driving50.copy(turnSignal = TurnSignal.RIGHT)
        detector.update(alertOf(AlertStage.WARNING, dispatchId = "출동1"), driving50)
        detector.update(alertOf(AlertStage.YIELD, dispatchId = "출동1"), signaling)

        assertEquals(YieldStatus.NOTIFIED, detector.update(alertOf(AlertStage.WARNING, dispatchId = "출동2"), driving50))
        assertEquals(YieldStatus.YIELDED, detector.update(alertOf(AlertStage.YIELD, dispatchId = "출동2"), signaling))
    }

    @Test
    fun `알림이 아닐 때는 아무것도 보내지 않는다`() {
        val signaling = driving50.copy(turnSignal = TurnSignal.RIGHT)

        assertNull(detector.update(AlertState(AlertStage.NONE, null), signaling))
        assertNull(detector.update(alertOf(AlertStage.CLEARED, approaching = false), signaling))
    }
}
