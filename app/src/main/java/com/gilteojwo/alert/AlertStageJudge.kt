package com.gilteojwo.alert

import com.gilteojwo.approach.ApproachInfo

/**
 * 알림 단계 정하기 (사양서 '알림 단계와 양보 안내').
 * - 관심: 1,000m 안 + 접근 중 / 경고: 500m 안 또는 도착 30초 안 / 양보: 200m 안 또는 도착 15초 안
 * - 해제: 알리던 차가 3초 넘게 '접근 아님'이거나 목록에서 사라짐. 해제는 3초 보여준 뒤 '알림 없음'
 * 위치가 흔들려도 알림이 깜빡이지 않게 두 가지를 지킨다.
 *   ① 단계는 올라가기만 한다 (양보였다가 210m가 돼도 경고로 내려가지 않음)
 *   ② '접근 아님'이 한 번 나왔다고 바로 해제하지 않고 3초 기다린다
 * 긴급차량이 여러 대면 가장 급한 한 대를 알린다. 기준값은 제안값이며 시연 후 조정한다.
 */
class AlertStageJudge {
    private var stage = AlertStage.NONE
    private var targetId: String? = null // 알리고 있는 긴급차량
    private var lastTarget: ApproachInfo? = null // 그 차의 마지막 계산 결과 (해제 화면에도 쓴다)
    private var notApproachingSinceMillis: Long? = null // '접근 아님'이 시작된 시각
    private var clearedAtMillis = 0L

    /** 계산 결과가 새로 나올 때마다(약 1초) 부른다 */
    fun update(results: List<ApproachInfo>, nowMillis: Long): AlertState {
        // 1) 알리고 있는 차를 계속 본다
        if (isAlerting()) {
            val target = results.find { it.vehicle.id == targetId }
            when {
                // 목록에서 사라짐 (긴급출동 종료, 송신 끊김)
                target == null -> clear(nowMillis)
                // 멀어지는 중: 3초 넘게 이어지면 해제
                !target.isApproaching -> {
                    lastTarget = target
                    val since = notApproachingSinceMillis ?: nowMillis.also { notApproachingSinceMillis = it }
                    if (nowMillis - since >= CLEAR_AFTER_MILLIS) clear(nowMillis)
                }
                // 계속 다가오는 중: 단계는 올라가기만 한다
                else -> {
                    lastTarget = target
                    notApproachingSinceMillis = null
                    val current = stageFor(target)
                    if (current.ordinal > stage.ordinal) stage = current
                }
            }
        }

        // 2) 가장 급한 차가 지금 단계보다 급하면 그 차를 알린다 (알림 중이 아니면 새로 시작)
        val mostUrgent = results
            .filter { stageFor(it) != AlertStage.NONE }
            .maxWithOrNull(compareBy<ApproachInfo>({ stageFor(it).ordinal }, { -it.distanceMeters }))
        if (mostUrgent != null && (!isAlerting() || stageFor(mostUrgent).ordinal > stage.ordinal)) {
            start(mostUrgent)
        }

        // 3) 해제는 3초 보여준 뒤 끝낸다
        if (stage == AlertStage.CLEARED && nowMillis - clearedAtMillis >= CLEARED_SHOW_MILLIS) {
            stage = AlertStage.NONE
            lastTarget = null
        }
        return AlertState(stage, if (stage == AlertStage.NONE) null else lastTarget)
    }

    private fun isAlerting() = stage == AlertStage.ATTENTION || stage == AlertStage.WARNING || stage == AlertStage.YIELD

    private fun start(info: ApproachInfo) {
        targetId = info.vehicle.id
        lastTarget = info
        stage = stageFor(info)
        notApproachingSinceMillis = null
    }

    private fun clear(nowMillis: Long) {
        stage = AlertStage.CLEARED
        clearedAtMillis = nowMillis
        targetId = null
        notApproachingSinceMillis = null
    }

    /** 이 긴급차량 하나만 보면 몇 단계인지. 접근 중이 아니면 알림 없음 */
    private fun stageFor(info: ApproachInfo): AlertStage {
        if (!info.isApproaching) return AlertStage.NONE
        val meters = info.distanceMeters
        val seconds = info.arrivalSeconds ?: Int.MAX_VALUE
        return when {
            meters <= YIELD_METERS || seconds <= YIELD_SECONDS -> AlertStage.YIELD
            meters <= WARNING_METERS || seconds <= WARNING_SECONDS -> AlertStage.WARNING
            meters <= ATTENTION_METERS -> AlertStage.ATTENTION
            else -> AlertStage.NONE
        }
    }

    companion object {
        const val ATTENTION_METERS = 1000.0
        const val WARNING_METERS = 500.0
        const val WARNING_SECONDS = 30
        const val YIELD_METERS = 200.0
        const val YIELD_SECONDS = 15
        const val CLEAR_AFTER_MILLIS = 3_000L // '접근 아님'이 3초 이어지면 해제
        const val CLEARED_SHOW_MILLIS = 3_000L // 해제 화면은 3초
    }
}
