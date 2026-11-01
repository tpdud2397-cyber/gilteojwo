package com.gilteojwo.settings

import android.content.Context
import com.gilteojwo.alert.AlertDistance

/** 수신 설정을 차에 저장해 둔다 (사양서 '화면 목록'의 설정): 지금은 알림 거리 하나 */
class ReceiveSettings(context: Context) {
    private val prefs = context.getSharedPreferences("receive_settings", Context.MODE_PRIVATE)

    var alertDistance: AlertDistance
        get() = AlertDistance.fromName(prefs.getString("alert_distance", null))
        set(value) = prefs.edit().putString("alert_distance", value.name).apply()
}
