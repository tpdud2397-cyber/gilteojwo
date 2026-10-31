package com.gilteojwo.sdktest

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager

/**
 * ⑦ 차량 상태 읽기 시험
 * Virtual Console에서 바꾼 값이 앱에 들어오는지 본다.
 * - 속도, 기어: 안드로이드 표준 차량 속성
 *   (Pleos 안내: 계기판도 PERF_VEHICLE_SPEED를 쓴다. Pleos의 기어 API는 폐지 예정이라 표준 GEAR_SELECTION을 쓴다)
 * - 브레이크 페달, 방향지시등: Pleos Vehicle SDK
 */
class VehicleStateTest(
    private val car: Car?,
    private val vehicle: Vehicle,
    private val card: TestCard,
) : SdkTest {
    private val speedName = "속도 (표준)"
    private val gearName = "기어 (표준)"
    private val brakeName = "브레이크 페달 (Pleos)"
    private val turnSignalName = "방향지시등 (Pleos)"

    // 항목 이름 → 화면에 보여줄 값 (넣은 순서대로 표시)
    private val values = linkedMapOf(
        speedName to "대기 중",
        gearName to "대기 중",
        brakeName to "대기 중",
        turnSignalName to "대기 중",
    )
    private var propertyManager: CarPropertyManager? = null

    // 표준 차량 속성(속도, 기어)이 바뀔 때마다 불린다
    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                    val metersPerSecond = value.value as? Float ?: return
                    update(speedName, "%.1f km/h".format(metersPerSecond * 3.6f))
                }
                VehiclePropertyIds.GEAR_SELECTION -> update(gearName, gearText(value.value as? Int))
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            val name = if (propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED) speedName else gearName
            update(name, "오류 이벤트")
        }
    }

    // Pleos 브레이크 페달 상태가 바뀔 때마다 불린다
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) {
            val text = when (status) {
                true -> "밟음"
                false -> "안 밟음"
                null -> "값 없음"
            }
            update(brakeName, text)
        }

        override fun onFailed(e: Exception) = update(brakeName, "오류: ${e.message}")
    }

    // Pleos 방향지시등 상태가 바뀔 때마다 불린다
    private val turnSignalListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            val text = when (state) {
                TurnSignalValue.LEFT -> "왼쪽"
                TurnSignalValue.RIGHT -> "오른쪽"
                TurnSignalValue.OFF -> "꺼짐"
                null -> "값 없음"
            }
            update(turnSignalName, text)
        }

        override fun onFailed(e: Exception) = update(turnSignalName, "오류: ${e.message}")
    }

    override fun start() {
        // 1) 표준 차량 속성 구독
        val manager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        propertyManager = manager
        if (manager == null) {
            update(speedName, "차량 서비스에 연결하지 못함")
            update(gearName, "차량 서비스에 연결하지 못함")
        } else {
            subscribe(manager, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL, speedName)
            subscribe(manager, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE, gearName)
        }

        // 2) Pleos Vehicle SDK 구독
        vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        vehicle.getTurnSignal().registerTurnSignalState(turnSignalListener)
    }

    override fun stop() {
        runCatching { propertyManager?.unregisterCallback(propertyCallback) }
        runCatching { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(turnSignalListener) }
    }

    private fun subscribe(manager: CarPropertyManager, propertyId: Int, rate: Float, name: String) {
        try {
            val ok = manager.registerCallback(propertyCallback, propertyId, rate)
            if (!ok) update(name, "구독 실패")
        } catch (e: SecurityException) {
            update(name, "권한 없음")
        } catch (e: Exception) {
            update(name, "구독 실패 (${e.message})")
        }
    }

    /** 안드로이드 표준 기어 값을 글자로 */
    private fun gearText(gear: Int?): String = when (gear) {
        1 -> "N"
        2 -> "R"
        4 -> "P"
        8 -> "D"
        null -> "값 없음"
        else -> "기타 ($gear)"
    }

    /** 값 하나를 바꾸고 표 전체를 다시 보여준다 (여러 스레드에서 동시에 불릴 수 있어 잠금) */
    @Synchronized
    private fun update(name: String, text: String) {
        values[name] = "$text   (${timeText()})"
        card.show(values.entries.joinToString("\n") { "${it.key}: ${it.value}" })
    }
}
