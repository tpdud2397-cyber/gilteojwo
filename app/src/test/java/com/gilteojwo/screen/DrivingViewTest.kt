package com.gilteojwo.screen

import com.gilteojwo.alert.AlertStage
import com.gilteojwo.alert.YieldAction
import com.gilteojwo.alert.YieldAdvice
import com.gilteojwo.alert.alertOf
import com.gilteojwo.approach.RelativeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 R-9: 주행 중에는 아이콘 하나와 문구 한 줄만 보여준다 */
class DrivingViewTest {
    @Test
    fun `알림이 없으면 주행 중 알림 화면도 없다`() {
        assertNull(DrivingView.of(alertOf(AlertStage.NONE), advice = null))
    }

    @Test
    fun `관심·경고 단계는 오는 쪽 화살표와 방향·차종·거리 한 줄`() {
        val view = DrivingView.of(alertOf(AlertStage.WARNING, RelativeDirection.REAR_LEFT, meters = 480.4), advice = null)!!

        assertEquals("↙", view.icon)
        assertEquals("좌측 후방 구급차 480m", view.line)
    }

    @Test
    fun `양보 단계는 양보 안내 그림과 문구`() {
        val advice = YieldAdvice("우측으로 비켜주세요", YieldAction.MOVE_RIGHT)

        val view = DrivingView.of(alertOf(AlertStage.YIELD), advice)!!

        assertEquals("→", view.icon)
        assertEquals("우측으로 비켜주세요", view.line)
    }

    @Test
    fun `해제는 확인 표시와 통과 안내`() {
        val view = DrivingView.of(alertOf(AlertStage.CLEARED), YieldAdvice("지나갔습니다", YieldAction.PASSED))!!

        assertEquals("✓", view.icon)
        assertEquals("지나갔습니다", view.line)
    }
}
