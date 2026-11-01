package com.gilteojwo.alert

import com.gilteojwo.approach.RelativeDirection

/** 화면(내비 칸)의 네 가장자리 */
enum class ScreenEdge { TOP, RIGHT, BOTTOM, LEFT }

/**
 * 화면 가장자리 빛 (사양서 R-10·R-11, 11/01 사용자 그림).
 * 긴급차량이 오는 쪽 면에 얇은 붉은 빛을 켠다. 대각선 방향은 두 면 (예: 좌측 후방 → 아래 + 왼쪽).
 * 멀수록 흐리고 가까울수록 진하다: 1,000m에서 0.15, 200m 안에서 1.0. 양보 단계에서는 깜빡인다.
 */
data class EdgeGlow(val edges: Set<ScreenEdge>, val strength: Float, val blinking: Boolean) {
    companion object {
        val OFF = EdgeGlow(emptySet(), 0f, false)
        private const val FAINTEST = 0.15f // 1,000m에서의 진하기
        private const val FAR_METERS = 1000.0 // 이보다 멀면 가장 흐리게
        private const val NEAR_METERS = 200.0 // 이보다 가까우면 가장 진하게

        fun of(alert: AlertState): EdgeGlow {
            val target = alert.target ?: return OFF
            val isAlerting = alert.stage == AlertStage.ATTENTION ||
                alert.stage == AlertStage.WARNING ||
                alert.stage == AlertStage.YIELD
            if (!isAlerting) return OFF

            val closeness = (FAR_METERS - target.distanceMeters) / (FAR_METERS - NEAR_METERS)
            val strength = closeness.toFloat().coerceIn(FAINTEST, 1f)
            return EdgeGlow(edgesFor(target.direction), strength, blinking = alert.stage == AlertStage.YIELD)
        }

        /** 오는 쪽 → 켤 면. 시계로 12시 = 위, 3시 = 오른쪽, 6시 = 아래, 9시 = 왼쪽 */
        private fun edgesFor(direction: RelativeDirection): Set<ScreenEdge> = when (direction) {
            RelativeDirection.FRONT -> setOf(ScreenEdge.TOP)
            RelativeDirection.FRONT_RIGHT -> setOf(ScreenEdge.TOP, ScreenEdge.RIGHT)
            RelativeDirection.RIGHT -> setOf(ScreenEdge.RIGHT)
            RelativeDirection.REAR_RIGHT -> setOf(ScreenEdge.RIGHT, ScreenEdge.BOTTOM)
            RelativeDirection.REAR -> setOf(ScreenEdge.BOTTOM)
            RelativeDirection.REAR_LEFT -> setOf(ScreenEdge.BOTTOM, ScreenEdge.LEFT)
            RelativeDirection.LEFT -> setOf(ScreenEdge.LEFT)
            RelativeDirection.FRONT_LEFT -> setOf(ScreenEdge.TOP, ScreenEdge.LEFT)
        }
    }
}
