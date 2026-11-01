package com.gilteojwo.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 차가 켜지면(부팅 완료) 수신 서비스를 자동으로 시작한다. 운전자가 길터줘를 열지 않아도 알림이 온다.
 * 앱을 새 판으로 덮어 설치했을 때도(MY_PACKAGE_REPLACED) 다시 시작한다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(ReceiveService.LOG_TAG, "자동 시작: ${intent.action}")
        ReceiveService.start(context)
    }
}
