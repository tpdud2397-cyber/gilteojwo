package com.gilteojwo.alert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 R-15: 관심 주황, 경고 붉은색, 양보 붉은색 깜빡임, 해제되면 원래대로 */
class AmbientCueTest {
    @Test
    fun `관심은 주황`() {
        assertEquals(AmbientCue(AmbientCue.ORANGE, blinking = false), AmbientCue.of(AlertStage.ATTENTION))
    }

    @Test
    fun `경고는 빨강`() {
        assertEquals(AmbientCue(AmbientCue.RED, blinking = false), AmbientCue.of(AlertStage.WARNING))
    }

    @Test
    fun `양보는 빨강 깜빡임`() {
        assertEquals(AmbientCue(AmbientCue.RED, blinking = true), AmbientCue.of(AlertStage.YIELD))
    }

    @Test
    fun `알림 없음과 해제는 원래 조명으로 되돌린다`() {
        assertNull(AmbientCue.of(AlertStage.NONE))
        assertNull(AmbientCue.of(AlertStage.CLEARED))
    }
}
