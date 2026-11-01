package com.gilteojwo.send

import android.content.Context

/**
 * 송신 기기에 저장해 두는 것: 등록 코드와 확인된 차량 정보(S-1), 진행 중인 출동 번호(S-2).
 * 출동 번호를 저장해 두면 앱이 꺼졌다 다시 켜져도 같은 출동을 이어서 보낼 수 있다.
 */
class SenderStore(context: Context) {
    private val prefs = context.getSharedPreferences("sender", Context.MODE_PRIVATE)

    var regCode: String?
        get() = prefs.getString("reg_code", null)
        set(value) = prefs.edit().putString("reg_code", value).apply()

    var vehicle: SenderVehicle?
        get() {
            val id = prefs.getString("vehicle_id", null) ?: return null
            val type = prefs.getString("vehicle_type", null) ?: return null
            return SenderVehicle(id, type, prefs.getString("vehicle_agency", null))
        }
        set(value) = prefs.edit()
            .putString("vehicle_id", value?.id)
            .putString("vehicle_type", value?.typeCode)
            .putString("vehicle_agency", value?.agency)
            .apply()

    /** 진행 중인 출동 번호. 출동 중이 아니면 null */
    var activeDispatchId: String?
        get() = prefs.getString("dispatch_id", null)
        set(value) = prefs.edit().putString("dispatch_id", value).apply()

    var dispatchStartedMillis: Long
        get() = prefs.getLong("dispatch_started", 0L)
        set(value) = prefs.edit().putLong("dispatch_started", value).apply()

    /** 등록 해제: 코드와 차량 정보를 지운다 */
    fun clearRegistration() {
        prefs.edit().clear().apply()
    }
}
