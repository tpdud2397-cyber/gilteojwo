package com.gilteojwo.vehiclestate

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 내 차 상태 읽기 (R-5 속도, R-6 브레이크, R-7 방향지시등, R-8 기어).
 * - 속도, 기어: 안드로이드 표준 차량 속성 (Pleos 안내: 계기판도 PERF_VEHICLE_SPEED를 쓰고, Pleos 기어 API는 폐지 예정)
 * - 브레이크 페달, 방향지시등: Pleos Vehicle SDK
 * 값이 하나라도 바뀌면 전체 상태를 onChanged로 알려준다. 항상 메인 스레드에서 부른다.
 * 시험 화면 ⑦(sdktest/VehicleStateTest)에서 확인한 방법을 본 앱용으로 옮겼다.
 */
class VehicleStateReader(private val context: Context, private val onChanged: (VehicleState) -> Unit) {
    private val mainThread = Handler(Looper.getMainLooper())
    private var car: Car? = null // 안드로이드 차량 서비스
    private var propertyManager: CarPropertyManager? = null
    private val vehicle = Vehicle(context) // Pleos Vehicle SDK
    private var state = VehicleState()

    // 속도·기어가 바뀔 때마다 불린다
    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                // 속도는 초속(m/s)으로 온다 → 시속(km/h)으로 바꾼다
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                    val metersPerSecond = value.value as? Float ?: return
                    update { it.copy(speedKmh = metersPerSecond * 3.6) }
                }
                VehiclePropertyIds.GEAR_SELECTION -> update { it.copy(gear = Gear.fromAndroid(value.value as? Int)) }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(LOG_TAG, "차량 속성 오류 (번호 $propertyId)")
        }
    }

    // 브레이크 페달을 밟았는지 바뀔 때마다 불린다
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) = update { it.copy(brakePressed = status) }
        override fun onFailed(e: Exception) {
            Log.w(LOG_TAG, "브레이크 읽기 실패: ${e.message}")
        }
    }

    // 방향지시등이 바뀔 때마다 불린다
    private val turnSignalListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            val signal = when (state) {
                TurnSignalValue.LEFT -> TurnSignal.LEFT
                TurnSignalValue.RIGHT -> TurnSignal.RIGHT
                TurnSignalValue.OFF -> TurnSignal.OFF
                null -> TurnSignal.UNKNOWN
            }
            update { it.copy(turnSignal = signal) }
        }

        override fun onFailed(e: Exception) {
            Log.w(LOG_TAG, "방향지시등 읽기 실패: ${e.message}")
        }
    }

    fun start() {
        // 1) 안드로이드 표준: 속도는 자주(보통 빠르기), 기어는 바뀔 때만
        val connectedCar = Car.createCar(context)
        car = connectedCar
        val manager = connectedCar?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        propertyManager = manager
        if (manager == null) {
            Log.w(LOG_TAG, "차량 서비스에 연결하지 못해 속도·기어를 읽을 수 없음")
        } else {
            subscribe(manager, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
            subscribe(manager, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        }

        // 2) Pleos Vehicle SDK: 시작한 뒤 브레이크·방향지시등을 구독한다 (연결이 끝나면 SDK가 알아서 등록한다)
        vehicle.initialize()
        vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        vehicle.getTurnSignal().registerTurnSignalState(turnSignalListener)
    }

    fun stop() {
        runCatching { propertyManager?.unregisterCallback(propertyCallback) }
        runCatching { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(turnSignalListener) }
        runCatching { vehicle.release() }
        runCatching { car?.disconnect() }
    }

    private fun subscribe(manager: CarPropertyManager, propertyId: Int, rate: Float) {
        try {
            if (!manager.registerCallback(propertyCallback, propertyId, rate)) {
                Log.w(LOG_TAG, "차량 속성 구독 실패 (번호 $propertyId)")
            }
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "차량 속성 권한 없음 (번호 $propertyId)")
        }
    }

    /** 값 하나를 바꾸고 전체 상태를 알려준다. 콜백은 여러 스레드에서 오므로 메인 스레드에서 차례로 처리한다 */
    private fun update(change: (VehicleState) -> VehicleState) {
        mainThread.post {
            state = change(state)
            onChanged(state)
        }
    }

    companion object {
        const val LOG_TAG = "Gilteojwo"
    }
}
