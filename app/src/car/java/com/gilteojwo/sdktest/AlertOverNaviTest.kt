package com.gilteojwo.sdktest

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.gilteojwo.R

/**
 * ⑥ 내비 위 알림 시험
 * 버튼을 누르고 10초 안에 내비를 열어 두면, 10초 뒤 알림이 내비 위에 보이는지 확인한다.
 * - 상단 알림: 안드로이드 알림 (중요도 '높음'이면 화면 위쪽에 카드로 잠깐 뜬다)
 * - 화면 덮기: '다른 앱 위에 표시' 권한으로 화면 위쪽에 빨간 띠를 5초 동안 그린다
 */
class AlertOverNaviTest(private val activity: Activity, private val card: TestCard) : SdkTest {
    private val channelId = "approach_test"
    private val waitSeconds = 10
    private val mainThread = Handler(Looper.getMainLooper())
    private val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val windowManager = activity.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var counting = false
    private var status = "대기 중"

    init {
        card.addButtons(
            "${waitSeconds}초 뒤 상단 알림" to { startCountdown("상단 알림") { showNotification() } },
            "${waitSeconds}초 뒤 화면 덮기" to { startCountdown("화면 덮기") { showOverlay() } },
        )
        card.addButtons("'다른 앱 위에 표시' 허용하러 가기" to ::openOverlaySettings)
    }

    override fun start() {
        // 알림 채널: 중요도 '높음'이어야 화면 위쪽에 카드로 뜬다
        val channel = NotificationChannel(channelId, "긴급차량 접근 (시험)", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        refresh()
    }

    override fun stop() {
        mainThread.removeCallbacksAndMessages(null)
        removeOverlay()
    }

    /** 1초마다 남은 시간을 보여주다가 0이 되면 action을 실행한다 */
    private fun startCountdown(name: String, action: () -> Unit) {
        if (counting) return
        counting = true
        fun tick(left: Int) {
            if (left == 0) {
                counting = false
                action()
                return
            }
            status = "$name: ${left}초 남음. 지금 내비를 여세요"
            refresh()
            mainThread.postDelayed({ tick(left - 1) }, 1000)
        }
        tick(waitSeconds)
    }

    private fun showNotification() {
        // 알림을 누르면 시험 화면으로 돌아온다
        val openApp = PendingIntent.getActivity(
            activity, 0, Intent(activity, SdkTestActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(activity, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("구급차 접근 (시험)")
            .setContentText("오른쪽 뒤 300m · 길터줘 알림 시험")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1, notification)
        status = "상단 알림을 보냈습니다 (${timeText()}). 내비 위에 보였나요?"
        refresh()
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(activity)) {
            status = "'다른 앱 위에 표시' 권한이 없어 화면 덮기를 못 했습니다"
            refresh()
            return
        }
        // 화면 위쪽 빨간 띠
        val band = TextView(activity.applicationContext)
        band.text = "구급차 접근 (시험) · 오른쪽 뒤 300m"
        band.textSize = 28f
        band.setTextColor(Color.WHITE)
        band.setBackgroundColor(Color.argb(230, 229, 57, 53))
        band.gravity = Gravity.CENTER
        band.setPadding(0, 28, 0, 28)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP

        try {
            windowManager.addView(band, params)
            overlayView = band
            status = "화면 덮기를 표시했습니다 (${timeText()}, 5초 뒤 사라짐). 내비 위에 보였나요?"
            mainThread.postDelayed({ removeOverlay() }, 5000)
        } catch (e: Exception) {
            status = "화면 덮기 실패 (${e.message})"
        }
        refresh()
    }

    private fun removeOverlay() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            status = "이 기기에는 설정 화면이 없습니다. 집 PC에서 adb 명령으로 허용해야 합니다"
            refresh()
        }
    }

    /** 권한 상태와 진행 상황을 보여준다. 설정에서 돌아왔을 때 Activity도 부른다. */
    fun refresh() = card.show(
        "알림 허용: ${notificationManager.areNotificationsEnabled()}, " +
            "다른 앱 위에 표시 허용: ${Settings.canDrawOverlays(activity)}\n$status"
    )
}
