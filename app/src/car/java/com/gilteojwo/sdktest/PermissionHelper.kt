package com.gilteojwo.sdktest

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

/**
 * 권한 확인과 요청.
 * 'normal' 등급 권한은 매니페스트 선언만으로 허용되고,
 * 'dangerous' 등급(위치, 속도 등)은 앱 실행 중에 사용자에게 허락을 받아야 한다.
 * 여기서는 선언한 권한을 모두 확인해서, 아직 없는 것만 요청한다.
 */
object PermissionHelper {
    const val REQUEST_CODE = 1

    /** 매니페스트에 선언한 권한 목록 (다른 앱 위 표시는 따로 설정 화면에서 허용) */
    private val permissions = buildList {
        add("pleos.car.permission.FUSED_LOCATION") // 위치 (dangerous)
        add("pleos.car.permission.CAR_DRIVE") // 브레이크
        add("pleos.car.permission.CAR_LIGHTS") // 방향지시등, 앰비언트
        add("pleos.car.permission.TTS_SERVICE") // Pleos 음성
        add("android.car.permission.CAR_SPEED") // 속도 (dangerous)
        add("android.car.permission.CAR_POWERTRAIN") // 기어
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** 아직 허용되지 않은 권한 */
    fun missing(activity: Activity): List<String> =
        permissions.filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

    /** 아직 없는 권한을 한 번에 요청한다. 결과는 Activity.onRequestPermissionsResult로 온다. */
    fun requestMissing(activity: Activity) {
        activity.requestPermissions(missing(activity).toTypedArray(), REQUEST_CODE)
    }

    /** 화면 맨 위에 보여줄 권한 상태 */
    fun statusText(activity: Activity): String {
        val notGranted = missing(activity)
        return if (notGranted.isEmpty()) {
            "권한: 모두 허용됨"
        } else {
            "권한: 허용 안 됨 → " + notGranted.joinToString(", ") { it.substringAfterLast('.') }
        }
    }
}
