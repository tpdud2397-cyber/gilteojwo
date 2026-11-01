package com.gilteojwo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.gilteojwo.R
import com.gilteojwo.alert.AlertAnnouncer
import com.gilteojwo.alert.AlertStage
import com.gilteojwo.alert.AlertStageJudge
import com.gilteojwo.alert.AmbientCue
import com.gilteojwo.alert.AmbientLight
import com.gilteojwo.alert.EdgeGlow
import com.gilteojwo.alert.EdgeGlowOverlay
import com.gilteojwo.alert.MusicDucker
import com.gilteojwo.alert.OverNaviNotifier
import com.gilteojwo.alert.SoundPlayer
import com.gilteojwo.alert.VoiceGuide
import com.gilteojwo.alert.YieldDetector
import com.gilteojwo.alert.YieldStatus
import com.gilteojwo.alert.YieldGuide
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.ApproachTracker
import com.gilteojwo.mylocation.MyLocationTracker
import com.gilteojwo.mylocation.MyPosition
import com.gilteojwo.receive.DeadReckoning
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.EmergencyVehicleReceiver
import com.gilteojwo.receive.YieldAckSender
import com.gilteojwo.report.YieldReportStore
import com.gilteojwo.screen.WaitingActivity
import com.gilteojwo.vehiclestate.VehicleState
import com.gilteojwo.vehiclestate.VehicleStateReader
import kotlin.math.roundToInt

/**
 * 수신 서비스: 길터줘의 '두뇌'. 앱 화면을 열지 않아도(내비를 보는 중에도) 뒤에서 계속 돈다.
 * 1) 내 위치(R-1), 긴급차량(R-2), 내 차 상태(R-5~R-8)를 받는다
 * 2) 내 위치가 올 때마다(약 1초) 거리·방향·접근(R-3·R-4) → 알림 단계 → 양보 문구를 계산한다
 * 3) 내비 위 상단 카드와 가장자리 빛(R-10·R-11), 알림음과 음성(R-12), 음악 소리 줄이기(R-13), 앰비언트 조명(R-15)으로 알린다
 * 4) 알림 중 양보를 자동으로 감지해 서버에 익명으로 알리고(R-16), 길을 열어준 횟수를 차 안에 남긴다(R-17)
 * 5) 화면(WaitingActivity)이 열려 있으면 같은 결과를 넘겨 준다
 * 안드로이드는 화면 없이 도는 앱을 멈출 수 있어서, 알림창에 '길터줘 수신 중'을 띄워 두는 서비스(포그라운드 서비스)로 만든다.
 */
class ReceiveService : Service() {
    private lateinit var locationTracker: MyLocationTracker
    private lateinit var vehicleReceiver: EmergencyVehicleReceiver
    private lateinit var vehicleStateReader: VehicleStateReader
    private val approachTracker = ApproachTracker()
    private val alertJudge = AlertStageJudge()
    private val announcer = AlertAnnouncer()
    private val yieldDetector = YieldDetector()
    private val yieldAckSender = YieldAckSender()
    private lateinit var reportStore: YieldReportStore

    private lateinit var notifier: OverNaviNotifier
    private lateinit var edgeGlow: EdgeGlowOverlay
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var voiceGuide: VoiceGuide
    private lateinit var musicDucker: MusicDucker
    private lateinit var ambientLight: AmbientLight

    private var vehicles: List<EmergencyVehicle> = emptyList()
    private var snapshot = ReceiveSnapshot()
    private var statusText = "" // 알림창 '길터줘 수신 중' 아래 글자 (바뀔 때만 고친다)

    override fun onCreate() {
        super.onCreate()
        startForegroundWithStatus("시작하는 중")

        notifier = OverNaviNotifier(this)
        edgeGlow = EdgeGlowOverlay(this)
        soundPlayer = SoundPlayer()
        voiceGuide = VoiceGuide(this)
        musicDucker = MusicDucker(this)
        ambientLight = AmbientLight(this)
        reportStore = YieldReportStore(this)
        snapshot = snapshot.copy(tally = reportStore.load())
        locationTracker = MyLocationTracker(this) { position -> onMyPositionChanged(position) }
        vehicleReceiver = EmergencyVehicleReceiver { list, status -> onVehiclesChanged(list, status) }
        vehicleStateReader = VehicleStateReader(this) { state -> onVehicleStateChanged(state) }

        // 하나가 실패해도 나머지는 계속 돌게 따로따로 시작한다
        startPart("내 위치") { locationTracker.start() }
        startPart("긴급차량 수신") { vehicleReceiver.start() }
        startPart("내 차 상태") { vehicleStateReader.start() }
        startPart("음성") { voiceGuide.start() }
        startPart("앰비언트 조명") { ambientLight.start() }
        Log.i(LOG_TAG, "수신 서비스 시작")
    }

    // 시스템이 서비스를 멈췄다가 살리면 다시 시작하게 한다
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { locationTracker.stop() }
        runCatching { vehicleReceiver.stop() }
        runCatching { vehicleStateReader.stop() }
        runCatching { edgeGlow.stop() }
        runCatching { notifier.stop() }
        runCatching { voiceGuide.stop() }
        runCatching { musicDucker.lower(false) }
        runCatching { ambientLight.stop() }
        Log.i(LOG_TAG, "수신 서비스 끝")
        super.onDestroy()
    }

    private fun startPart(name: String, start: () -> Unit) {
        try {
            start()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "$name 시작 실패: ${e.message}")
            publish(snapshot.copy(problems = snapshot.problems + "$name 시작 실패 (${e.message})"))
        }
    }

    /** 내 위치가 새로 올 때마다(약 1초) 계산하고 알린다 */
    private fun onMyPositionChanged(position: MyPosition) {
        // R-14 송신이 잠깐 끊긴 차는 마지막 위치·속도로 추정하고, 5초 넘게 끊긴 차는 뺀다
        val current = vehicles.mapNotNull { DeadReckoning.estimate(it, position.timeMillis) }
        val results = approachTracker.update(position, current)
        val alert = alertJudge.update(results, position.timeMillis)
        val advice = YieldGuide.choose(alert, position.headingDegrees, snapshot.vehicleState)

        // R-16·R-17 양보 감지: 서버에 익명으로 알리고, 양보했으면 차 안에 횟수를 남긴다
        var tally = snapshot.tally
        var yieldedVehicleId = snapshot.yieldedVehicleId
        yieldDetector.update(alert, snapshot.vehicleState)?.let { status ->
            val target = alert.target
            target?.vehicle?.dispatchId?.let { yieldAckSender.send(it, status, position.point) }
            if (status == YieldStatus.YIELDED && target != null) {
                tally = reportStore.addYield()
                yieldedVehicleId = target.vehicle.id
            }
        }
        // 해제 때 양보했던 차였으면 "오늘 구급차 1대에 길을 열어줬습니다"를 카드에 덧붙인다
        val target = alert.target
        val reportLine = if (alert.stage == AlertStage.CLEARED && target != null && target.vehicle.id == yieldedVehicleId) {
            tally.message(YieldReportStore.today(), target.vehicle.type.koreanName)
        } else {
            null
        }

        // 알리기: 내비 위 카드, 가장자리 빛, 앰비언트 조명, 음악 줄이기, 소리·음성
        notifier.update(alert, advice, reportLine)
        edgeGlow.show(EdgeGlow.of(alert))
        ambientLight.show(AmbientCue.of(alert.stage))
        musicDucker.lower(AlertAnnouncer.lowersMusic(alert.stage))
        announcer.next(alert, advice, position.timeMillis)?.let { announcement ->
            announcement.sound?.let { soundPlayer.play(it) }
            announcement.speech?.let { voiceGuide.speak(it, afterBeep = announcement.sound != null) }
        }

        publish(
            snapshot.copy(
                myPosition = position,
                locationSource = locationTracker.sourceName,
                results = results,
                alert = alert,
                advice = advice,
                tally = tally,
                yieldedVehicleId = yieldedVehicleId,
                reportLine = reportLine,
            ),
        )
        // 집 PC에서 `adb logcat -s Gilteojwo`로 계산 결과를 시간순으로 볼 수 있다
        Log.i(
            LOG_TAG,
            "계산: 긴급차량 ${results.size}대 " + results.joinToString(" / ") { describe(it) } +
                " | 내 차: ${snapshot.vehicleState.describe()} | 알림: ${alertLine()}",
        )
    }

    /** 서버 목록이 바뀌면 기억만 해 두고, 계산은 다음 내 위치가 올 때(1초 안) 한다 */
    private fun onVehiclesChanged(list: List<EmergencyVehicle>, status: String) {
        vehicles = list
        publish(snapshot.copy(serverStatus = status, vehicleCount = list.size))
    }

    private fun onVehicleStateChanged(state: VehicleState) {
        publish(snapshot.copy(vehicleState = state))
    }

    /** 새 상태를 저장하고, 열려 있는 화면에 넘기고, 알림창 글자를 고친다 */
    private fun publish(newSnapshot: ReceiveSnapshot) {
        snapshot = newSnapshot
        latest = newSnapshot
        listeners.forEach { it(newSnapshot) }
        updateStatusNotification()
    }

    // ── 알림창의 '길터줘 수신 중' ──

    private fun startForegroundWithStatus(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(STATUS_CHANNEL, "길터줘 수신 상태", NotificationManager.IMPORTANCE_LOW),
        )
        val notification = statusNotification(text)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(STATUS_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(STATUS_NOTIFICATION_ID, notification)
        }
        statusText = text
    }

    private fun updateStatusNotification() {
        val text = "주변 긴급차량 ${snapshot.vehicleCount}대 · ${alertLine()}"
        if (text == statusText) return
        statusText = text
        getSystemService(NotificationManager::class.java).notify(STATUS_NOTIFICATION_ID, statusNotification(text))
    }

    private fun statusNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, WaitingActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, STATUS_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("길터줘 수신 중")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    // ── 기록·알림창용 글자 ──

    /** 예: "양보 · 구급차 후방(6시) 150m · 약 9초 · 우측으로 비켜주세요" */
    private fun alertLine(): String = describeAlert(snapshot)

    /** 예: "구급차 · 후방(6시) · 480m · 약 24초 · 접근 중" */
    private fun describe(info: ApproachInfo): String {
        val arrival = info.arrivalText ?: "도착 시간 없음"
        val state = if (info.isApproaching) "접근 중" else "접근 아님"
        val estimated = if (info.vehicle.isEstimated) " · 추정" else ""
        return "${info.vehicle.type.koreanName} · ${info.direction.koreanName}(${info.direction.clock}) · " +
            "${info.distanceMeters.roundToInt()}m · $arrival · $state$estimated"
    }

    companion object {
        const val LOG_TAG = "Gilteojwo"
        private const val STATUS_CHANNEL = "receive_status"
        private const val STATUS_NOTIFICATION_ID = 1

        /** 마지막 상태. 화면이 열릴 때 바로 그릴 수 있게 */
        var latest: ReceiveSnapshot? = null
            private set
        private val listeners = mutableSetOf<(ReceiveSnapshot) -> Unit>()

        /** 화면이 열려 있는 동안 새 상태를 받는다 (메인 스레드에서 불린다) */
        fun addListener(listener: (ReceiveSnapshot) -> Unit) = listeners.add(listener)
        fun removeListener(listener: (ReceiveSnapshot) -> Unit) = listeners.remove(listener)

        /** 수신 서비스를 시작한다. 이미 돌고 있으면 아무 일도 없다 */
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, ReceiveService::class.java))
            } catch (e: Exception) {
                Log.w(LOG_TAG, "수신 서비스 시작 실패: ${e.message}")
            }
        }

        /** 예: "양보 · 구급차 후방(6시) 150m · 약 9초 · 우측으로 비켜주세요". 화면에서도 쓴다 */
        fun describeAlert(snapshot: ReceiveSnapshot): String {
            val target = snapshot.alert.target ?: return snapshot.alert.stage.koreanName
            return listOfNotNull(
                snapshot.alert.stage.koreanName,
                "${target.vehicle.type.koreanName} ${target.direction.koreanName}(${target.direction.clock}) " +
                    "${target.distanceMeters.roundToInt()}m",
                target.arrivalText,
                snapshot.advice?.text,
            ).joinToString(" · ")
        }
    }
}
