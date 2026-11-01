package com.gilteojwo.alert

import com.gilteojwo.alert.ScreenEdge.BOTTOM
import com.gilteojwo.alert.ScreenEdge.LEFT
import com.gilteojwo.alert.ScreenEdge.TOP
import com.gilteojwo.approach.RelativeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 화면 가장자리 빛: 오는 쪽 면에, 가까울수록 진하게 (11/01 사용자 그림) */
class EdgeGlowTest {
    @Test
    fun `뒤에서 오면 아래쪽 면만 켠다`() {
        assertEquals(setOf(BOTTOM), EdgeGlow.of(alertOf(AlertStage.WARNING, RelativeDirection.REAR)).edges)
    }

    @Test
    fun `대각선에서 오면 두 면을 켠다`() {
        assertEquals(setOf(BOTTOM, LEFT), EdgeGlow.of(alertOf(AlertStage.WARNING, RelativeDirection.REAR_LEFT)).edges)
        assertEquals(setOf(TOP, LEFT), EdgeGlow.of(alertOf(AlertStage.WARNING, RelativeDirection.FRONT_LEFT)).edges)
    }

    @Test
    fun `1,000m는 가장 흐리고 200m 안은 가장 진하고 600m는 그 중간`() {
        assertEquals(0.15f, EdgeGlow.of(alertOf(AlertStage.ATTENTION, meters = 1000.0)).strength, 0.01f)
        assertEquals(1.0f, EdgeGlow.of(alertOf(AlertStage.YIELD, meters = 150.0)).strength, 0.01f)
        assertEquals(0.5f, EdgeGlow.of(alertOf(AlertStage.WARNING, meters = 600.0)).strength, 0.01f)
    }

    @Test
    fun `양보 단계에서만 깜빡인다`() {
        assertTrue(EdgeGlow.of(alertOf(AlertStage.YIELD, meters = 150.0)).blinking)
        assertFalse(EdgeGlow.of(alertOf(AlertStage.WARNING, meters = 400.0)).blinking)
    }

    @Test
    fun `알림 없음이나 해제에는 빛을 끈다`() {
        assertEquals(EdgeGlow.OFF, EdgeGlow.of(AlertState(AlertStage.NONE, null)))
        assertEquals(EdgeGlow.OFF, EdgeGlow.of(alertOf(AlertStage.CLEARED, approaching = false)))
    }
}
