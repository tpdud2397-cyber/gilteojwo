package com.gilteojwo.receive

/**
 * Pleos 송신 모드에서 내 차가 긴급차량으로 실제 출동 중이면, 내 차 위치는 수신 목록에서 뺀다.
 * 자기 자신에게 "뒤에서 구급차"라고 알리지 않게 하려는 것이다.
 * 시연 경로는 가상의 긴급차량이라 빼지 않는다. 그래서 차량 앱 한 대로 송신과 수신을 함께 시연할 수 있다.
 */
object OwnDispatchFilter {
    /** ownRealDispatchId: 내 차의 실제 출동 번호. 출동 중이 아니거나 시연 중이면 null */
    fun apply(vehicles: List<EmergencyVehicle>, ownRealDispatchId: String?): List<EmergencyVehicle> {
        if (ownRealDispatchId == null) return vehicles
        return vehicles.filter { it.dispatchId != ownRealDispatchId }
    }
}
