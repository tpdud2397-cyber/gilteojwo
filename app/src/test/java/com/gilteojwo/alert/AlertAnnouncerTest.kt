package com.gilteojwo.alert

import com.gilteojwo.approach.RelativeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 '알림 단계' 표의 소리: 관심 알림음 1회 / 경고 오는 쪽 알림음 + 음성 / 양보 음성 / 해제 완료음 */
class AlertAnnouncerTest {
    private val announcer = AlertAnnouncer()
    private val moveRight = YieldAdvice("우측으로 비켜주세요")

    @Test
    fun `관심이 되면 알림음만 한 번`() {
        val result = announcer.next(alertOf(AlertStage.ATTENTION, RelativeDirection.REAR, 900.0), null, 0)

        assertEquals(Announcement(SoundCue.BOTH, null), result)
    }

    @Test
    fun `경고가 되면 오는 쪽 스피커 알림음과 방향·거리 음성`() {
        val result = announcer.next(alertOf(AlertStage.WARNING, RelativeDirection.REAR_LEFT, 430.0), null, 0)

        assertEquals(Announcement(SoundCue.LEFT, "왼쪽 뒤에서 구급차, 약 400미터"), result)
    }

    @Test
    fun `같은 단계가 이어지면 다시 알리지 않는다`() {
        announcer.next(alertOf(AlertStage.WARNING, meters = 430.0), null, 0)

        assertNull(announcer.next(alertOf(AlertStage.WARNING, meters = 410.0), null, 1000))
    }

    @Test
    fun `양보가 되면 양보 문구를 말하고 10초마다 되풀이한다`() {
        val first = announcer.next(alertOf(AlertStage.YIELD, meters = 190.0), moveRight, 0)
        val quiet = announcer.next(alertOf(AlertStage.YIELD, meters = 110.0), moveRight, 5000)
        val again = announcer.next(alertOf(AlertStage.YIELD, meters = 30.0), moveRight, 10000)

        assertEquals(Announcement(null, "우측으로 비켜주세요"), first)
        assertNull(quiet)
        assertEquals(Announcement(null, "우측으로 비켜주세요"), again)
    }

    @Test
    fun `양보를 확인하면 확인했습니다를 한 번 말하고 되풀이를 멈춘다`() {
        val confirmed = YieldAdvice("확인했습니다", isConfirmed = true)
        announcer.next(alertOf(AlertStage.YIELD, meters = 190.0), moveRight, 0)
        val said = announcer.next(alertOf(AlertStage.YIELD, meters = 150.0), confirmed, 2000)
        val later = announcer.next(alertOf(AlertStage.YIELD, meters = 30.0), confirmed, 15000)

        assertEquals(Announcement(null, "확인했습니다"), said)
        assertNull(later)
    }

    @Test
    fun `해제가 되면 완료음과 지나갔습니다`() {
        announcer.next(alertOf(AlertStage.YIELD, meters = 50.0), moveRight, 0)
        val result = announcer.next(alertOf(AlertStage.CLEARED, approaching = false), YieldAdvice("지나갔습니다"), 4000)

        assertEquals(Announcement(SoundCue.DONE, "지나갔습니다"), result)
    }

    @Test
    fun `알림이 끝난 뒤 새 긴급차량이 오면 다시 알린다`() {
        announcer.next(alertOf(AlertStage.WARNING, meters = 430.0), null, 0)
        announcer.next(AlertState(AlertStage.NONE, null), null, 10000)
        val result = announcer.next(alertOf(AlertStage.WARNING, RelativeDirection.RIGHT, 480.0), null, 20000)

        assertEquals(Announcement(SoundCue.RIGHT, "오른쪽에서 구급차, 약 500미터"), result)
    }

    @Test
    fun `100미터 안이면 거리 대신 곧 도착이라고 말한다`() {
        val result = announcer.next(alertOf(AlertStage.WARNING, RelativeDirection.FRONT, 80.0), null, 0)

        assertEquals("앞에서 구급차, 곧 도착", result?.speech)
    }
}
