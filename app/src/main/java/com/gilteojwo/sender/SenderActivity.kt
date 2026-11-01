package com.gilteojwo.sender

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.gilteojwo.BuildConfig
import com.gilteojwo.BuildInfo
import com.gilteojwo.receive.VehicleType
import com.gilteojwo.send.DemoRoute
import com.gilteojwo.send.DispatchCheck
import com.gilteojwo.send.RegCode
import com.gilteojwo.send.SenderServer
import com.gilteojwo.send.SenderStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 송신 화면 (사양서 '화면 목록'의 송신 화면). 폰 앱과 차량 앱(Pleos 송신 모드)이 같은 화면을 쓴다.
 * 차량 앱에서는 수신 화면의 '송신 화면' 버튼으로 열고, 맨 위 '← 돌아가기'로 돌아간다.
 * - 등록 (S-1): 등록 코드를 넣으면 서버에서 차량 ID와 차종을 확인한다. 미등록 코드는 송신 불가
 * - 대기: 차량 정보와 큰 "긴급출동 시작" 버튼 하나
 * - 긴급출동 중 (S-2·S-6): 송신 상태와 "긴급출동 끄기" 버튼만 (운전 중 조작을 없앤다)
 *   · 길 열림 현황 "알림 N대 · 양보 N대" (S-8)
 *   · 60분이 지나면 "아직 출동 중인가요?"와 큰 "출동 중" 버튼 (S-7)
 * - 시연 모드 (S-4): 대기 화면의 시연 버튼으로 미리 정한 경로를 재생해 보낸다. 화면에 "시연 모드"를 표시한다
 * 실제 송신은 SendService가 뒤에서 한다. 화면을 꺼도 계속 보낸다 (S-5).
 */
class SenderActivity : Activity() {
    private val server = SenderServer()
    private lateinit var store: SenderStore

    private lateinit var headerText: TextView
    // 등록
    private lateinit var registerPanel: LinearLayout
    private lateinit var codeInput: EditText
    private lateinit var registerMessage: TextView
    // 대기
    private lateinit var standbyPanel: LinearLayout
    private lateinit var vehicleText: TextView
    // 긴급출동 중
    private lateinit var dispatchPanel: LinearLayout
    private lateinit var dispatchTitle: TextView
    private lateinit var healthText: TextView
    private lateinit var openRoadText: TextView // 길 열림 현황 (S-8)
    private lateinit var detailText: TextView
    private lateinit var checkPanel: LinearLayout // 출동 확인 질문 (S-7)
    private lateinit var checkText: TextView
    private var pendingDemo: DemoRoute? = null // 권한을 받은 뒤 시작할 시연 경로

    // 시험용: adb로 화면을 열 때 출동 확인 시간을 줄일 수 있다
    // 예: am start ... --ei check_after_seconds 10 --ei answer_seconds 15
    private var testCheckAfterSeconds = 0
    private var testAnswerSeconds = 0

    private val onState: (SendState) -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SenderStore(this)
        readTestValues(intent)
        buildScreen()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        readTestValues(intent)
    }

    private fun readTestValues(intent: android.content.Intent?) {
        testCheckAfterSeconds = intent?.getIntExtra(SendService.EXTRA_CHECK_AFTER_SECONDS, 0) ?: 0
        testAnswerSeconds = intent?.getIntExtra(SendService.EXTRA_ANSWER_SECONDS, 0) ?: 0
    }

    override fun onResume() {
        super.onResume()
        SendService.addListener(onState)
        render()
    }

    override fun onPause() {
        SendService.removeListener(onState)
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST && hasLocationPermission()) startDispatch(pendingDemo)
    }

    private fun render() {
        headerText.text = "길터줘 송신 · ${BuildInfo.text()}"
        val vehicle = store.vehicle
        val dispatching = store.activeDispatchId != null || SendService.latest.isDispatching
        registerPanel.visibility = if (vehicle == null) View.VISIBLE else View.GONE
        standbyPanel.visibility = if (vehicle != null && !dispatching) View.VISIBLE else View.GONE
        dispatchPanel.visibility = if (vehicle != null && dispatching) View.VISIBLE else View.GONE
        if (vehicle == null) return

        val typeName = VehicleType.fromCode(vehicle.typeCode)?.koreanName ?: vehicle.typeCode
        vehicleText.text = listOfNotNull(vehicle.id, typeName, vehicle.agency).joinToString(" · ")

        val state = SendService.latest
        dispatchTitle.text = state.demoRoute?.let { "긴급출동 중 · 시연 모드\n${it.koreanName} · 남은 ${state.demoSecondsLeft}초" } ?: "긴급출동 중"
        openRoadText.text = "알림 ${state.notifiedCount}대 · 양보 ${state.yieldedCount}대"
        val check = state.check
        checkPanel.visibility = if (check is DispatchCheck.Asking) View.VISIBLE else View.GONE
        if (check is DispatchCheck.Asking) checkText.text = "아직 출동 중인가요?\n${check.secondsLeft}초 안에 누르지 않으면 자동으로 끕니다"
        healthText.text = state.health.text
        healthText.setTextColor(if (state.health.isWarning) Color.parseColor("#FF9800") else Color.parseColor("#4CAF50"))
        val last = state.lastSuccessMillis?.let { "마지막 송신 ${timeText(it)}" } ?: "아직 보내지 않음"
        val fix = state.fix?.let { "위도 %.5f, 경도 %.5f · %.0f km/h".format(it.latitude, it.longitude, it.speedKmh) } ?: "위치를 기다리는 중"
        detailText.text = "$typeName ${vehicle.id}\n$last\n$fix"
    }

    // ── 등록 (S-1) ──
    private fun register() {
        val code = RegCode.normalize(codeInput.text.toString())
        if (code == null) {
            registerMessage.text = "등록 코드를 입력하세요"
            return
        }
        registerMessage.text = "확인하는 중…"
        server.checkVehicle(
            code,
            onDone = { vehicle ->
                if (vehicle == null) {
                    registerMessage.text = "등록되지 않은 코드입니다"
                } else {
                    store.regCode = code
                    store.vehicle = vehicle
                    registerMessage.text = ""
                    render()
                }
            },
            onError = { message -> registerMessage.text = "확인 실패: $message" },
        )
    }

    // ── 긴급출동 시작·끄기 (S-2) ──
    private fun startDispatch(demoRoute: DemoRoute? = null) {
        pendingDemo = demoRoute
        if (!hasLocationPermission()) {
            val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS
            requestPermissions(needed.toTypedArray(), PERMISSION_REQUEST)
            return
        }
        SendService.start(this, demoRoute, testCheckAfterSeconds, testAnswerSeconds)
        render()
    }

    private fun stopDispatch() {
        SendService.stop(this)
    }

    private fun hasLocationPermission() =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // ── 화면 쌓기 ──
    private fun buildScreen() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 48, 48, 48)
        headerText = addText(root, 16f)
        headerText.setTextColor(Color.GRAY)
        // 차량 앱에서만: 연 화면(수신 화면이나 설정)으로 돌아가기. 송신 중에도 수신은 뒤에서 계속 돈다
        if (BuildConfig.FLAVOR == "car") {
            addButton(root, "← 돌아가기", 20f, Color.parseColor("#424242"), height = 120) { finish() }
        }

        // 등록
        registerPanel = panel(root)
        addText(registerPanel, 28f, bold = true).text = "차량 등록"
        addText(registerPanel, 18f).text = "기관에서 받은 등록 코드를 입력하세요"
        codeInput = EditText(this)
        codeInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        codeInput.textSize = 24f
        codeInput.hint = "등록 코드"
        registerPanel.addView(codeInput)
        addButton(registerPanel, "확인", 24f, Color.parseColor("#2196F3")) { register() }
        registerMessage = addText(registerPanel, 18f)
        registerMessage.setTextColor(Color.parseColor("#F44336"))

        // 대기: 큰 버튼 하나
        standbyPanel = panel(root)
        vehicleText = addText(standbyPanel, 24f, bold = true)
        addButton(standbyPanel, "긴급출동 시작", 40f, Color.parseColor("#D32F2F"), height = 360) { startDispatch() }
        addText(standbyPanel, 16f).text = "긴급출동 중일 때만 위치를 보냅니다. 평소에는 위치를 보내지 않습니다."
        // 시연 모드 (S-4): 내 위치 대신 미리 정한 경로를 실제 속도로 보낸다.
        // 목표는 폰 앱이면 에뮬레이터 기본 위치, 차량 앱이면 지금 내 차 위치 (SenderLocation.demoTarget)
        addText(standbyPanel, 22f, bold = true).text = "시연 모드"
        for (route in DemoRoute.entries) {
            addButton(standbyPanel, "시연: ${route.koreanName} (${route.totalSeconds}초)", 22f, Color.parseColor("#7B1FA2")) {
                startDispatch(route)
            }
        }
        addButton(standbyPanel, "등록 해제", 16f, Color.GRAY) {
            store.clearRegistration()
            render()
        }

        // 긴급출동 중: 상태와 끄기 버튼만
        dispatchPanel = panel(root)
        dispatchTitle = addText(dispatchPanel, 32f, bold = true)
        dispatchTitle.setTextColor(Color.parseColor("#F44336"))
        // 출동 확인 질문 (S-7): 평소에는 숨겨 두고, 60분이 지나면 보인다
        checkPanel = panel(dispatchPanel)
        checkText = addText(checkPanel, 26f, bold = true)
        checkText.setTextColor(Color.parseColor("#FF9800"))
        addButton(checkPanel, "출동 중", 36f, Color.parseColor("#D32F2F"), height = 260) { SendService.confirm(this) }
        healthText = addText(dispatchPanel, 24f, bold = true)
        openRoadText = addText(dispatchPanel, 30f, bold = true)
        openRoadText.setTextColor(Color.parseColor("#4CAF50"))
        detailText = addText(dispatchPanel, 18f)
        addButton(dispatchPanel, "긴급출동 끄기", 36f, Color.parseColor("#424242"), height = 300) { stopDispatch() }

        setContentView(root)
    }

    private fun panel(parent: LinearLayout): LinearLayout {
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(0, 32, 0, 32)
        parent.addView(panel)
        return panel
    }

    private fun addText(parent: LinearLayout, sizeSp: Float, bold: Boolean = false): TextView {
        val text = TextView(this)
        text.textSize = sizeSp
        text.setPadding(0, 12, 0, 12)
        if (bold) text.setTypeface(null, Typeface.BOLD)
        parent.addView(text)
        return text
    }

    private fun addButton(parent: LinearLayout, label: String, sizeSp: Float, color: Int, height: Int = 160, onClick: () -> Unit) {
        val button = Button(this)
        button.text = label
        button.textSize = sizeSp
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(color)
        button.gravity = Gravity.CENTER
        button.setOnClickListener { onClick() }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        params.setMargins(0, 16, 0, 16)
        parent.addView(button, params)
    }

    private fun timeText(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(millis))

    companion object {
        private const val PERMISSION_REQUEST = 1
    }
}
