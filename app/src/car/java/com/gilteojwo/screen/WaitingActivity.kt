package com.gilteojwo.screen

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.gilteojwo.BuildInfo
import com.gilteojwo.alert.AlertStage
import com.gilteojwo.alert.YieldAction
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.permission.PermissionHelper
import com.gilteojwo.report.YieldReportStore
import com.gilteojwo.send.SenderStore
import com.gilteojwo.sender.SendService
import com.gilteojwo.sender.SenderActivity
import com.gilteojwo.service.ReceiveService
import com.gilteojwo.service.ReceiveSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 길터줘 화면 (사양서 '화면 목록'의 수신 화면). 알림 단계에 따라 보이는 내용이 바뀐다.
 * - 대기: 서버 연결, 주변 긴급차량 수, "알림이 없어도 사이렌에 주의하세요"
 * - 접근 알림(관심·경고): 내 차 기준 레이더, 방향, 거리, 도착 예상 시간
 * - 양보 안내(양보): 큰 그림 하나 + 한 줄 문구 (주행 중에는 이것만 보여준다), 행동 확인 표시
 * - 해제: "지나갔습니다" (3초 뒤 대기로). 양보했으면 "오늘 구급차 1대에 길을 열어줬습니다"(R-17)
 * 계산과 알림(내비 위 카드·가장자리 빛·소리·조명)은 모두 수신 서비스(ReceiveService)가 뒤에서 한다.
 * 이 화면은 그 결과를 보여주기만 해서, 닫거나 내비로 넘어가도 알림은 계속 온다.
 * 맨 위 '설정' 버튼, 송신 모드가 켜져 있으면(등록된 긴급차량) '송신 화면' 버튼도 보인다.
 */
class WaitingActivity : Activity() {
    // 맨 위 상태 줄
    private lateinit var headerText: TextView
    private lateinit var problemText: TextView
    private lateinit var sendScreenButton: Button // 송신 모드일 때만 보인다

    // 대기
    private lateinit var waitPanel: LinearLayout
    private lateinit var waitTitle: TextView
    private lateinit var serverText: TextView
    private lateinit var reportText: TextView // 지금까지 길을 열어준 횟수 (R-17)

    // 접근 알림·양보 안내·해제
    private lateinit var alertPanel: LinearLayout
    private lateinit var radar: RadarView
    private lateinit var stageText: TextView
    private lateinit var iconText: TextView
    private lateinit var directionText: TextView
    private lateinit var distanceText: TextView
    private lateinit var adviceText: TextView
    private lateinit var reportLineText: TextView // 해제 때 "오늘 구급차 1대에 길을 열어줬습니다"

    // 시험용 정보 (작게)
    private lateinit var debugText: TextView

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

    private fun render(s: ReceiveSnapshot) {
        val overlay = if (Settings.canDrawOverlays(this)) "가장자리 빛: 허용됨" else "가장자리 빛: '다른 앱 위에 표시' 허용 필요"
        headerText.text = "길터줘 · ${BuildInfo.text()} · ${PermissionHelper.statusText(this)} · $overlay"
        problemText.text = s.problems.joinToString("\n")
        sendScreenButton.visibility = if (SenderStore(this).vehicle != null) View.VISIBLE else View.GONE
        sendScreenButton.text = if (SendService.latest.isDispatching) "송신 화면 · 긴급출동 중" else "송신 화면"
        debugText.text = debugLines(s)

        val target = s.alert.target
        if (s.alert.stage == AlertStage.NONE || target == null) {
            showWait(s)
        } else {
            showAlert(s, target)
        }
    }

    /** 대기 화면 */
    private fun showWait(s: ReceiveSnapshot) {
        waitPanel.visibility = View.VISIBLE
        alertPanel.visibility = View.GONE
        waitTitle.text = if (s.vehicleCount == 0) "주변에 출동 중인 긴급차량이 없습니다" else "출동 중 긴급차량 ${s.vehicleCount}대 · 다가오는 차 없음"
        serverText.text = "서버 연결: ${s.serverStatus}"
        val today = s.tally.todayCount(YieldReportStore.today())
        reportText.text = if (s.tally.total > 0) "지금까지 긴급차량 ${s.tally.total}대에 길을 열어줬습니다 (오늘 ${today}대)" else ""
    }

    /** 접근 알림(관심·경고), 양보 안내(양보), 해제 화면 */
    private fun showAlert(s: ReceiveSnapshot, target: ApproachInfo) {
        waitPanel.visibility = View.GONE
        alertPanel.visibility = View.VISIBLE
        val stage = s.alert.stage
        val color = stageColor(stage)

        stageText.text = "${target.vehicle.type.koreanName} 접근 · ${stage.koreanName}"
        stageText.setTextColor(color)
        directionText.text = "${target.direction.koreanName}(${target.direction.clock})"
        distanceText.text = listOfNotNull("${target.distanceMeters.roundToInt()}m", target.arrivalText).joinToString(" · ")
        radar.show(target, color)

        // 양보·해제는 큰 그림 하나 + 한 줄 문구만 (주행 중에는 단순하게)
        val advice = s.advice
        val simple = stage == AlertStage.YIELD || stage == AlertStage.CLEARED
        radar.visibility = if (simple) View.GONE else View.VISIBLE
        directionText.visibility = if (simple) View.GONE else View.VISIBLE
        distanceText.visibility = if (simple) View.GONE else View.VISIBLE
        iconText.visibility = if (simple && advice != null) View.VISIBLE else View.GONE
        adviceText.visibility = if (advice != null) View.VISIBLE else View.GONE
        if (advice != null) {
            iconText.text = iconFor(advice.action)
            val adviceColor = when (advice.action) {
                YieldAction.CONFIRMED, YieldAction.PASSED -> GREEN
                YieldAction.SIGNAL_LOST -> ORANGE
                else -> color
            }
            iconText.setTextColor(adviceColor)
            adviceText.text = advice.text
            adviceText.setTextColor(adviceColor)
        }
        reportLineText.text = s.reportLine ?: ""
        reportLineText.visibility = if (s.reportLine != null) View.VISIBLE else View.GONE
    }

    /** 양보 안내 종류마다 크게 보여줄 그림 (글자 기호) */
    private fun iconFor(action: YieldAction): String = when (action) {
        YieldAction.MOVE_RIGHT -> "→" // 오른쪽으로
        YieldAction.SLOW_DOWN -> "↘" // 속도를 줄이며 비키기
        YieldAction.STOP_BEFORE_CROSSING -> "✋" // 진입 금지
        YieldAction.CONFIRMED, YieldAction.PASSED -> "✓"
        YieldAction.SIGNAL_LOST -> "⚠" // 정보가 끊김: 사이렌에 주의
    }

    private fun stageColor(stage: AlertStage): Int = when (stage) {
        AlertStage.ATTENTION -> ORANGE
        AlertStage.WARNING, AlertStage.YIELD -> RED
        AlertStage.CLEARED -> GREEN
        AlertStage.NONE -> Color.GRAY
    }

    /** 시험용 정보: 내 위치, 내 차 상태, 긴급차량 목록 */
    private fun debugLines(s: ReceiveSnapshot): String {
        val my = s.myPosition
        val location = if (my == null) {
            "내 위치 (${s.locationSource}): 기다리는 중"
        } else {
            "내 위치 (${s.locationSource}): %.6f, %.6f · 진행 방향 %.0f° · %s".format(
                my.point.latitude, my.point.longitude, my.headingDegrees, timeText(my.timeMillis),
            )
        }
        val vehicles = s.results.joinToString("") { info ->
            val arrival = info.arrivalText ?: "도착 시간 없음"
            val state = if (info.isApproaching) "접근 중" else "접근 아님"
            "\n· ${info.vehicle.type.koreanName} · ${info.direction.koreanName}(${info.direction.clock}) · " +
                "${info.distanceMeters.roundToInt()}m · $arrival · $state"
        }
        return "$location\n내 차: ${s.vehicleState.describe()}\n서버: ${s.serverStatus} · 긴급차량 ${s.results.size}대$vehicles"
    }

    /** 화면 쌓기: 상태 줄 → (대기 | 알림) → 주의 문구 → 시험용 정보 */
    private fun buildScreen() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 24, 48, 24)

        headerText = addText(root, 18f)
        problemText = addText(root, 18f)
        problemText.setTextColor(RED)

        // 맨 위 버튼 줄: 설정, (송신 모드면) 송신 화면
        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.HORIZONTAL
        addButton(buttons, "설정", Color.parseColor("#424242")) { startActivity(Intent(this, SettingsActivity::class.java)) }
        sendScreenButton = addButton(buttons, "송신 화면", Color.parseColor("#D32F2F")) {
            startActivity(Intent(this, SenderActivity::class.java))
        }
        root.addView(buttons)

        // 대기
        waitPanel = LinearLayout(this)
        waitPanel.orientation = LinearLayout.VERTICAL
        waitPanel.setPadding(0, 40, 0, 40)
        waitTitle = addText(waitPanel, 36f, bold = true)
        serverText = addText(waitPanel, 24f)
        reportText = addText(waitPanel, 28f, bold = true)
        reportText.setTextColor(GREEN)
        root.addView(waitPanel)

        // 알림: 왼쪽 레이더 + 오른쪽 글자
        alertPanel = LinearLayout(this)
        alertPanel.orientation = LinearLayout.HORIZONTAL
        alertPanel.gravity = Gravity.CENTER_VERTICAL
        radar = RadarView(this)
        alertPanel.addView(radar, LinearLayout.LayoutParams(RADAR_SIZE_PX, RADAR_SIZE_PX))
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(48, 0, 0, 0)
        stageText = addText(column, 40f, bold = true)
        iconText = addText(column, 140f, bold = true)
        directionText = addText(column, 40f, bold = true)
        distanceText = addText(column, 52f, bold = true)
        adviceText = addText(column, 44f, bold = true)
        reportLineText = addText(column, 32f, bold = true)
        reportLineText.setTextColor(GREEN)
        alertPanel.addView(column)
        root.addView(alertPanel)

        // 모든 긴급차량이 송신하는 것은 아니라서 이 문구는 항상 보인다 (사양서 '알림 단계와 양보 안내')
        val caution = addText(root, 30f, bold = true)
        caution.text = "알림이 없어도 사이렌에 주의하세요"
        caution.setTextColor(Color.parseColor("#FFB74D"))

        debugText = addText(root, 16f)
        debugText.setTextColor(Color.GRAY)

        setContentView(root)
    }

    private fun addText(parent: LinearLayout, sizeSp: Float, bold: Boolean = false): TextView {
        val text = TextView(this)
        text.textSize = sizeSp
        text.setPadding(0, 8, 0, 8)
        if (bold) text.setTypeface(null, Typeface.BOLD)
        parent.addView(text)
        return text
    }

    private fun addButton(parent: LinearLayout, label: String, color: Int, onClick: () -> Unit): Button {
        val button = Button(this)
        button.text = label
        button.textSize = 20f
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(color)
        button.setPadding(48, 0, 48, 0)
        button.setOnClickListener { onClick() }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 110)
        params.setMargins(0, 8, 24, 8)
        parent.addView(button, params)
        return button
    }

    private fun timeText(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(millis))

    companion object {
        private const val RADAR_SIZE_PX = 640
        private val ORANGE = Color.parseColor("#FF9800")
        private val RED = Color.parseColor("#F44336")
        private val GREEN = Color.parseColor("#4CAF50")
    }
}
