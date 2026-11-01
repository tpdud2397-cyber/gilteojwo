package com.gilteojwo.alert

import com.gilteojwo.CITY_HALL
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.approach.RelativeDirection
import com.gilteojwo.receive.EmergencyVehicle
import com.gilteojwo.receive.VehicleType

/** 시험용: stage 단계에서 direction 쪽 meters 거리의 구급차를 알리고 있는 상태 */
fun alertOf(
    stage: AlertStage,
    direction: RelativeDirection = RelativeDirection.REAR,
    meters: Double = 300.0,
    approaching: Boolean = true,
    dispatchId: String = "출동1",
) = AlertState(
    stage,
    ApproachInfo(
        vehicle = EmergencyVehicle("구급차1", VehicleType.AMBULANCE, CITY_HALL, 0.0, 60.0, 0, dispatchId = dispatchId),
        distanceMeters = meters,
        direction = direction,
        arrivalSeconds = if (approaching) (meters / 16.7).toInt() else null,
        isApproaching = approaching,
    ),
)
