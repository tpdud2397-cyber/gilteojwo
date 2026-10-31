package com.gilteojwo.sdktest

import android.app.Activity
import android.car.Car
import android.car.content.pm.CarPackageManager
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager

/**
 * ② 주행 중 화면 제한 시험
 * - 지금 주행 제한(CarUxRestrictions)이 걸렸는지
 * - 시스템이 우리 화면을 "주행 중에 봐도 되는 화면(DO)"으로 인정하는지
 *   (매니페스트에 distractionOptimized=true를 선언했다)
 * - 실제로 우리 화면이 다른 화면에 가려진 횟수
 */
class DrivingLimitTest(
    private val activity: Activity,
    private val car: Car?,
    private val card: TestCard,
) : SdkTest {
    private var uxManager: CarUxRestrictionsManager? = null
    private var restrictionText = "주행 제한: 확인 전"
    private var optimizedText = "확인 전"
    private var hiddenCount = 0
    private var lastHiddenTime = "-"

    override fun start() {
        optimizedText = checkOurScreenOptimized()

        val manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        uxManager = manager
        if (manager == null) {
            restrictionText = "주행 제한: 차량 서비스에 연결하지 못함"
            refresh()
            return
        }
        showRestrictions(manager.currentCarUxRestrictions)
        // 주행 제한이 바뀔 때마다 불린다 (기어, 속도에 따라 시스템이 알려준다)
        manager.registerListener { restrictions -> showRestrictions(restrictions) }
    }

    override fun stop() {
        runCatching { uxManager?.unregisterListener() }
    }

    /** 우리 화면이 가려졌을 때(onStop) Activity가 부른다 */
    fun onScreenHidden() {
        hiddenCount++
        lastHiddenTime = timeText()
        refresh()
    }

    private fun showRestrictions(restrictions: CarUxRestrictions?) {
        restrictionText = if (restrictions == null) {
            "주행 제한: 정보 없음"
        } else {
            val limited = if (restrictions.isRequiresDistractionOptimization) "예" else "아니오"
            "주행 제한 걸림: $limited (제한 코드 ${restrictions.activeRestrictions}, ${timeText()})"
        }
        refresh()
    }

    /** 시스템에 "이 화면을 주행 중에 띄워도 되는가"를 묻는다 */
    private fun checkOurScreenOptimized(): String {
        val packageManager = car?.getCarManager(Car.PACKAGE_SERVICE) as? CarPackageManager
            ?: return "확인 못 함 (차량 서비스 없음)"
        return try {
            packageManager.isActivityDistractionOptimized(activity.packageName, activity.javaClass.name).toString()
        } catch (e: Exception) {
            "확인 실패 (${e.message})"
        }
    }

    private fun refresh() = card.show(
        "$restrictionText\n" +
            "우리 화면을 주행용으로 인정: $optimizedText\n" +
            "화면이 가려진 횟수: $hiddenCount (마지막 $lastHiddenTime)"
    )
}
