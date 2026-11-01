package com.gilteojwo.service

import com.gilteojwo.alert.AlertStage
import com.gilteojwo.alert.AlertState
import com.gilteojwo.alert.YieldAdvice
import com.gilteojwo.approach.ApproachInfo
import com.gilteojwo.mylocation.MyPosition
import com.gilteojwo.report.YieldTally
import com.gilteojwo.vehiclestate.VehicleState

/** 수신 서비스가 지금 알고 있는 것 전부. 화면은 이것만 받아서 그린다 */
data class ReceiveSnapshot(
    val serverStatus: String = "연결 준비",
    val vehicleCount: Int = 0, // 서버에서 받은 출동 중 긴급차량 수
    val locationSource: String = "준비 전",
    val myPosition: MyPosition? = null,
    val results: List<ApproachInfo> = emptyList(), // 차마다 거리·방향·도착 시간·접근 여부
    val vehicleState: VehicleState = VehicleState(),
    val alert: AlertState = AlertState(AlertStage.NONE, null),
    val advice: YieldAdvice? = null, // 양보 안내 문구 (양보·해제 단계)
    val problems: List<String> = emptyList(), // 시작하다 실패한 것 (화면에 보여준다)
    val tally: YieldTally = YieldTally(), // 길을 열어준 횟수 (R-17)
    val yieldedVehicleId: String? = null, // 이번에 양보한 긴급차량 (해제 화면에 리포트를 띄울지)
    val reportLine: String? = null, // 해제 때 보여줄 "오늘 구급차 1대에 길을 열어줬습니다"
)
