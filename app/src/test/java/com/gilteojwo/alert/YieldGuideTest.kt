package com.gilteojwo.alert

import com.gilteojwo.CITY_HALL
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.RelativeDirection
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.VehicleType
import com.gilteojwo.vehiclestate.Gear
import com.gilteojwo.vehiclestate.TurnSignal
import com.gilteojwo.vehiclestate.VehicleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 사양서 '양보 안내' 표. 내 차는 북쪽(0°)으로 향해 있다 */
class YieldGuideTest {
    private val driving = VehicleState(speedKmh = 40.0, gear = Gear.D, brakePressed = false, turnSignal = TurnSignal.OFF)
    private val stoppedWithBrake = VehicleState(speedKmh = 0.0, gear = Gear.D, brakePressed = true, turnSignal = TurnSignal.OFF)

    /** direction 쪽에 있는 긴급차량. vehicleHeading = 긴급차량이 달리는 방향 */
    private fun alert(stage: AlertStage, direction: RelativeDirection, vehicleHeading: Double = 0.0) = AlertState(
        stage,
        ApproachInfo(
            vehicle = EmergencyVehicle("구급차1", VehicleType.AMBULANCE, CITY_HALL, vehicleHeading, 60.0, 0),
            distanceMeters = 150.0,
            direction = direction,
            arrivalSeconds = 9,
            isApproaching = stage != AlertStage.CLEARED,
        ),
    )

    @Test
    fun `달리는 중 뒤에서 오면 우측으로 비켜주세요`() {
        val advice = YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR), 0.0, driving)

        assertEquals("우측으로 비켜주세요", advice?.text)
        assertFalse(advice!!.isConfirmed)
    }

    @Test
    fun `달리는 중 좌측 후방이나 우측 후방에서 와도 우측으로 비켜주세요`() {
        assertEquals("우측으로 비켜주세요", YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR_LEFT), 0.0, driving)?.text)
        assertEquals("우측으로 비켜주세요", YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR_RIGHT), 0.0, driving)?.text)
    }

    @Test
    fun `달리는 중 앞이나 옆에서 오면 속도를 줄이고 진로를 비켜주세요`() {
        val text = "속도를 줄이고 진로를 비켜주세요"
        assertEquals(text, YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.FRONT, 180.0), 0.0, driving)?.text)
        assertEquals(text, YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.LEFT, 90.0), 0.0, driving)?.text)
    }

    @Test
    fun `서서 브레이크를 밟고 있는데 옆에서 가로질러 오면 교차로에 진입하지 마세요`() {
        // 왼쪽에서 동쪽(90°)으로 달려온다: 내 진행 방향(북쪽)과 직각으로 교차
        val advice = YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.LEFT, 90.0), 0.0, stoppedWithBrake)

        assertEquals("녹색 신호여도 교차로에 진입하지 마세요", advice?.text)
    }

    @Test
    fun `서 있어도 뒤에서 같은 방향으로 오면 우측으로 비켜주세요`() {
        val advice = YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR, 0.0), 0.0, stoppedWithBrake)

        assertEquals("우측으로 비켜주세요", advice?.text)
    }

    @Test
    fun `양보 단계에서 우측 방향지시등을 켜면 확인했습니다`() {
        val signaling = driving.copy(turnSignal = TurnSignal.RIGHT)
        val advice = YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR), 0.0, signaling)

        assertEquals("확인했습니다", advice?.text)
        assertTrue(advice!!.isConfirmed)
    }

    @Test
    fun `지나간 뒤에는 지나갔습니다, 서 있으면 출발하셔도 됩니다까지`() {
        assertEquals("지나갔습니다", YieldGuide.choose(alert(AlertStage.CLEARED, RelativeDirection.FRONT), 0.0, driving)?.text)
        assertEquals(
            "지나갔습니다. 출발하셔도 됩니다",
            YieldGuide.choose(alert(AlertStage.CLEARED, RelativeDirection.FRONT), 0.0, stoppedWithBrake)?.text,
        )
    }

    @Test
    fun `양보 단계라도 긴급차량이 막 지나가 접근 중이 아니면 새 안내를 하지 않는다`() {
        // 해제까지 3초를 기다리는 동안: 구급차가 이제 앞에 있지만 비키라고 하면 안 된다
        val passing = alert(AlertStage.YIELD, RelativeDirection.FRONT).let {
            it.copy(target = it.target!!.copy(isApproaching = false, arrivalSeconds = null))
        }

        assertNull(YieldGuide.choose(passing, 0.0, driving))
    }

    @Test
    fun `양보 문구마다 화면에 크게 보여줄 그림 종류가 정해진다`() {
        val signaling = driving.copy(turnSignal = TurnSignal.RIGHT)
        assertEquals(YieldAction.MOVE_RIGHT, YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR), 0.0, driving)?.action)
        assertEquals(YieldAction.SLOW_DOWN, YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.FRONT, 180.0), 0.0, driving)?.action)
        assertEquals(
            YieldAction.STOP_BEFORE_CROSSING,
            YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.LEFT, 90.0), 0.0, stoppedWithBrake)?.action,
        )
        assertEquals(YieldAction.CONFIRMED, YieldGuide.choose(alert(AlertStage.YIELD, RelativeDirection.REAR), 0.0, signaling)?.action)
        assertEquals(YieldAction.PASSED, YieldGuide.choose(alert(AlertStage.CLEARED, RelativeDirection.FRONT), 0.0, driving)?.action)
    }

    @Test
    fun `긴급차량 정보가 끊겨 해제되면 지나갔다고 하지 않고 사이렌에 주의하라고 한다`() {
        val lost = alert(AlertStage.CLEARED, RelativeDirection.REAR).copy(signalLost = true)
        val advice = YieldGuide.choose(lost, 0.0, stoppedWithBrake)

        assertEquals("긴급차량 정보가 끊겼습니다. 사이렌에 주의하세요", advice?.text)
        assertEquals(YieldAction.SIGNAL_LOST, advice?.action)
    }

    @Test
    fun `관심·경고 단계에는 양보 문구가 없다`() {
        assertNull(YieldGuide.choose(alert(AlertStage.ATTENTION, RelativeDirection.REAR), 0.0, driving))
        assertNull(YieldGuide.choose(alert(AlertStage.WARNING, RelativeDirection.REAR), 0.0, driving))
    }
}
