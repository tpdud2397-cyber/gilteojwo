package com.gilteojwo.alert

import org.junit.Assert.assertEquals
import org.junit.Test

/** 사양서 '설정' 화면의 알림 거리: 몇 m 안으로 다가오면 '관심' 알림을 시작할지 */
class AlertDistanceTest {
    @Test
    fun `기본은 1,000m`() {
        assertEquals(1000.0, AlertDistance.DEFAULT.meters, 0.0)
    }

    @Test
    fun `저장된 이름이 없거나 모르는 이름이면 기본값`() {
        assertEquals(AlertDistance.DEFAULT, AlertDistance.fromName(null))
        assertEquals(AlertDistance.DEFAULT, AlertDistance.fromName("없는 값"))
        assertEquals(AlertDistance.LONG, AlertDistance.fromName("LONG"))
    }
}
