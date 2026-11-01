package com.gilteojwo.alert

import com.gilteojwo.CITY_HALL
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.RelativeDirection
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 '알림 단계와 양보 안내': 관심 1,000m / 경고 500m·30초 / 양보 200m·15초 / 해제 */
class AlertStageJudgeTest {
    private val judge = AlertStageJudge()

    private fun info(meters: Double, arrivalSeconds: Int?, approaching: Boolean, id: String = "구급차1") = ApproachInfo(
        vehicle = EmergencyVehicle(id, VehicleType.AMBULANCE, CITY_HALL, 0.0, 60.0, 0),
        distanceMeters = meters,
        direction = RelativeDirection.REAR,
        arrivalSeconds = arrivalSeconds,
        isApproaching = approaching,
    )

    @Test
    fun `1,000m 안에서 접근 중이면 관심`() {
        assertEquals(AlertStage.ATTENTION, judge.update(listOf(info(900.0, 60, true)), 0).stage)
    }

    @Test
    fun `1,000m 밖이면 접근 중이어도 알림 없음`() {
        val state = judge.update(listOf(info(1200.0, 70, true)), 0)

        assertEquals(AlertStage.NONE, state.stage)
        assertNull(state.target)
    }

    @Test
    fun `접근 중이 아니면 가까워도 알림 없음`() {
        assertEquals(AlertStage.NONE, judge.update(listOf(info(100.0, null, false)), 0).stage)
    }

    @Test
    fun `500m 안이거나 도착 30초 안이면 경고`() {
        assertEquals(AlertStage.WARNING, AlertStageJudge().update(listOf(info(450.0, 40, true)), 0).stage)
        assertEquals(AlertStage.WARNING, AlertStageJudge().update(listOf(info(700.0, 25, true)), 0).stage)
    }

    @Test
    fun `200m 안이거나 도착 15초 안이면 양보`() {
        assertEquals(AlertStage.YIELD, AlertStageJudge().update(listOf(info(180.0, 20, true)), 0).stage)
        assertEquals(AlertStage.YIELD, AlertStageJudge().update(listOf(info(300.0, 12, true)), 0).stage)
    }

    @Test
    fun `한 번 양보 단계가 되면 거리가 조금 늘어도 경고로 내려가지 않는다`() {
        judge.update(listOf(info(190.0, 14, true)), 0)
        val state = judge.update(listOf(info(210.0, 16, true)), 1000)

        assertEquals(AlertStage.YIELD, state.stage)
    }

    @Test
    fun `접근 아님이 잠깐 나왔다가 다시 접근 중이면 해제하지 않는다`() {
        judge.update(listOf(info(190.0, 14, true)), 0)
        judge.update(listOf(info(180.0, null, false)), 1000) // 위치가 흔들려 한 번 '접근 아님'
        val state = judge.update(listOf(info(160.0, 11, true)), 2000)

        assertEquals(AlertStage.YIELD, state.stage)
    }

    @Test
    fun `접근 아님이 3초 이어지면 해제`() {
        judge.update(listOf(info(50.0, 3, true)), 0)
        judge.update(listOf(info(20.0, null, false)), 1000) // 지나감
        judge.update(listOf(info(40.0, null, false)), 2000)
        val state = judge.update(listOf(info(60.0, null, false)), 4000)

        assertEquals(AlertStage.CLEARED, state.stage)
    }

    @Test
    fun `해제는 3초 동안 보여준 뒤 알림 없음으로 돌아간다`() {
        judge.update(listOf(info(50.0, 3, true)), 0)
        judge.update(listOf(info(20.0, null, false)), 1000)
        judge.update(listOf(info(60.0, null, false)), 4000) // 해제
        val shown = judge.update(listOf(info(80.0, null, false)), 6000)
        val after = judge.update(listOf(info(120.0, null, false)), 7000)

        assertEquals(AlertStage.CLEARED, shown.stage)
        assertEquals(AlertStage.NONE, after.stage)
    }

    @Test
    fun `알림 중이던 긴급차량이 목록에서 사라지면 해제`() {
        judge.update(listOf(info(400.0, 25, true)), 0)
        val state = judge.update(emptyList(), 1000)

        assertEquals(AlertStage.CLEARED, state.stage)
    }

    @Test
    fun `더 급한 긴급차량이 오면 그 차로 바꾼다`() {
        judge.update(listOf(info(900.0, 60, true, id = "먼 구급차")), 0)
        val state = judge.update(
            listOf(info(880.0, 58, true, id = "먼 구급차"), info(150.0, 9, true, id = "가까운 소방차")),
            1000,
        )

        assertEquals(AlertStage.YIELD, state.stage)
        assertEquals("가까운 소방차", state.target?.vehicle?.id)
    }
}
