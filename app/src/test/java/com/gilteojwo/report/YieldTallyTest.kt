package com.gilteojwo.report

import org.junit.Assert.assertEquals
import org.junit.Test

/** 사양서 R-17: 양보가 확인되면 "오늘 구급차 1대에 길을 열어줬습니다", 누적 횟수는 차 안에만 저장 */
class YieldTallyTest {
    @Test
    fun `같은 날 두 번 양보하면 오늘 2대, 누적 2대`() {
        val tally = YieldTally().add("2026-11-01").add("2026-11-01")

        assertEquals(2, tally.todayCount("2026-11-01"))
        assertEquals(2, tally.total)
    }

    @Test
    fun `날짜가 바뀌면 오늘은 다시 1대부터, 누적은 이어진다`() {
        val tally = YieldTally().add("2026-11-01").add("2026-11-01").add("2026-11-02")

        assertEquals(1, tally.todayCount("2026-11-02"))
        assertEquals(3, tally.total)
    }

    @Test
    fun `양보한 뒤 보여줄 문구`() {
        val tally = YieldTally().add("2026-11-01")

        assertEquals("오늘 구급차 1대에 길을 열어줬습니다", tally.message("2026-11-01", "구급차"))
        assertEquals("오늘 긴급차량 2대에 길을 열어줬습니다 · 누적 2대", tally.add("2026-11-01").message("2026-11-01", "소방차"))
    }
}
