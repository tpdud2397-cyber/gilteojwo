package com.gilteojwo.sender

import android.content.Context
import com.gilteojwo.geo.GeoPoint
import com.gilteojwo.send.DemoRoute
import com.gilteojwo.send.SenderFix
import com.gilteojwo.service.ReceiveService
import com.gilteojwo.service.ReceiveSnapshot

/**
 * 차량 앱의 송신 위치 (사양서 S-3 Pleos 송신 모드).
 * 수신 서비스가 이미 받고 있는 내 차 위치(Pleos FusedLocation, 안 되면 안드로이드 위치)와
 * 계기판 속도를 그대로 쓴다. 같은 위치 기능을 두 번 연결하지 않으려고 이렇게 했다.
 * 폰 앱은 같은 이름의 파일(app/src/phone/.../sender/SenderLocation.kt)이 폰 GPS를 쓴다.
 */
class SenderLocation(private val context: Context, private val onFix: (SenderFix) -> Unit) {
    // 수신 서비스가 새 결과를 낼 때마다(약 1초) 불린다
    private val listener: (ReceiveSnapshot) -> Unit = { snapshot ->
        snapshot.myPosition?.let { onFix(SenderFix.fromCar(it, snapshot.vehicleState)) }
    }

    fun start() {
        ReceiveService.start(context) // 이미 돌고 있으면 아무 일도 없다
        ReceiveService.addListener(listener)
        ReceiveService.latest?.let(listener)
    }

    fun stop() {
        ReceiveService.removeListener(listener)
    }

    /** 시연 경로의 목표: 지금 내 차 위치 (아직 모르면 에뮬레이터 기본 위치) */
    fun demoTarget(): GeoPoint = ReceiveService.latest?.myPosition?.point ?: DemoRoute.DEMO_TARGET
}
