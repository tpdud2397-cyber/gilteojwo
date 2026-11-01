package com.gilteojwo.alert

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 앰비언트 조명 (사양서 R-15, Pleos Vehicle SDK · Light).
 * 1) 시작할 때 조명 구역과 지금의 켜기·색·밝기를 저장해 둔다
 * 2) 알림 단계마다 색을 바꾼다: 관심 주황, 경고 빨강, 양보 빨강 깜빡임(1초마다 밝게·어둡게)
 * 3) 알림이 끝나면 저장해 둔 처음 값으로 되돌린다
 * 켜기·끄기는 Virtual Console에 안 보이므로 '끄기'가 아니라 '처음 값으로 되돌리기'로 끝낸다 (11/01 시험).
 * 바꾸기에는 CONTROL_CAR_LIGHTS 권한이 필요하다. 시험 화면 ③(AmbientLightTest)에서 확인한 방법이다.
 */
class AmbientLight(context: Context) {
    private val vehicle = Vehicle(context)
    private val mainThread = Handler(Looper.getMainLooper())

    // 조명 구역 (에뮬레이터는 구역 하나)
    private var switchAreas: List<SeatArea> = emptyList()
    private var colorAreas: List<SeatArea> = emptyList()
    private var brightnessBounds: Map<SeatArea, AreaBounds<Int>> = emptyMap()

    // 처음 값 (되돌리기용)
    private val savedSwitch = mutableMapOf<SeatArea, LightSwitch>()
    private val savedColor = mutableMapOf<SeatArea, Int>()
    private val savedBrightness = mutableMapOf<SeatArea, Int>()

    private var current: AmbientCue? = null
    private var blinkBright = true

    // 양보 단계: 1초마다 밝기를 최대·낮게 바꾼다
    private val blink = object : Runnable {
        override fun run() {
            blinkBright = !blinkBright
            setBrightness(if (blinkBright) BRIGHT else DIM)
            mainThread.postDelayed(this, BLINK_MILLIS)
        }
    }

    fun start() {
        vehicle.initialize()
        val light = vehicle.getLight()
        // 구역을 묻고, 구역마다 지금 값을 저장한다 (결과는 나중에 따로따로 온다)
        light.checkAmbientLightSwitchCapabilities(
            onComplete = { _, map ->
                switchAreas = map.keys.toList()
                switchAreas.forEach { area ->
                    light.getAmbientLightSwitch(area, onComplete = { v -> if (v != null) savedSwitch[area] = v }, onFailed = {})
                }
            },
            onFailed = { e -> Log.w(LOG_TAG, "앰비언트 켜기·끄기 조회 실패: ${e.message}") },
        )
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { _, list ->
                colorAreas = list
                list.forEach { area ->
                    light.getAmbientLightBrightnessColor(
                        area,
                        onComplete = { v ->
                            if (v != null) savedColor[area] = v
                            Log.i(LOG_TAG, "앰비언트 처음 색 저장: #%06X".format((v ?: 0) and 0xFFFFFF))
                        },
                        onFailed = {},
                    )
                }
            },
            onFailed = { e -> Log.w(LOG_TAG, "앰비언트 색 조회 실패: ${e.message}") },
        )
        light.checkAmbientLightBrightnessCapabilities(
            onComplete = { _, map ->
                brightnessBounds = map
                map.keys.forEach { area ->
                    light.getAmbientLightBrightness(
                        area,
                        onComplete = { v ->
                            if (v != null) savedBrightness[area] = v
                            Log.i(LOG_TAG, "앰비언트 처음 밝기 저장: $v")
                        },
                        onFailed = {},
                    )
                }
            },
            onFailed = { e -> Log.w(LOG_TAG, "앰비언트 밝기 조회 실패: ${e.message}") },
        )
    }

    /** 알림 단계가 바뀔 때 부른다. null이면 처음 값으로 되돌린다 */
    fun show(cue: AmbientCue?) {
        if (cue == current) return
        current = cue
        mainThread.removeCallbacks(blink)
        if (cue == null) return restore()

        Log.i(LOG_TAG, "앰비언트: #%06X%s".format(cue.color and 0xFFFFFF, if (cue.blinking) " 깜빡임" else ""))
        val light = vehicle.getLight()
        switchAreas.forEach { area -> light.setAmbientLightSwitch(area, LightSwitch.ON, onComplete = { _, _ -> }, onFailed = ::report) }
        colorAreas.forEach { area -> light.setAmbientLightBrightnessColor(area, cue.color, onComplete = { _, _ -> }, onFailed = ::report) }
        blinkBright = true
        setBrightness(BRIGHT)
        if (cue.blinking) mainThread.postDelayed(blink, BLINK_MILLIS)
    }

    fun stop() {
        show(null)
        mainThread.postDelayed({ runCatching { vehicle.release() } }, 1000) // 되돌리기가 끝날 시간을 준다
    }

    /** 밝기를 구역마다의 범위 안에서 정한다. level: 0.0(가장 어둡게) ~ 1.0(가장 밝게) */
    private fun setBrightness(level: Double) {
        brightnessBounds.forEach { (area, bounds) ->
            val value = bounds.minValue + ((bounds.maxValue - bounds.minValue) * level).toInt()
            vehicle.getLight().setAmbientLightBrightness(area, value, onComplete = { _, _ -> }, onFailed = ::report)
        }
    }

    private fun restore() {
        Log.i(LOG_TAG, "앰비언트: 처음 값으로 되돌림")
        val light = vehicle.getLight()
        savedSwitch.forEach { (area, v) -> light.setAmbientLightSwitch(area, v, onComplete = { _, _ -> }, onFailed = ::report) }
        savedColor.forEach { (area, v) -> light.setAmbientLightBrightnessColor(area, v, onComplete = { _, _ -> }, onFailed = ::report) }
        savedBrightness.forEach { (area, v) -> light.setAmbientLightBrightness(area, v, onComplete = { _, _ -> }, onFailed = ::report) }
    }

    private fun report(e: Exception) {
        Log.w(LOG_TAG, "앰비언트 바꾸기 실패: ${e.message}")
    }

    companion object {
        private const val LOG_TAG = "Gilteojwo"
        private const val BRIGHT = 1.0
        private const val DIM = 0.2
        private const val BLINK_MILLIS = 1000L // 차 조명은 반응이 느려서 1초 간격
    }
}
