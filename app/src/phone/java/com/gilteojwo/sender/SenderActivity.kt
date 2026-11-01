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
import com.gilteojwo.BuildInfo
import com.gilteojwo.receive.VehicleType
import com.gilteojwo.send.RegCode
import com.gilteojwo.send.SenderServer
import com.gilteojwo.send.SenderStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 폰용 송신 앱 화면 (사양서 '화면 목록'의 송신 화면).
 * - 등록 (S-1): 등록 코드를 넣으면 서버에서 차량 ID와 차종을 확인한다. 미등록 코드는 송신 불가
 * - 대기: 차량 정보와 큰 "긴급출동 시작" 버튼 하나
 * - 긴급출동 중 (S-2·S-6): 송신 상태와 "긴급출동 끄기" 버튼만 (운전 중 조작을 없앤다)
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
    private lateinit var healthText: TextView
    private lateinit var detailText: TextView

    private val onState: (SendState) -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SenderStore(this)
        buildScreen()
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
        if (requestCode == PERMISSION_REQUEST && hasLocationPermission()) startDispatch()
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
    private fun startDispatch() {
        if (!hasLocationPermission()) {
            val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS
            requestPermissions(needed.toTypedArray(), PERMISSION_REQUEST)
            return
        }
        SendService.start(this)
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
        addButton(standbyPanel, "등록 해제", 16f, Color.GRAY) {
            store.clearRegistration()
            render()
        }

        // 긴급출동 중: 상태와 끄기 버튼만
        dispatchPanel = panel(root)
        addText(dispatchPanel, 32f, bold = true).apply {
            text = "긴급출동 중"
            setTextColor(Color.parseColor("#F44336"))
        }
        healthText = addText(dispatchPanel, 24f, bold = true)
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
