package com.gilteojwo.send

import org.junit.Assert.assertEquals
import org.junit.Test

/** 사양서 S-7: 60분 넘게 켜져 있으면 "아직 출동 중인가요?"를 묻고, 2분 안에 답이 없으면 자동 종료 */
class DispatchCheckTest {
    private val minute = 60_000L

    @Test
    fun `60분이 안 됐으면 묻지 않는다`() {
        assertEquals(DispatchCheck.Ok, DispatchCheck.of(lastConfirmedMillis = 0, nowMillis = 59 * minute))
    }

    @Test
    fun `60분이 지나면 묻고 남은 답 시간을 알려준다`() {
        assertEquals(DispatchCheck.Asking(secondsLeft = 90), DispatchCheck.of(lastConfirmedMillis = 0, nowMillis = 60 * minute + 30_000))
    }

    @Test
    fun `물은 뒤 2분 안에 답이 없으면 자동 종료`() {
        assertEquals(DispatchCheck.Expired, DispatchCheck.of(lastConfirmedMillis = 0, nowMillis = 62 * minute + 1))
    }

    @Test
    fun `출동 중을 누르면 그때부터 다시 60분`() {
        assertEquals(DispatchCheck.Ok, DispatchCheck.of(lastConfirmedMillis = 61 * minute, nowMillis = 100 * minute))
    }

    @Test
    fun `시험용으로 시간을 줄일 수 있다`() {
        assertEquals(
            DispatchCheck.Asking(secondsLeft = 5),
            DispatchCheck.of(lastConfirmedMillis = 0, nowMillis = 15_000, checkAfterMillis = 10_000, answerWithinMillis = 10_000),
        )
    }
}
