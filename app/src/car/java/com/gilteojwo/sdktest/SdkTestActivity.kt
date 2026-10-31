package com.gilteojwo.sdktest

import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.car.Car
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.gilteojwo.BuildInfo

/**
 * 차량용 시험 화면.
 * 왼쪽 칸은 값이 저절로 바뀌는 항목(①②⑦), 오른쪽 칸은 버튼을 눌러 시험하는 항목(③④⑤⑥)이다.
 * 순서: 공용 연결 → 화면 만들기 → 권한 요청 → 권한 결과가 오면 모든 시험 시작.
 */
class SdkTestActivity : Activity() {
    private var car: Car? = null // 안드로이드 차량 서비스 (주행 제한, 속도, 기어)
    private lateinit var vehicle: Vehicle // Pleos Vehicle SDK (브레이크, 방향지시등, 앰비언트)

    private val tests = mutableListOf<SdkTest>()
    private lateinit var drivingLimitTest: DrivingLimitTest
    private lateinit var alertOverNaviTest: AlertOverNaviTest
    private lateinit var headerText: TextView
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 공용 연결: 여러 시험이 같이 쓴다
        car = Car.createCar(this)
        vehicle = Vehicle(this)
        vehicle.initialize()

        // 2) 화면 만들기: 맨 위 상태 줄 + 왼쪽 칸 / 오른쪽 칸
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 16, 24, 16)

        headerText = TextView(this)
        headerText.textSize = 18f
        root.addView(headerText)

        val columns = LinearLayout(this)
        columns.orientation = LinearLayout.HORIZONTAL
        root.addView(columns, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val leftColumn = addColumn(columns)
        val rightColumn = addColumn(columns)
        setContentView(root)

        // 3) 시험 항목 만들기 (화면에 보이는 순서대로)
        tests += LocationTest(this, addCard(leftColumn, "① 내 위치"))
        drivingLimitTest = DrivingLimitTest(this, car, addCard(leftColumn, "② 주행 중 화면 제한"))
        tests += drivingLimitTest
        tests += VehicleStateTest(car, vehicle, addCard(leftColumn, "⑦ 차량 상태 (Virtual Console로 바꿔 보기)"))
        tests += AmbientLightTest(vehicle, addCard(rightColumn, "③ 앰비언트 라이트"))
        tests += SpeechTest(this, addCard(rightColumn, "④ 한국어 음성"))
        tests += StereoBeepTest(addCard(rightColumn, "⑤ 좌우 알림음"))
        alertOverNaviTest = AlertOverNaviTest(this, addCard(rightColumn, "⑥ 내비 위 알림"))
        tests += alertOverNaviTest

        // 4) 권한 요청. 이미 다 있으면 바로 시작한다
        updateHeader()
        if (PermissionHelper.missing(this).isEmpty()) {
            startTests()
        } else {
            PermissionHelper.requestMissing(this)
        }
    }

    /** 권한 요청 결과가 오면 (허용이든 거부든) 시험을 시작한다. 거부된 항목은 화면에 오류로 보인다. */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) startTests()
    }

    private fun startTests() {
        if (started) return
        started = true
        updateHeader()
        tests.forEach { it.start() }
    }

    override fun onResume() {
        super.onResume()
        // 설정 화면에서 권한을 바꾸고 돌아왔을 수 있으니 다시 보여준다
        updateHeader()
        if (started) alertOverNaviTest.refresh()
    }

    override fun onStop() {
        super.onStop()
        // 다른 화면에 가려졌다 (주행 제한 화면, 내비 등)
        if (started) drivingLimitTest.onScreenHidden()
    }

    override fun onDestroy() {
        tests.forEach { it.stop() }
        vehicle.release()
        car?.disconnect()
        super.onDestroy()
    }

    private fun updateHeader() {
        headerText.text = "길터줘 Pleos 시험 화면 · ${BuildInfo.text()} · ${PermissionHelper.statusText(this)}"
    }

    /** 세로로 스크롤되는 칸 하나를 만들어 columns에 반씩 나눠 넣는다 */
    private fun addColumn(columns: LinearLayout): LinearLayout {
        val scroll = ScrollView(this)
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(8, 8, 8, 8)
        scroll.addView(column)
        columns.addView(scroll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        return column
    }

    /** 칸 안에 시험 카드 하나를 추가한다 */
    private fun addCard(column: LinearLayout, title: String): TestCard {
        val card = TestCard(this, title)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = 16
        column.addView(card.view, params)
        return card
    }
}
