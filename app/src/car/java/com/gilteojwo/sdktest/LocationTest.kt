package com.gilteojwo.sdktest

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper

/**
 * ① 내 위치 시험
 * Pleos FusedLocation SDK와 안드로이드 기본 위치(LocationManager)를 나란히 받는다.
 * 에뮬레이터에서 위치를 바꿨을 때 어느 쪽 숫자가 따라 바뀌는지 본다.
 */
class LocationTest(context: Context, private val card: TestCard) : SdkTest {
    private val fusedLocation = FusedLocation(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var pleosText = "Pleos 위치: 대기 중"
    private var androidText = "안드로이드 위치: 대기 중"
    private var pleosCount = 0
    private var androidCount = 0

    // Pleos 위치가 바뀔 때마다 불린다
    private val pleosListener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            if (location == null) {
                pleosText = "Pleos 위치: 값 없음 (null)"
            } else {
                pleosCount++
                pleosText = "Pleos 위치 (받은 횟수 $pleosCount)\n" +
                    "  위도 %.6f, 경도 %.6f\n".format(location.latitude, location.longitude) +
                    "  진행 방향 %.0f°, 신뢰도 %.1f, 측정 시각 %s"
                        .format(location.heading, location.confidence, timeText(location.timestampMillis))
            }
            refresh()
        }

        override fun onError(exception: Exception) {
            pleosText = "Pleos 위치 오류: ${exception.message}"
            refresh()
        }
    }

    // 안드로이드 기본 위치가 바뀔 때마다 불린다
    private val androidListener = LocationListener { location ->
        androidCount++
        val speedKmh = if (location.hasSpeed()) "%.1f km/h".format(location.speed * 3.6f) else "없음"
        androidText = "안드로이드 위치 (받은 횟수 $androidCount)\n" +
            "  위도 %.6f, 경도 %.6f\n".format(location.latitude, location.longitude) +
            "  속도 $speedKmh, 받은 시각 ${timeText()}"
        refresh()
    }

    override fun start() {
        // Pleos FusedLocation: 지원 여부 확인 → 초기화 → 위치 구독
        if (fusedLocation.isSupported()) {
            fusedLocation.initialize()
            try {
                fusedLocation.registerFusedLocationCallback(pleosListener)
            } catch (e: Exception) {
                pleosText = "Pleos 위치: 구독 실패 (${e.message})"
            }
        } else {
            pleosText = "Pleos 위치: 이 기기에서 지원 안 함 (isSupported = false)"
        }

        // 안드로이드 기본 위치: GPS를 1초마다
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, androidListener, Looper.getMainLooper()
            )
        } catch (e: Exception) {
            androidText = "안드로이드 위치: 시작 실패 (${e.message})"
        }
        refresh()
    }

    override fun stop() {
        runCatching { fusedLocation.unregisterFusedLocationCallback(pleosListener) }
        runCatching { fusedLocation.release() }
        runCatching { locationManager.removeUpdates(androidListener) }
    }

    private fun refresh() = card.show("$pleosText\n\n$androidText")
}
