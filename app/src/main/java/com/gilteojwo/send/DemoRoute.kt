package com.gilteojwo.send

import com.gilteojwo.geo.GeoMath
import com.gilteojwo.geo.GeoPoint

/**
 * 시연용 경로 (사양서 S-4): 미리 정한 경로를 실제 속도로 재생해 송신한다. 화면에는 "시연 모드"를 표시한다.
 * 목표(수신 차)를 기준으로 출발점을 정하고, 정해진 방향·속도로 달려 목표를 지나 300m 더 간 뒤 끝난다.
 * 목표 차는 북쪽을 보고 있다고 본다 (에뮬레이터 기본 진행 방향 0°).
 * - 후방 접근: 남쪽(뒤) 1,200m에서 북쪽으로 시속 60km → 시연 2~7단계 (관심 → 경고 → 양보 → 해제)
 * - 교차로 측면 접근: 서쪽(왼쪽) 800m에서 동쪽으로 시속 50km → 시연 9단계 (정차 중 측면 접근)
 */
enum class DemoRoute(
    val koreanName: String,
    private val startBearingDegrees: Double, // 목표에서 본 출발점 방향
    private val startMeters: Double, // 목표에서 출발점까지
    private val headingDegrees: Double, // 달리는 방향
    private val speedKmh: Double,
) {
    REAR_APPROACH("후방 접근", 180.0, 1200.0, 0.0, 60.0),
    SIDE_CROSSING("교차로 측면 접근", 270.0, 800.0, 90.0, 50.0);

    private val passMeters = 300.0 // 목표를 지나 더 가는 거리

    /** 처음부터 끝까지 걸리는 시간(초) */
    val totalSeconds: Int get() = ((startMeters + passMeters) / (speedKmh / 3.6)).toInt()

    /** 출발하고 elapsedSeconds초 뒤의 위치. 경로가 끝났으면 null */
    fun fixAt(target: GeoPoint, elapsedSeconds: Double): SenderFix? {
        val traveled = speedKmh / 3.6 * elapsedSeconds
        if (traveled > startMeters + passMeters) return null
        val start = GeoMath.destination(target, startBearingDegrees, startMeters)
        val point = GeoMath.destination(start, headingDegrees, traveled)
        return SenderFix(point.latitude, point.longitude, headingDegrees, speedKmh, accuracyMeters = 5.0)
    }

    companion object {
        /** 시연 목표: 에뮬레이터 기본 위치 (성남 시흥동). 수신 차가 여기 있다 */
        val DEMO_TARGET = GeoPoint(37.383537, 126.934173)
    }
}
