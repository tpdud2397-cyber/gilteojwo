package com.gilteojwo.screen

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import com.gilteojwo.BuildInfo
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.ApproachTracker
import com.gilteojwo.mylocation.MyLocationTracker
import com.gilteojwo.mylocation.MyPosition
import com.gilteojwo.permission.PermissionHelper
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.EmergencyVehicleReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 수신 대기 화면 (길터줘를 켜면 처음 보이는 화면).
 * 내 위치(R-1)와 주변 긴급차량 목록(R-2)을 받아, 차마다 거리·방향·도착 시간·접근 여부(R-3·R-4)를 보여준다.
 * 지금은 확인용으로 숫자를 그대로 보여주고, 알림 화면·소리·조명은 다음 단계에서 붙인다.
 */
class WaitingActivity : Activity() {
    private lateinit var locationTracker: MyLocationTracker
    private lateinit var vehicleReceiver: EmergencyVehicleReceiver
    private val approachTracker = ApproachTracker()

    private var myPosition: MyPosition? = null
    private var vehicles: List<EmergencyVehicle> = emptyList() // 서버에서 받은 출동 중 긴급차량
    private var serverStatus = "연결 준비"
    private var results: List<ApproachInfo> = emptyList() // 차마다 거리·방향·도착 시간·접근 여부

    private lateinit var headerText: TextView
    private lateinit var serverText: TextView
    private lateinit var myLocationText: TextView
    private lateinit var vehiclesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        locationTracker = MyLocationTracker(this) { position -> onMyPositionChanged(position) }
        vehicleReceiver = EmergencyVehicleReceiver { list, status -> onVehiclesChanged(list, status) }

        // 권한이 다 있으면 바로 시작하고, 없으면 먼저 허락을 받는다
        if (PermissionHelper.missing(this).isEmpty()) {
            start()
        } else {
            PermissionHelper.requestMissing(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) start()
    }

    override fun onDestroy() {
        locationTracker.stop()
        vehicleReceiver.stop()
        super.onDestroy()
    }

    private fun start() {
        try {
            locationTracker.start()
        } catch (e: Exception) {
            myLocationText.text = "내 위치: 시작 실패 (${e.message})"
        }
        vehicleReceiver.start()
        refresh()
    }

    /** 내 위치가 새로 올 때마다(약 1초) 거리·방향·도착 시간·접근 여부를 다시 계산한다 */
    private fun onMyPositionChanged(position: MyPosition) {
        myPosition = position
        results = approachTracker.update(position, vehicles)
        // 집 PC에서 `adb logcat -s Gilteojwo`로 계산 결과를 시간순으로 볼 수 있다
        Log.i(LOG_TAG, "계산: 긴급차량 ${results.size}대 " + results.joinToString(" / ") { describe(it) })
        refresh()
    }

    /** 서버 목록이 바뀌면 기억만 해 두고, 계산은 다음 내 위치가 올 때(1초 안) 한다 */
    private fun onVehiclesChanged(list: List<EmergencyVehicle>, status: String) {
        vehicles = list
        serverStatus = status
        refresh()
    }

    private fun refresh() {
        headerText.text = "길터줘 · ${BuildInfo.text()} · ${PermissionHelper.statusText(this)}"
        serverText.text = "서버 연결: $serverStatus · 출동 중 긴급차량 ${vehicles.size}대"

        val my = myPosition
        if (my == null) {
            myLocationText.text = "내 위치 (${locationTracker.sourceName}): 기다리는 중"
            vehiclesText.text = "주변 긴급차량: 내 위치를 받으면 계산합니다"
            return
        }
        myLocationText.text = "내 위치 (${locationTracker.sourceName}): " +
            "위도 %.6f, 경도 %.6f, 진행 방향 %.0f° · %s".format(
                my.point.latitude, my.point.longitude, my.headingDegrees, timeText(my.timeMillis)
            )

        vehiclesText.text = "주변 긴급차량: ${results.size}대" +
            results.joinToString("") { "\n· " + describe(it) }
    }

    /** 예: "구급차 · 후방 · 480m · 약 24초 · 접근 중" */
    private fun describe(info: ApproachInfo): String {
        val arrival = info.arrivalSeconds?.let { "약 ${it}초" } ?: "도착 시간 없음"
        val state = if (info.isApproaching) "접근 중" else "접근 아님"
        return "${info.vehicle.type.koreanName} · ${info.direction.koreanName} · " +
            "${info.distanceMeters.roundToInt()}m · $arrival · $state"
    }

    /** 화면을 위에서 아래로 쌓는다: 상태 줄 → 서버 → 내 위치 → 긴급차량 목록 → 주의 문구 */
    private fun buildScreen() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 32, 48, 32)

        headerText = addText(root, 20f)
        serverText = addText(root, 26f)
        myLocationText = addText(root, 26f)
        vehiclesText = addText(root, 30f)

        // 모든 긴급차량이 송신하는 것은 아니라서 이 문구는 항상 보인다 (사양서 '알림 단계와 양보 안내')
        val caution = addText(root, 30f)
        caution.text = "알림이 없어도 사이렌에 주의하세요"
        caution.setTextColor(Color.parseColor("#FFB74D"))
        caution.setTypeface(null, Typeface.BOLD)

        setContentView(root)
    }

    private fun addText(parent: LinearLayout, sizeSp: Float): TextView {
        val text = TextView(this)
        text.textSize = sizeSp
        text.setPadding(0, 16, 0, 16)
        parent.addView(text)
        return text
    }

    private fun timeText(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(millis))

    companion object {
        const val LOG_TAG = "Gilteojwo"
    }
}
