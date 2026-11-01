package com.gilteojwo.screen

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import com.gilteojwo.BuildInfo
import com.gilteojwo.alert.AlertStage
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.permission.PermissionHelper
import com.gilteojwo.service.ReceiveService
import com.gilteojwo.service.ReceiveSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 수신 대기 화면 (길터줘를 켜면 처음 보이는 화면).
 * 계산과 알림은 모두 수신 서비스(ReceiveService)가 뒤에서 한다. 이 화면은 그 결과를 보여주기만 한다.
 * 그래서 이 화면을 닫거나 내비로 넘어가도 알림은 계속 온다.
 * 처음 켤 때 권한을 받고 수신 서비스를 시작한다 (차를 켤 때도 서비스가 저절로 시작된다).
 */
class WaitingActivity : Activity() {
    private lateinit var headerText: TextView
    private lateinit var serverText: TextView
    private lateinit var myLocationText: TextView
    private lateinit var vehicleStateText: TextView
    private lateinit var vehiclesText: TextView
    private lateinit var alertText: TextView
    private lateinit var problemText: TextView

    // 수신 서비스가 새 결과를 낼 때마다 불린다
    private val onSnapshot: (ReceiveSnapshot) -> Unit = { render(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()

        // 권한이 다 있으면 바로 수신을 시작하고, 없으면 먼저 허락을 받는다
        if (PermissionHelper.missing(this).isEmpty()) {
            ReceiveService.start(this)
        } else {
            PermissionHelper.requestMissing(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) ReceiveService.start(this)
    }

    override fun onResume() {
        super.onResume()
        ReceiveService.addListener(onSnapshot)
        render(ReceiveService.latest ?: ReceiveSnapshot())
    }

    override fun onPause() {
        ReceiveService.removeListener(onSnapshot)
        super.onPause()
    }

    private fun render(snapshot: ReceiveSnapshot) {
        val overlay = if (Settings.canDrawOverlays(this)) "가장자리 빛: 허용됨" else "가장자리 빛: '다른 앱 위에 표시' 허용 필요"
        headerText.text = "길터줘 · ${BuildInfo.text()} · ${PermissionHelper.statusText(this)} · $overlay"
        serverText.text = "서버 연결: ${snapshot.serverStatus} · 출동 중 긴급차량 ${snapshot.vehicleCount}대"
        vehicleStateText.text = "내 차: ${snapshot.vehicleState.describe()}"
        problemText.text = snapshot.problems.joinToString("\n")

        val my = snapshot.myPosition
        myLocationText.text = if (my == null) {
            "내 위치 (${snapshot.locationSource}): 기다리는 중"
        } else {
            "내 위치 (${snapshot.locationSource}): " +
                "위도 %.6f, 경도 %.6f, 진행 방향 %.0f° · %s".format(
                    my.point.latitude, my.point.longitude, my.headingDegrees, timeText(my.timeMillis),
                )
        }

        vehiclesText.text = "주변 긴급차량: ${snapshot.results.size}대" +
            snapshot.results.joinToString("") { "\n· " + describe(it) }

        alertText.text = "알림: ${ReceiveService.describeAlert(snapshot)}"
        alertText.setTextColor(
            when (snapshot.alert.stage) {
                AlertStage.ATTENTION -> Color.parseColor("#FF9800") // 주황
                AlertStage.WARNING, AlertStage.YIELD -> Color.parseColor("#F44336") // 빨강
                AlertStage.CLEARED -> Color.parseColor("#4CAF50") // 초록
                AlertStage.NONE -> Color.GRAY
            },
        )
    }

    /** 예: "구급차 · 후방(6시) · 480m · 약 24초 · 접근 중" */
    private fun describe(info: ApproachInfo): String {
        val arrival = info.arrivalText ?: "도착 시간 없음"
        val state = if (info.isApproaching) "접근 중" else "접근 아님"
        return "${info.vehicle.type.koreanName} · ${info.direction.koreanName}(${info.direction.clock}) · " +
            "${info.distanceMeters.roundToInt()}m · $arrival · $state"
    }

    /** 화면을 위에서 아래로 쌓는다: 상태 줄 → 서버 → 내 위치 → 내 차 상태 → 긴급차량 목록 → 알림 → 주의 문구 */
    private fun buildScreen() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 32, 48, 32)

        headerText = addText(root, 20f)
        problemText = addText(root, 20f)
        problemText.setTextColor(Color.parseColor("#F44336"))
        serverText = addText(root, 26f)
        myLocationText = addText(root, 26f)
        vehicleStateText = addText(root, 26f)
        vehiclesText = addText(root, 30f)
        alertText = addText(root, 34f)
        alertText.setTypeface(null, Typeface.BOLD)

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
}
