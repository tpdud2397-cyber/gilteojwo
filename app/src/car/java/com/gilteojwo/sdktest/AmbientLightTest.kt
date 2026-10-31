package com.gilteojwo.sdktest

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.control.LightSwitch

/**
 * ③ 앰비언트 라이트 시험 (Pleos Vehicle SDK · Light)
 * 1) 지원 범위 조회: 구역(좌석 영역)별로 켜기·끄기, 밝기 범위, 색 변경을 지원하는지 본다.
 *    조회하면서 지금 값을 저장해 둔다 (원래대로 버튼용).
 * 2) 버튼으로 켜기·끄기·색·밝기를 바꾸고, Virtual Console에서 실제로 바뀌는지 본다.
 * 3) 원래대로: 저장해 둔 값으로 되돌린다.
 * 색은 32비트 ARGB 숫자로 보낸다 (예: 빨강 0xFFFF0000).
 */
class AmbientLightTest(private val vehicle: Vehicle, private val card: TestCard) : SdkTest {
    private val red = 0xFFFF0000.toInt()
    private val orange = 0xFFFF8000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    // 조회 결과: 기능별로 지원하는 구역
    private var switchAreas: List<SeatArea> = emptyList()
    private var brightnessBounds: Map<SeatArea, AreaBounds<Int>> = emptyMap()
    private var colorAreas: List<SeatArea> = emptyList()

    // 시험 전 원래 값 (처음 읽은 값만 저장)
    private val savedSwitch = mutableMapOf<SeatArea, LightSwitch>()
    private val savedBrightness = mutableMapOf<SeatArea, Int>()
    private val savedColor = mutableMapOf<SeatArea, Int>()

    // 화면에 보여줄 조회 결과와 최근 명령 결과
    private var capabilityText = "지원 범위: 조회 전"
    private val recentResults = ArrayDeque<String>()

    init {
        card.addButtons("지원 범위 조회" to ::checkCapabilities)
        card.addButtons("켜기" to { setSwitch(LightSwitch.ON) }, "끄기" to { setSwitch(LightSwitch.OFF) })
        card.addButtons("빨강" to { setColor(red, "빨강") }, "주황" to { setColor(orange, "주황") }, "흰색" to { setColor(white, "흰색") })
        card.addButtons("왼쪽만 빨강" to ::setLeftOnlyRed, "밝기 최대" to { setBrightness(toMax = true) }, "밝기 최소" to { setBrightness(toMax = false) })
        card.addButtons("원래대로" to ::restore)
    }

    override fun start() = checkCapabilities()

    override fun stop() {}

    /** 세 가지 기능의 지원 범위를 묻고, 지금 값을 저장한다 */
    private fun checkCapabilities() {
        val light = vehicle.getLight()
        val lines = mutableListOf<String>()
        fun addLine(text: String) {
            synchronized(lines) { lines.add(text) }
            capabilityText = "지원 범위:\n" + synchronized(lines) { lines.joinToString("\n") }
            refresh()
        }

        light.checkAmbientLightSwitchCapabilities(
            onComplete = { config, map ->
                switchAreas = map.keys.toList()
                addLine("  켜기·끄기: ${config.accessType}, " + map.entries.joinToString { "${areaName(it.key)} ${it.value}" })
                switchAreas.forEach { area ->
                    light.getAmbientLightSwitch(area, onComplete = { v -> if (v != null) savedSwitch.putIfAbsent(area, v) }, onFailed = {})
                }
            },
            onFailed = { e -> addLine("  켜기·끄기: 조회 실패 (${e.message})") },
        )
        light.checkAmbientLightBrightnessCapabilities(
            onComplete = { config, map ->
                brightnessBounds = map
                addLine("  밝기: ${config.accessType}, " + map.entries.joinToString { "${areaName(it.key)} ${it.value.minValue}~${it.value.maxValue}" })
                map.keys.forEach { area ->
                    light.getAmbientLightBrightness(area, onComplete = { v -> if (v != null) savedBrightness.putIfAbsent(area, v) }, onFailed = {})
                }
            },
            onFailed = { e -> addLine("  밝기: 조회 실패 (${e.message})") },
        )
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { config, list ->
                colorAreas = list
                addLine("  색: ${config.accessType}, 구역 " + list.joinToString { areaName(it) })
                list.forEach { area ->
                    light.getAmbientLightBrightnessColor(area, onComplete = { v -> if (v != null) savedColor.putIfAbsent(area, v) }, onFailed = {})
                }
            },
            onFailed = { e -> addLine("  색: 조회 실패 (${e.message})") },
        )
    }

    private fun setSwitch(value: LightSwitch) {
        if (switchAreas.isEmpty()) return report("켜기·끄기 지원 구역이 없습니다 (먼저 지원 범위 조회)")
        switchAreas.forEach { area ->
            vehicle.getLight().setAmbientLightSwitch(
                area, value,
                onComplete = { a, v -> report("${areaName(a)} 켜기·끄기 → $v 성공") },
                onFailed = { e -> report("${areaName(area)} 켜기·끄기 실패 (${e.message})") },
            )
        }
    }

    private fun setColor(argb: Int, colorName: String) {
        if (colorAreas.isEmpty()) return report("색 지원 구역이 없습니다 (먼저 지원 범위 조회)")
        colorAreas.forEach { area -> sendColor(area, argb, colorName) }
    }

    /** 구역 구분이 되는지 확인: 왼쪽 좌석 구역만 빨강, 나머지는 흰색 */
    private fun setLeftOnlyRed() {
        if (colorAreas.isEmpty()) return report("색 지원 구역이 없습니다 (먼저 지원 범위 조회)")
        if (colorAreas.size == 1) report("색 구역이 하나뿐이라 좌우 구분이 안 됩니다 (${areaName(colorAreas[0])})")
        colorAreas.forEach { area ->
            val isLeft = area.values.any { it.name.endsWith("LEFT") }
            if (isLeft) sendColor(area, red, "빨강") else sendColor(area, white, "흰색")
        }
    }

    private fun sendColor(area: SeatArea, argb: Int, colorName: String) {
        vehicle.getLight().setAmbientLightBrightnessColor(
            area, argb,
            onComplete = { a, v -> report("${areaName(a)} 색 → $colorName (${colorHex(v)}) 성공") },
            onFailed = { e -> report("${areaName(area)} 색 → $colorName 실패 (${e.message})") },
        )
    }

    private fun setBrightness(toMax: Boolean) {
        if (brightnessBounds.isEmpty()) return report("밝기 지원 구역이 없습니다 (먼저 지원 범위 조회)")
        brightnessBounds.forEach { (area, bounds) ->
            val value = if (toMax) bounds.maxValue else bounds.minValue
            vehicle.getLight().setAmbientLightBrightness(
                area, value,
                onComplete = { a, v -> report("${areaName(a)} 밝기 → $v 성공") },
                onFailed = { e -> report("${areaName(area)} 밝기 → $value 실패 (${e.message})") },
            )
        }
    }

    /** 처음에 저장한 값으로 되돌린다 */
    private fun restore() {
        if (savedSwitch.isEmpty() && savedBrightness.isEmpty() && savedColor.isEmpty()) {
            return report("저장된 원래 값이 없습니다")
        }
        val light = vehicle.getLight()
        savedSwitch.forEach { (area, v) ->
            light.setAmbientLightSwitch(area, v, onComplete = { _, _ -> }, onFailed = { e -> report("켜기·끄기 복원 실패 (${e.message})") })
        }
        savedBrightness.forEach { (area, v) ->
            light.setAmbientLightBrightness(area, v, onComplete = { _, _ -> }, onFailed = { e -> report("밝기 복원 실패 (${e.message})") })
        }
        savedColor.forEach { (area, v) ->
            light.setAmbientLightBrightnessColor(area, v, onComplete = { _, _ -> }, onFailed = { e -> report("색 복원 실패 (${e.message})") })
        }
        report("원래 값으로 되돌리기 요청함 (켜기·끄기 ${savedSwitch.size}, 밝기 ${savedBrightness.size}, 색 ${savedColor.size}개 구역)")
    }

    /** 좌석 영역 이름을 짧게: ROW_1_LEFT → 1열좌, ROW_2_CENTER → 2열중 */
    private fun areaName(area: SeatArea): String = area.values.joinToString(",") { seat ->
        val row = seat.name.removePrefix("ROW_").substringBefore("_")
        val side = when {
            seat.name.endsWith("LEFT") -> "좌"
            seat.name.endsWith("RIGHT") -> "우"
            else -> "중"
        }
        "${row}열$side"
    }

    private fun colorHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    /** 명령 결과를 최근 6줄까지 보여준다 */
    @Synchronized
    private fun report(text: String) {
        recentResults.addFirst("${timeText()} $text")
        while (recentResults.size > 6) recentResults.removeLast()
        refresh()
    }

    @Synchronized
    private fun refresh() = card.show(capabilityText + "\n\n최근 결과:\n" + recentResults.joinToString("\n"))
}
