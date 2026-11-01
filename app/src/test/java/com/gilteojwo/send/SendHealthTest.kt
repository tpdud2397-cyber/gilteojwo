package com.gilteojwo.send

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 사양서 S-6: 마지막 송신 시각을 보여주고, 5초 넘게 실패하면 경고한다 */
class SendHealthTest {
    @Test
    fun `시작한 지 5초 안이고 아직 한 번도 못 보냈으면 준비 중`() {
        val state = SendHealth.of(lastSuccessMillis = null, startedMillis = 0, nowMillis = 3000)

        assertFalse(state.isWarning)
        assertEquals("송신 준비 중", state.text)
    }

    @Test
    fun `마지막 성공이 2초 전이면 정상`() {
        val state = SendHealth.of(lastSuccessMillis = 8000, startedMillis = 0, nowMillis = 10_000)

        assertFalse(state.isWarning)
        assertEquals("정상 송신 중 · 2초 전", state.text)
    }

    @Test
    fun `5초 넘게 성공이 없으면 경고`() {
        val state = SendHealth.of(lastSuccessMillis = 3000, startedMillis = 0, nowMillis = 10_000)

        assertTrue(state.isWarning)
        assertEquals("7초째 송신 실패 · 인터넷과 위치를 확인하세요", state.text)
    }

    @Test
    fun `시작하고 5초 넘게 한 번도 못 보냈어도 경고`() {
        val state = SendHealth.of(lastSuccessMillis = null, startedMillis = 0, nowMillis = 6000)

        assertTrue(state.isWarning)
        assertEquals("6초째 송신 실패 · 인터넷과 위치를 확인하세요", state.text)
    }
}

class RegCodeTest {
    @Test
    fun `등록 코드 앞뒤 빈칸은 없앤다`() {
        assertEquals("abc-123", RegCode.normalize("  abc-123 \n"))
    }

    @Test
    fun `빈 등록 코드는 받지 않는다`() {
        assertNull(RegCode.normalize("   "))
    }
}
