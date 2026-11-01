package com.gilteojwo.sender

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.gilteojwo.geo.GeoPoint
import com.gilteojwo.send.DemoRoute
import com.gilteojwo.send.SenderFix

/**
 * 폰 앱의 송신 위치 (사양서 S-3): 폰 GPS 위치를 1초마다 받아 onFix로 넘긴다.
 * 차량 앱은 같은 이름의 파일(app/src/car/.../sender/SenderLocation.kt)이 Pleos 위치를 쓴다.
 * 송신 서비스(SendService)는 폰·차 공용이고, 위치를 얻는 방법만 이렇게 나눴다.
 */
class SenderLocation(context: Context, private val onFix: (SenderFix) -> Unit) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private var lastHeading = 0.0

    // 폰 GPS 위치가 바뀔 때마다 불린다
    private val listener = LocationListener { location ->
        // 진행 방향은 움직이고 있을 때만 믿는다 (서 있으면 GPS 방향이 흔들린다)
        if (location.hasBearing() && location.speed > MOVING_METERS_PER_SECOND) lastHeading = location.bearing.toDouble()
        onFix(
            SenderFix(
                latitude = location.latitude,
                longitude = location.longitude,
                headingDegrees = lastHeading,
                speedKmh = location.speed * 3.6,
                accuracyMeters = location.accuracy.toDouble(),
            ),
        )
    }

    @SuppressLint("MissingPermission") // 화면에서 위치 권한을 받은 뒤에만 시작한다
    fun start() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
    }

    fun stop() {
        runCatching { locationManager.removeUpdates(listener) }
    }

    /** 시연 경로의 목표(수신 차 위치): 폰은 수신 차가 어디 있는지 모르니 에뮬레이터 기본 위치 */
    fun demoTarget(): GeoPoint = DemoRoute.DEMO_TARGET

    companion object {
        private const val MOVING_METERS_PER_SECOND = 1f // 초속 1m(시속 3.6km)보다 빠를 때만 진행 방향을 믿는다
    }
}
