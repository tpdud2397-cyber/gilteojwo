package com.gilteojwo.sender

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.gilteojwo.R
import com.gilteojwo.send.DemoRoute
import com.gilteojwo.send.DispatchCheck
import com.gilteojwo.send.SendHealth
import com.gilteojwo.send.SendHealthState
import com.gilteojwo.send.SenderFix
import com.gilteojwo.send.SenderServer
import com.gilteojwo.send.SenderStore

/** 송신 상태 (화면에 보여줄 것) */
data class SendState(
    val isDispatching: Boolean = false,
    val dispatchId: String? = null,
    val health: SendHealthState = SendHealthState(false, "대기 중"),
    val lastSuccessMillis: Long? = null,
    val lastError: String? = null,
    val fix: SenderFix? = null,
    val demoRoute: DemoRoute? = null, // 시연 모드면 재생 중인 경로 (S-4)
    val demoSecondsLeft: Int = 0,
    val check: DispatchCheck = DispatchCheck.Ok, // 출동 확인 (S-7)
    val notifiedCount: Int = 0, // 길 열림 현황 (S-8)
    val yieldedCount: Int = 0,
)

/**
 * 긴급출동 송신 서비스 (사양서 S-2~S-8).
 * - 시작: 서버에 긴급출동을 시작하고(start_dispatch), 위치를 1초마다 보낸다(send_position)
 *   · 보통: 폰 GPS 위치 (S-3)
 *   · 시연 모드: 미리 정한 경로를 실제 속도로 재생한 위치. 경로가 끝나면 긴급출동도 끝낸다 (S-4)
 * - 화면이 꺼지거나 다른 앱을 써도 계속 보낸다. 알림창에 '긴급출동 송신 중'을 띄우는 서비스다 (S-5)
 * - 5초 넘게 보내지 못하면 경고한다 (S-6)
 * - 60분이 지나면 "아직 출동 중인가요?"를 소리와 함께 묻고, 2분 안에 답이 없으면 '자동 종료'로 끝낸다 (S-7)
 * - 3초마다 서버에서 '알림 받은 차 수 · 양보한 차 수'를 받아 보여준다 (S-8)
 * 긴급출동 모드가 꺼져 있으면 아무것도 보내지 않는다 (위치 추적 방지).
 */
class SendService : Service() {
    private val server = SenderServer()
    private val mainThread = Handler(Looper.getMainLooper())
    private lateinit var store: SenderStore
    private lateinit var locationManager: LocationManager

    private var lastFix: SenderFix? = null
    private var lastHeading = 0.0
    private var lastSuccessMillis: Long? = null
    private var lastError: String? = null
    private var sending = false // 앞의 보내기가 끝나기 전에는 새로 보내지 않는다
    private var running = false

    private var demoRoute: DemoRoute? = null
    private var demoStartedMillis = 0L
    private var checkAfterMillis = DispatchCheck.CHECK_AFTER_MILLIS
    private var answerWithinMillis = DispatchCheck.ANSWER_WITHIN_MILLIS
    private var check: DispatchCheck = DispatchCheck.Ok
    private var notifiedCount = 0
    private var yieldedCount = 0
    private var seconds = 0

    // 폰 GPS 위치가 바뀔 때마다 불린다 (시연 모드가 아닐 때)
    private val locationListener = LocationListener { location ->
        // 진행 방향은 움직이고 있을 때만 믿는다 (서 있으면 GPS 방향이 흔들린다)
        if (location.hasBearing() && location.speed > MOVING_METERS_PER_SECOND) lastHeading = location.bearing.toDouble()
        lastFix = SenderFix(
            latitude = location.latitude,
            longitude = location.longitude,
            headingDegrees = lastHeading,
            speedKmh = location.speed * 3.6,
            accuracyMeters = location.accuracy.toDouble(),
        )
    }

    // 1초마다: 위치 정하기 → 보내기 → 출동 확인 → (3초마다) 길 열림 현황 → 상태 고치기
    private val everySecond = object : Runnable {
        override fun run() {
            seconds++
            if (!updateDemoFix()) return // 시연 경로가 끝나면 출동을 끝낸다
            sendOnce()
            if (!checkDispatch()) return // 답이 없어 자동 종료했으면 멈춘다
            if (seconds % STATUS_EVERY_SECONDS == 0) loadStatus()
            publish()
            mainThread.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = SenderStore(this)
        locationManager = getSystemService(LocationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopDispatch("manual")
            ACTION_CONFIRM -> confirmDispatch()
            else -> startDispatch(intent)
        }
        return START_STICKY // 시스템이 멈췄다 살리면(intent 없음) 저장된 출동을 이어서 보낸다
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainThread.removeCallbacksAndMessages(null)
        runCatching { locationManager.removeUpdates(locationListener) }
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // 화면에서 위치 권한을 받은 뒤에만 시작한다
    private fun startDispatch(intent: Intent?) {
        startForegroundWithStatus("긴급출동을 시작하는 중")
        val code = store.regCode
        if (code == null) {
            finish("등록 코드가 없습니다")
            return
        }
        if (running) return
        running = true

        demoRoute = intent?.getStringExtra(EXTRA_DEMO_ROUTE)?.let { DemoRoute.valueOf(it) }
        demoStartedMillis = System.currentTimeMillis()
        // 시험용: 출동 확인 시간을 줄일 수 있다 (adb로 시작할 때만 쓴다)
        intent?.getIntExtra(EXTRA_CHECK_AFTER_SECONDS, 0)?.takeIf { it > 0 }?.let { checkAfterMillis = it * 1000L }
        intent?.getIntExtra(EXTRA_ANSWER_SECONDS, 0)?.takeIf { it > 0 }?.let { answerWithinMillis = it * 1000L }

        if (demoRoute == null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper())
        }
        if (store.activeDispatchId == null) {
            server.startDispatch(
                code,
                onDone = { id ->
                    val now = System.currentTimeMillis()
                    store.activeDispatchId = id
                    store.dispatchStartedMillis = now
                    store.lastConfirmedMillis = now
                    Log.i(LOG_TAG, "긴급출동 시작: $id ${demoRoute?.let { "(시연: ${it.koreanName})" } ?: ""}")
                    publish()
                },
                onError = { message ->
                    lastError = "긴급출동 시작 실패: $message"
                    publish()
                },
            )
        }
        mainThread.post(everySecond)
    }

    /** 시연 모드면 지금 시각에 맞는 경로 위치를 정한다. 경로가 끝났으면 출동을 끝내고 false */
    private fun updateDemoFix(): Boolean {
        val route = demoRoute ?: return true
        val elapsed = (System.currentTimeMillis() - demoStartedMillis) / 1000.0
        val fix = route.fixAt(DemoRoute.DEMO_TARGET, elapsed)
        if (fix == null) {
            Log.i(LOG_TAG, "시연 경로 끝: ${route.koreanName}")
            stopDispatch("manual")
            return false
        }
        lastFix = fix
        return true
    }

    private fun sendOnce() {
        val code = store.regCode ?: return
        val dispatchId = store.activeDispatchId ?: return
        val fix = lastFix ?: return
        if (sending) return
        sending = true
        server.sendPosition(
            code, dispatchId, fix,
            onDone = {
                sending = false
                lastSuccessMillis = System.currentTimeMillis()
                lastError = null
            },
            onError = { message ->
                sending = false
                lastError = message
            },
        )
    }

    /** S-7 출동 확인. 답이 없어 자동 종료했으면 false */
    private fun checkDispatch(): Boolean {
        if (store.activeDispatchId == null) return true
        val previous = check
        check = DispatchCheck.of(store.lastConfirmedMillis, System.currentTimeMillis(), checkAfterMillis, answerWithinMillis)
        when (val now = check) {
            is DispatchCheck.Asking -> if (previous !is DispatchCheck.Asking) showCheckQuestion(now.secondsLeft)
            DispatchCheck.Expired -> {
                Log.i(LOG_TAG, "출동 확인에 답이 없어 자동 종료")
                stopDispatch("auto")
                return false
            }
            DispatchCheck.Ok -> Unit
        }
        return true
    }

    /** "출동 중"을 눌렀다: 지금부터 다시 60분 */
    private fun confirmDispatch() {
        store.lastConfirmedMillis = System.currentTimeMillis()
        check = DispatchCheck.Ok
        getSystemService(NotificationManager::class.java).cancel(CHECK_NOTIFICATION_ID)
        Log.i(LOG_TAG, "출동 확인: 출동 중 (연장)")
        publish()
    }

    /** S-8 서버에서 알림 받은 차 수와 양보한 차 수를 받는다 */
    private fun loadStatus() {
        val code = store.regCode ?: return
        val dispatchId = store.activeDispatchId ?: return
        server.dispatchStatus(
            code, dispatchId,
            onDone = { notified, yielded ->
                notifiedCount = notified
                yieldedCount = yielded
            },
            onError = { message -> Log.w(LOG_TAG, "길 열림 현황 받기 실패: $message") },
        )
    }

    /** endType: "manual"(직접 끔, 시연 끝) 또는 "auto"(출동 확인에 답 없음) */
    private fun stopDispatch(endType: String) {
        mainThread.removeCallbacks(everySecond)
        runCatching { locationManager.removeUpdates(locationListener) }
        getSystemService(NotificationManager::class.java).cancel(CHECK_NOTIFICATION_ID)
        running = false
        val code = store.regCode
        val dispatchId = store.activeDispatchId
        if (code == null || dispatchId == null) {
            finish(null)
            return
        }
        server.endDispatch(
            code, dispatchId, endType,
            onDone = { finish(if (endType == "auto") "출동 확인에 답이 없어 자동으로 끝냈습니다" else null) },
            onError = { message -> finish("긴급출동 끝내기 실패: $message") },
        )
    }

    /** 출동을 끝내고 서비스도 멈춘다 */
    private fun finish(message: String?) {
        Log.i(LOG_TAG, "긴급출동 끝 ${message ?: ""}")
        store.activeDispatchId = null
        lastError = message
        lastFix = null
        lastSuccessMillis = null
        demoRoute = null
        check = DispatchCheck.Ok
        notifiedCount = 0
        yieldedCount = 0
        publish()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** 새 상태를 화면에 넘기고 알림창 글자를 고친다 */
    private fun publish() {
        val dispatchId = store.activeDispatchId
        val health = if (dispatchId == null) {
            SendHealthState(lastError != null, lastError ?: "대기 중")
        } else {
            SendHealth.of(lastSuccessMillis, store.dispatchStartedMillis, System.currentTimeMillis())
        }
        val demoLeft = demoRoute?.let { (it.totalSeconds - (System.currentTimeMillis() - demoStartedMillis) / 1000).toInt().coerceAtLeast(0) } ?: 0
        val state = SendState(
            running && dispatchId != null, dispatchId, health, lastSuccessMillis, lastError, lastFix,
            demoRoute, demoLeft, check, notifiedCount, yieldedCount,
        )
        latest = state
        listeners.forEach { it(state) }
        if (running) {
            val text = "${health.text} · 알림 ${notifiedCount}대 · 양보 ${yieldedCount}대"
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, statusNotification(text))
        }
    }

    /** S-7 질문: 소리 나는 알림으로 묻고, 알림의 "출동 중" 버튼으로도 답할 수 있게 한다 */
    private fun showCheckQuestion(secondsLeft: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHECK_CHANNEL, "출동 확인", NotificationManager.IMPORTANCE_HIGH))
        val confirm = PendingIntent.getService(
            this, 1, Intent(this, SendService::class.java).setAction(ACTION_CONFIRM), PendingIntent.FLAG_IMMUTABLE,
        )
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, SenderActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHECK_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("아직 출동 중인가요?")
            .setContentText("${secondsLeft}초 안에 '출동 중'을 누르지 않으면 긴급출동을 자동으로 끕니다")
            .setContentIntent(openApp)
            .addAction(Notification.Action.Builder(null, "출동 중", confirm).build())
            .setAutoCancel(true)
            .build()
        manager.notify(CHECK_NOTIFICATION_ID, notification)
        Log.i(LOG_TAG, "출동 확인 질문")
    }

    private fun startForegroundWithStatus(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "긴급출동 송신 상태", NotificationManager.IMPORTANCE_LOW))
        val notification = statusNotification(text)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun statusNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, SenderActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val title = demoRoute?.let { "긴급출동 송신 중 · 시연 모드" } ?: "긴급출동 송신 중"
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val LOG_TAG = "Gilteojwo"
        private const val CHANNEL = "send_status"
        private const val CHECK_CHANNEL = "dispatch_check"
        private const val NOTIFICATION_ID = 1
        private const val CHECK_NOTIFICATION_ID = 2
        private const val ACTION_STOP = "com.gilteojwo.sender.STOP"
        private const val ACTION_CONFIRM = "com.gilteojwo.sender.CONFIRM"
        const val EXTRA_DEMO_ROUTE = "demo_route"
        const val EXTRA_CHECK_AFTER_SECONDS = "check_after_seconds" // 시험용
        const val EXTRA_ANSWER_SECONDS = "answer_seconds" // 시험용
        private const val MOVING_METERS_PER_SECOND = 1f // 초속 1m(시속 3.6km)보다 빠를 때만 진행 방향을 믿는다
        private const val STATUS_EVERY_SECONDS = 3

        var latest: SendState = SendState()
            private set
        private val listeners = mutableSetOf<(SendState) -> Unit>()
        fun addListener(listener: (SendState) -> Unit) = listeners.add(listener)
        fun removeListener(listener: (SendState) -> Unit) = listeners.remove(listener)

        /**
         * 긴급출동 시작. demoRoute를 주면 시연 모드 (폰 GPS 대신 경로 재생).
         * checkAfterSeconds·answerSeconds: 시험할 때만 출동 확인 시간을 줄인다 (0이면 60분·2분)
         */
        fun start(context: Context, demoRoute: DemoRoute? = null, checkAfterSeconds: Int = 0, answerSeconds: Int = 0) {
            val intent = Intent(context, SendService::class.java)
            demoRoute?.let { intent.putExtra(EXTRA_DEMO_ROUTE, it.name) }
            intent.putExtra(EXTRA_CHECK_AFTER_SECONDS, checkAfterSeconds)
            intent.putExtra(EXTRA_ANSWER_SECONDS, answerSeconds)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) =
            context.startService(Intent(context, SendService::class.java).setAction(ACTION_STOP))

        fun confirm(context: Context) =
            context.startService(Intent(context, SendService::class.java).setAction(ACTION_CONFIRM))
    }
}
