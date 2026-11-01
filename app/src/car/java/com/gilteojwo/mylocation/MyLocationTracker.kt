package com.gilteojwo.mylocation

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import com.gilteojwo.geo.GeoPoint

/**
 * R-1 내 차 위치: Pleos 위치(FusedLocation SDK)를 받아 MyPosition으로 알려준다.
 * Pleos 위치를 지원하지 않는 기기면 안드로이드 기본 위치(GPS)를 1초마다 받는다.
 * 진행 방향은 HeadingFromMovement가 움직인 방향으로 계산한다 (에뮬레이터는 SDK 진행 방향이 늘 0°).
 * 결과(onChanged)는 항상 메인 스레드에서 부른다.
 */
class MyLocationTracker(context: Context, private val onChanged: (MyPosition) -> Unit) {
    private val fusedLocation = FusedLocation(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val heading = HeadingFromMovement()
    private val mainThread = Handler(Looper.getMainLooper())

    /** 지금 어느 쪽 위치를 쓰는지 (화면 표시용) */
    var sourceName = "준비 전"
        private set

    // Pleos 위치가 바뀔 때마다 불린다 (다른 스레드에서 올 수 있다)
    private val pleosListener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            if (location != null) report(location.latitude, location.longitude, location.heading.toDouble())
        }

        override fun onError(exception: Exception) {
            sourceName = "Pleos 위치 오류 (${exception.message})"
        }
    }

    // 안드로이드 기본 위치가 바뀔 때마다 불린다
    private val androidListener = LocationListener { location ->
        report(location.latitude, location.longitude, location.bearing.toDouble())
    }

    fun start() {
        if (fusedLocation.isSupported()) {
            fusedLocation.initialize()
            fusedLocation.registerFusedLocationCallback(pleosListener)
            sourceName = "Pleos 위치"
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, androidListener, Looper.getMainLooper()
            )
            sourceName = "안드로이드 위치"
        }
    }

    fun stop() {
        runCatching { fusedLocation.unregisterFusedLocationCallback(pleosListener) }
        runCatching { fusedLocation.release() }
        runCatching { locationManager.removeUpdates(androidListener) }
    }

    /** 받은 위치로 진행 방향을 계산해 MyPosition을 만들어 알려준다. 시각은 받은 시각을 쓴다. */
    private fun report(latitude: Double, longitude: Double, sdkHeadingDegrees: Double) {
        mainThread.post {
            val point = GeoPoint(latitude, longitude)
            val position = MyPosition(point, heading.update(point, sdkHeadingDegrees), System.currentTimeMillis())
            onChanged(position)
        }
    }
}
