package com.gilteojwo.alert

import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.RelativeDirection
import kotlin.math.roundToInt

/** 낼 소리: 알림음을 어느 쪽 스피커에서 낼지, 또는 해제 때의 완료음 */
enum class SoundCue { LEFT, RIGHT, BOTH, DONE }

/** 한 번에 낼 것: 소리(없을 수 있음)와 음성 문장(없을 수 있음) */
data class Announcement(val sound: SoundCue?, val speech: String?)

/**
 * 언제 무슨 소리를 낼지 정한다 (사양서 '알림 단계와 양보 안내'의 소리 칸, R-12).
 * - 관심이 되면: 알림음 1회 (오는 쪽 스피커)
 * - 경고가 되면: 오는 쪽 알림음 + "왼쪽 뒤에서 구급차, 약 400미터"
 * - 양보: 양보 문구를 말하고 10초마다 되풀이. 문구가 바뀌면 바로 새 문구. "확인했습니다"는 한 번만
 * - 해제가 되면: 완료음 + "지나갔습니다"
 * 같은 단계가 이어지는 동안에는 되풀이하지 않는다 (계산은 1초마다 오지만 소리는 단계가 바뀔 때만).
 */
class AlertAnnouncer {
    private var lastStage = AlertStage.NONE
    private var lastSpeech: String? = null
    private var lastSpokenAtMillis = 0L

    /** 계산 결과가 새로 나올 때마다 부른다. 낼 것이 없으면 null */
    fun next(alert: AlertState, advice: YieldAdvice?, nowMillis: Long): Announcement? {
        val stageChanged = alert.stage != lastStage
        lastStage = alert.stage
        val target = alert.target

        return when (alert.stage) {
            AlertStage.NONE -> {
                lastSpeech = null
                null
            }
            AlertStage.ATTENTION ->
                if (stageChanged && target != null) Announcement(soundFor(target.direction), null) else null
            AlertStage.WARNING ->
                if (stageChanged && target != null) say(soundFor(target.direction), warningSpeech(target), nowMillis) else null
            AlertStage.YIELD -> yieldAnnouncement(advice, nowMillis)
            AlertStage.CLEARED ->
                if (stageChanged) say(SoundCue.DONE, advice?.text ?: "지나갔습니다", nowMillis) else null
        }
    }

    /** 양보 문구: 새 문구면 바로, 같은 문구면 10초마다. 확인된 뒤에는 되풀이하지 않는다 */
    private fun yieldAnnouncement(advice: YieldAdvice?, nowMillis: Long): Announcement? {
        val text = advice?.text ?: return null // 막 지나가는 중이라 안내가 없을 때
        val isNewText = text != lastSpeech
        val repeatDue = !advice.isConfirmed && nowMillis - lastSpokenAtMillis >= YIELD_REPEAT_MILLIS
        return if (isNewText || repeatDue) say(null, text, nowMillis) else null
    }

    private fun say(sound: SoundCue?, speech: String, nowMillis: Long): Announcement {
        lastSpeech = speech
        lastSpokenAtMillis = nowMillis
        return Announcement(sound, speech)
    }

    /** 예: "왼쪽 뒤에서 구급차, 약 400미터". 거리는 100m 단위, 100m 안이면 "곧 도착" */
    private fun warningSpeech(target: ApproachInfo): String {
        val meters = (target.distanceMeters / 100).roundToInt() * 100
        val distance = if (target.distanceMeters < 100) "곧 도착" else "약 ${meters}미터"
        return "${spokenFrom(target.direction)} ${target.vehicle.type.koreanName}, $distance"
    }

    /** 말로 하는 방향: 듣기 쉽게 전방·후방 대신 앞·뒤로 */
    private fun spokenFrom(direction: RelativeDirection): String = when (direction) {
        RelativeDirection.FRONT -> "앞에서"
        RelativeDirection.FRONT_RIGHT -> "오른쪽 앞에서"
        RelativeDirection.RIGHT -> "오른쪽에서"
        RelativeDirection.REAR_RIGHT -> "오른쪽 뒤에서"
        RelativeDirection.REAR -> "뒤에서"
        RelativeDirection.REAR_LEFT -> "왼쪽 뒤에서"
        RelativeDirection.LEFT -> "왼쪽에서"
        RelativeDirection.FRONT_LEFT -> "왼쪽 앞에서"
    }

    /** 알림음은 긴급차량이 오는 쪽 스피커에서. 앞이나 뒤면 양쪽 */
    private fun soundFor(direction: RelativeDirection): SoundCue = when (direction) {
        RelativeDirection.LEFT, RelativeDirection.FRONT_LEFT, RelativeDirection.REAR_LEFT -> SoundCue.LEFT
        RelativeDirection.RIGHT, RelativeDirection.FRONT_RIGHT, RelativeDirection.REAR_RIGHT -> SoundCue.RIGHT
        RelativeDirection.FRONT, RelativeDirection.REAR -> SoundCue.BOTH
    }

    companion object {
        const val YIELD_REPEAT_MILLIS = 10_000L // 양보 문구는 10초마다 되풀이 (제안값)
    }
}
