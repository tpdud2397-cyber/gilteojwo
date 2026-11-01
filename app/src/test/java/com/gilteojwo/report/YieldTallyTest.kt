package com.gilteojwo.report

import com.gilteojwo.receive.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 R-17: 양보가 확인되면 "오늘 구급차 1대에 길을 열어줬습니다", 기록과 누적 횟수는 차 안에만 저장 */
class YieldTallyTest {
    private fun record(date: String, time: String = "09:00", type: VehicleType = VehicleType.AMBULANCE) = YieldRecord(date, time, type)

    @Test
    fun `같은 날 두 번 양보하면 오늘 2대, 누적 2대`() {
        val tally = YieldTally().add(record("2026-11-01")).add(record("2026-11-01"))

        assertEquals(2, tally.todayCount("2026-11-01"))
        assertEquals(2, tally.total)
    }

    @Test
    fun `날짜가 바뀌면 오늘은 다시 1대부터, 누적은 이어진다`() {
        val tally = YieldTally().add(record("2026-11-01")).add(record("2026-11-01")).add(record("2026-11-02"))

        assertEquals(1, tally.todayCount("2026-11-02"))
        assertEquals(3, tally.total)
    }

    @Test
    fun `양보한 뒤 보여줄 문구`() {
        val tally = YieldTally().add(record("2026-11-01"))

        assertEquals("오늘 구급차 1대에 길을 열어줬습니다", tally.message("2026-11-01", "구급차"))
        assertEquals("오늘 긴급차량 2대에 길을 열어줬습니다 · 누적 2대", tally.add(record("2026-11-01")).message("2026-11-01", "소방차"))
    }

    @Test
    fun `리포트 화면용 날짜별 기록은 최근 날짜부터, 차종별 수와 함께`() {
        val tally = YieldTally()
            .add(record("2026-11-01", "09:10", VehicleType.AMBULANCE))
            .add(record("2026-11-02", "08:00", VehicleType.FIRE_TRUCK))
            .add(record("2026-11-02", "18:30", VehicleType.AMBULANCE))

        val days = tally.days()

        assertEquals(listOf("2026-11-02", "2026-11-01"), days.map { it.date })
        assertEquals("2026-11-02 · 2대 (구급차 1 · 소방차 1)", days[0].text())
    }

    @Test
    fun `차종별 누적`() {
        val tally = YieldTally()
            .add(record("2026-11-01", type = VehicleType.AMBULANCE))
            .add(record("2026-11-01", type = VehicleType.AMBULANCE))
            .add(record("2026-11-02", type = VehicleType.POLICE))

        assertEquals("구급차 2 · 경찰차 1", tally.byTypeText())
    }

    @Test
    fun `기록 한 줄을 글자로 저장했다가 다시 읽는다`() {
        val record = record("2026-11-01", "15:31", VehicleType.FIRE_TRUCK)

        assertEquals(record, YieldRecord.fromLine(record.toLine()))
        assertNull(YieldRecord.fromLine("망가진 줄"))
    }
}
