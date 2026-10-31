package com.gilteojwo.sdktest

import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.car.Car
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.gilteojwo.BuildInfo

/**
 * 차량용 시험 화면.
 * 왼쪽 칸은 값이 저절로 바뀌는 항목(①②⑦), 오른쪽 칸은 버튼을 눌러 시험하는 항목(③④⑤⑥)이다.
 * 순서: 오류 기록 준비 → 공용 연결 → 화면 만들기 → 권한 요청 → 권한 결과가 오면 모든 시험 시작.
 * 지난번에 앱이 꺼졌으면 자동 시작을 하지 않고 꺼진 원인을 보여준다 (안전 모드).
 * 이때는 항목마다 [이 항목 시작]을 눌러 하나씩 켤 수 있다.
 */
class SdkTestActivity : Activity() {
    private var car: Car? = null // 안드로이드 차량 서비스 (주행 제한, 속도, 기어)
    private lateinit var vehicle: Vehicle // Pleos Vehicle SDK (브레이크, 방향지시등, 앰비언트)

    private val tests = mutableListOf<SdkTest>()
    private val startedTests = mutableSetOf<SdkTest>()
    private val startErrors = mutableListOf<String>() // 시작하다 오류가 난 항목
    private lateinit var drivingLimitTest: DrivingLimitTest
    private lateinit var alertOverNaviTest: AlertOverNaviTest
    private lateinit var headerText: TextView
    private lateinit var crashText: TextView
    private var lastCrash: String? = null // 지난번에 꺼진 원인 (없으면 null)
    private val mainThread = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0) 오류 기록: 지난번에 꺼졌는지 확인하고, 이번에 꺼지면 원인을 저장하게 한다
        lastCrash = CrashRecorder.takeLastCrash(this)
        CrashRecorder.install(this)

        // 1) 공용 연결: 여러 시험이 같이 쓴다
        car = Car.createCar(this)
        vehicle = Vehicle(this)
        vehicle.initialize()

        // 2) 화면 만들기: 맨 위 상태 줄 + (꺼진 원인) + 왼쪽 칸 / 오른쪽 칸
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 16, 24, 16)

        headerText = TextView(this)
        headerText.textSize = 22f
        root.addView(headerText)

        crashText = TextView(this)
        crashText.textSize = 18f
        crashText.setTextColor(Color.parseColor("#FF6E6E"))
        crashText.visibility = View.GONE
        root.addView(crashText)

        val columns = LinearLayout(this)
        columns.orientation = LinearLayout.HORIZONTAL
        root.addView(columns, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val leftColumn = addColumn(columns)
        val rightColumn = addColumn(columns)
        setContentView(root)

        // 3) 시험 항목 만들기 (화면에 보이는 순서대로)
        addTest(leftColumn, "① 내 위치") { card -> LocationTest(this, card) }
        drivingLimitTest = addTest(leftColumn, "② 주행 중 화면 제한") { card -> DrivingLimitTest(this, car, card) }
        addTest(leftColumn, "⑦ 차량 상태 (Virtual Console로 바꿔 보기)") { card -> VehicleStateTest(car, vehicle, card) }
        addTest(rightColumn, "③ 앰비언트 라이트") { card -> AmbientLightTest(vehicle, card) }
        addTest(rightColumn, "④ 한국어 음성") { card -> SpeechTest(this, card) }
        addTest(rightColumn, "⑤ 좌우 알림음") { card -> StereoBeepTest(card) }
        alertOverNaviTest = addTest(rightColumn, "⑥ 내비 위 알림") { card -> AlertOverNaviTest(this, card) }

        // 4) 지난번에 꺼졌으면 원인을 보여준다
        lastCrash?.let {
            crashText.text = "지난번에 앱이 꺼졌습니다. 이 빨간 글자를 캡처해서 보내 주세요.\n" +
                "자동 시작을 멈췄습니다. 항목마다 [이 항목 시작]을 하나씩 눌러 보세요.\n\n$it"
            crashText.visibility = View.VISIBLE
        }

        // 5) 권한 요청. 이미 다 있으면 바로 시작한다
        updateHeader()
        if (PermissionHelper.missing(this).isEmpty()) {
            startAllTests()
        } else {
            PermissionHelper.requestMissing(this)
        }
    }

    /** 권한 요청 결과가 오면 (허용이든 거부든) 시험을 시작한다. 거부된 항목은 화면에 오류로 보인다. */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) startAllTests()
    }

    /** 모든 시험을 자동으로 시작한다. 지난번에 꺼졌으면(안전 모드) 시작하지 않는다. */
    private fun startAllTests() {
        updateHeader()
        if (lastCrash != null) return
        for (test in tests) startOne(test, "전체 시험")
    }

    /** 시험 하나를 시작한다. 시작 직후 5초 안에 앱이 꺼지면 다음에 켤 때 이름이 보인다. */
    private fun startOne(test: SdkTest, label: String) {
        if (test in startedTests) return
        CrashRecorder.markStarting(this, label)
        try {
            test.start()
            startedTests += test
        } catch (e: Throwable) {
            // 일반 오류(Exception)보다 심각한 오류(Error)까지 잡아서 화면에 보여준다
            startErrors += "${test.javaClass.simpleName}: ${e.javaClass.simpleName} ${e.message}"
        }
        mainThread.removeCallbacksAndMessages(null)
        mainThread.postDelayed({ CrashRecorder.clearMark(this) }, 5000)
        updateHeader()
    }

    override fun onResume() {
        super.onResume()
        // 설정 화면에서 권한을 바꾸고 돌아왔을 수 있으니 다시 보여준다
        updateHeader()
        if (alertOverNaviTest in startedTests) alertOverNaviTest.refresh()
    }

    override fun onStop() {
        super.onStop()
        // 다른 화면에 가려졌다 (주행 제한 화면, 내비 등)
        if (drivingLimitTest in startedTests) drivingLimitTest.onScreenHidden()
    }

    override fun onDestroy() {
        startedTests.forEach { runCatching { it.stop() } }
        vehicle.release()
        car?.disconnect()
        super.onDestroy()
    }

    private fun updateHeader() {
        var text = "길터줘 Pleos 시험 화면 · ${BuildInfo.text()} · ${PermissionHelper.statusText(this)}" +
            " · 시작한 항목 ${startedTests.size}/${tests.size}"
        if (startErrors.isNotEmpty()) text += "\n시작 오류: " + startErrors.joinToString(" / ")
        headerText.text = text
    }

    /** 칸(card)을 만들고, 그 칸을 쓰는 시험을 만들고, [이 항목 시작] 버튼을 붙인다 */
    private fun <T : SdkTest> addTest(column: LinearLayout, title: String, create: (TestCard) -> T): T {
        val card = addCard(column, title)
        val test = create(card)
        card.addButtons("이 항목 시작" to { startOne(test, title) })
        tests += test
        return test
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
        params.bottomMargin = 24
        column.addView(card.view, params)
        return card
    }
}
