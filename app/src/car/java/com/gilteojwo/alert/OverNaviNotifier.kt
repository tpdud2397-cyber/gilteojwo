package com.gilteojwo.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gilteojwo.R
import com.gilteojwo.screen.WaitingActivity
import kotlin.math.roundToInt

/**
 * 내비 위 상단 알림 카드 (사양서 R-11).
 * 차량용 안드로이드는 알림 종류가 '내비'(CATEGORY_NAVIGATION)인 것만 내비 위에 카드로 띄운다 (11/01 시험).
 * 단계가 바뀌거나 양보 문구가 바뀔 때만 새 카드를 띄운다. 카드는 약 8초 뒤 저절로 들어가고 알림 목록에는 남는다.
 * 소리는 길터줘가 직접 내므로(오는 쪽 스피커) 알림 자체의 소리는 끈다.
 */
class OverNaviNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private var shownStage = AlertStage.NONE
    private var shownAdvice: String? = null
    private var notificationId = FIRST_ID

    init {
        val channel = NotificationChannel(CHANNEL, "긴급차량 접근 알림", NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(null, null)
        channel.enableVibration(false)
        manager.createNotificationChannel(channel)
    }

    /** note: 해제 때 덧붙일 한 줄 (예: "오늘 구급차 1대에 길을 열어줬습니다") */
    fun update(alert: AlertState, advice: YieldAdvice?, note: String? = null) {
        val target = alert.target
        if (alert.stage == AlertStage.NONE || target == null) {
            if (shownStage != AlertStage.NONE) manager.cancel(notificationId)
            shownStage = AlertStage.NONE
            shownAdvice = null
            return
        }
        val adviceText = listOfNotNull(advice?.text, note).joinToString(" · ").ifEmpty { null }
        if (alert.stage == shownStage && adviceText == shownAdvice) return
        shownStage = alert.stage
        shownAdvice = adviceText

        // 예: 제목 "구급차 접근 · 경고", 내용 "좌측 후방(7~8시) 400m · 약 24초"
        val title = "${target.vehicle.type.koreanName} 접근 · ${alert.stage.koreanName}"
        val text = listOfNotNull(
            adviceText,
            "${target.direction.koreanName}(${target.direction.clock}) ${target.distanceMeters.roundToInt()}m",
            target.arrivalText,
        ).joinToString(" · ")
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, WaitingActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .apply { if (alert.stage == AlertStage.CLEARED) setTimeoutAfter(CLEARED_TIMEOUT_MILLIS) }
            .build()

        // 매번 새 번호로 보낸다. 같은 번호로 고치면 카드가 다시 뜨지 않는다 (11/01 시험)
        manager.cancel(notificationId)
        notificationId += 1
        manager.notify(notificationId, notification)
    }

    fun stop() = manager.cancel(notificationId)

    companion object {
        private const val CHANNEL = "approach_alert"
        private const val FIRST_ID = 100 // 1번은 '길터줘 수신 중' 알림이 쓴다
        private const val CLEARED_TIMEOUT_MILLIS = 5_000L // 해제 알림은 5초 뒤 지운다
    }
}
