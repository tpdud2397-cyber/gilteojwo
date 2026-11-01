package com.gilteojwo.alert

/**
 * 알림 거리 (사양서 '화면 목록'의 수신 설정): 긴급차량이 몇 m 안으로 다가오면 '관심' 알림을 시작할지.
 * 경고(500m·30초)와 양보(200m·15초) 기준은 바꾸지 않는다. 값은 제안값이다.
 * 도심처럼 긴급차량이 많은 곳은 짧게, 고속도로처럼 빠른 곳은 길게 쓴다.
 */
enum class AlertDistance(val meters: Double, val label: String) {
    SHORT(700.0, "700m"),
    NORMAL(1000.0, "1,000m (기본)"),
    LONG(1500.0, "1,500m");

    companion object {
        val DEFAULT = NORMAL

        /** 저장해 둔 이름으로 찾는다. 없거나 모르는 이름이면 기본값 */
        fun fromName(name: String?): AlertDistance = entries.find { it.name == name } ?: DEFAULT
    }
}
