package com.gilteojwo.screen

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.gilteojwo.BuildInfo
import com.gilteojwo.alert.AlertDistance
import com.gilteojwo.receive.VehicleType
import com.gilteojwo.send.SenderStore
import com.gilteojwo.sender.SenderActivity
import com.gilteojwo.settings.ReceiveSettings

/**
 * 설정 화면 (사양서 '화면 목록'의 수신 설정).
 * - 알림 거리: 긴급차량이 몇 m 안으로 오면 알리기 시작할지 (700m / 1,000m / 1,500m)
 * - 송신 모드: 긴급차량에 단 Pleos 차량이면 등록 코드를 넣어 켠다 (S-1). 켜지면 수신 화면에 '송신 화면' 버튼이 생긴다.
 *   등록 코드 입력과 해제는 송신 화면(폰 앱과 같은 화면)에서 한다.
 */
class SettingsActivity : Activity() {
    private lateinit var settings: ReceiveSettings
    private lateinit var store: SenderStore

    private val distanceButtons = mutableMapOf<AlertDistance, Button>()
    private lateinit var sendModeText: TextView
    private lateinit var sendModeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = ReceiveSettings(this)
        store = SenderStore(this)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        render() // 송신 화면에서 등록하고 돌아오면 바로 바뀐다
    }

    private fun render() {
        val chosen = settings.alertDistance
        distanceButtons.forEach { (distance, button) ->
            button.setBackgroundColor(if (distance == chosen) BLUE else Color.parseColor("#424242"))
        }
        val vehicle = store.vehicle
        if (vehicle == null) {
            sendModeText.text = "꺼짐 · 구급차·소방차·경찰차에 단 차량만 켭니다"
            sendModeButton.text = "송신 모드 켜기 (등록 코드 입력)"
        } else {
            val typeName = VehicleType.fromCode(vehicle.typeCode)?.koreanName ?: vehicle.typeCode
            sendModeText.text = "켜짐 · " + listOfNotNull(vehicle.id, typeName, vehicle.agency).joinToString(" · ")
            sendModeButton.text = "송신 화면 열기"
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 24, 48, 24)

        addText(root, 16f).apply { text = "길터줘 설정 · ${BuildInfo.text()}"; setTextColor(Color.GRAY) }
        addButton(root, "← 돌아가기", 20f, Color.parseColor("#424242"), weight = 0f) { finish() }

        // 알림 거리
        addText(root, 30f, bold = true).text = "알림 거리"
        addText(root, 20f).text = "긴급차량이 이 거리 안으로 다가오면 알리기 시작합니다. 경고(500m)·양보(200m)는 그대로입니다."
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (distance in AlertDistance.entries) {
            distanceButtons[distance] = addButton(row, distance.label, 24f, Color.GRAY, weight = 1f) {
                settings.alertDistance = distance
                render()
            }
        }
        root.addView(row)

        // 송신 모드 (긴급차량용)
        addText(root, 30f, bold = true).text = "송신 모드 (긴급차량용)"
        sendModeText = addText(root, 22f)
        sendModeButton = addButton(root, "", 24f, Color.parseColor("#D32F2F"), weight = 0f) {
            startActivity(Intent(this, SenderActivity::class.java))
        }
        addText(root, 18f).apply {
            text = "송신 중에도 수신은 계속됩니다. 실제 출동 중에는 내 차를 수신 목록에서 빼고, 시연 경로는 그대로 받아 알립니다."
            setTextColor(Color.GRAY)
        }

        setContentView(root)
    }

    private fun addText(parent: LinearLayout, sizeSp: Float, bold: Boolean = false): TextView {
        val text = TextView(this)
        text.textSize = sizeSp
        text.setPadding(0, 16, 0, 8)
        if (bold) text.setTypeface(null, Typeface.BOLD)
        parent.addView(text)
        return text
    }

    /** weight 0이면 한 줄 전체, 1이면 같은 줄의 다른 버튼과 나눠 쓴다 */
    private fun addButton(parent: LinearLayout, label: String, sizeSp: Float, color: Int, weight: Float, onClick: () -> Unit): Button {
        val button = Button(this)
        button.text = label
        button.textSize = sizeSp
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(color)
        button.gravity = Gravity.CENTER
        button.setOnClickListener { onClick() }
        val params = if (weight > 0f) {
            LinearLayout.LayoutParams(0, BUTTON_HEIGHT, weight)
        } else {
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, BUTTON_HEIGHT)
        }
        params.setMargins(8, 12, 8, 12)
        parent.addView(button, params)
        return button
    }

    companion object {
        private const val BUTTON_HEIGHT = 140
        private val BLUE = Color.parseColor("#1976D2")
    }
}
