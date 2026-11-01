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
)

/**
 * 긴급출동 송신 서비스 (사양서 S-2·S-3·S-5·S-6).
 * - 시작: 서버에 긴급출동을 시작하고(start_dispatch), 폰 GPS 위치를 1초마다 서버로 보낸다(send_position)
 * - 화면이 꺼지거나 다른 앱을 써도 계속 보낸다. 그래서 알림창에 '긴급출동 송신 중'을 띄우는 서비스로 만든다(S-5)
 * - 끝: 서버에 긴급출동을 끝낸다(end_dispatch). 서버는 위치를 바로 지운다
 * - 마지막으로 보낸 시각을 기억해, 5초 넘게 실패하면 경고한다(S-6)
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

    // 폰 GPS 위치가 바뀔 때마다 불린다
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

    // 1초마다: 가장 최근 위치를 보내고 상태를 고친다
    private val everySecond = object : Runnable {
        override fun run() {
            sendOnce()
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
        if (intent?.action == ACTION_STOP) stopDispatch() else startDispatch()
        return START_STICKY // 시스템이 멈췄다 살리면(intent 없음) 저장된 출동을 이어서 보낸다
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainThread.removeCallbacksAndMessages(null)
        runCatching { locationManager.removeUpdates(locationListener) }
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // 화면에서 위치 권한을 받은 뒤에만 시작한다
    private fun startDispatch() {
        startForegroundWithStatus("긴급출동을 시작하는 중")
        val code = store.regCode
        if (code == null) {
            finish("등록 코드가 없습니다")
            return
        }
        if (running) return
        running = true

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper())
        if (store.activeDispatchId == null) {
            server.startDispatch(
                code,
                onDone = { id ->
                    store.activeDispatchId = id
                    store.dispatchStartedMillis = System.currentTimeMillis()
                    Log.i(LOG_TAG, "긴급출동 시작: $id")
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

    private fun stopDispatch() {
        mainThread.removeCallbacks(everySecond)
        runCatching { locationManager.removeUpdates(locationListener) }
        running = false
        val code = store.regCode
        val dispatchId = store.activeDispatchId
        if (code == null || dispatchId == null) {
            finish(null)
            return
        }
        server.endDispatch(
            code, dispatchId, "manual",
            onDone = { finish(null) },
            onError = { message -> finish("긴급출동 끝내기 실패: $message") },
        )
    }

    /** 출동을 끝내고 서비스도 멈춘다 */
    private fun finish(error: String?) {
        Log.i(LOG_TAG, "긴급출동 끝 ${error ?: ""}")
        store.activeDispatchId = null
        lastError = error
        lastFix = null
        lastSuccessMillis = null
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
        val state = SendState(running && dispatchId != null, dispatchId, health, lastSuccessMillis, lastError, lastFix)
        latest = state
        listeners.forEach { it(state) }
        if (running) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, statusNotification(health.text))
        }
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
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("긴급출동 송신 중")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val LOG_TAG = "Gilteojwo"
        private const val CHANNEL = "send_status"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.gilteojwo.sender.STOP"
        private const val MOVING_METERS_PER_SECOND = 1f // 초속 1m(시속 3.6km)보다 빠를 때만 진행 방향을 믿는다

        var latest: SendState = SendState()
            private set
        private val listeners = mutableSetOf<(SendState) -> Unit>()
        fun addListener(listener: (SendState) -> Unit) = listeners.add(listener)
        fun removeListener(listener: (SendState) -> Unit) = listeners.remove(listener)

        fun start(context: Context) = context.startForegroundService(Intent(context, SendService::class.java))

        fun stop(context: Context) =
            context.startService(Intent(context, SendService::class.java).setAction(ACTION_STOP))
    }
}
