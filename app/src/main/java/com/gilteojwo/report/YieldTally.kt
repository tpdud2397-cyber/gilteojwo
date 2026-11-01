package com.gilteojwo.report

/**
 * 양보 리포트 (사양서 R-17): 길을 열어준 횟수. 오늘 몇 대, 지금까지 몇 대.
 * 기록은 차 안에만 저장한다 (YieldReportStore). 날짜는 "2026-11-01" 글자로 비교한다.
 */
data class YieldTally(val total: Int = 0, val date: String = "", val today: Int = 0) {
    /** 양보를 한 번 더한다. 날짜가 바뀌었으면 오늘은 1부터 다시 센다 */
    fun add(nowDate: String): YieldTally =
        if (nowDate == date) copy(total = total + 1, today = today + 1) else YieldTally(total + 1, nowDate, 1)

    fun todayCount(nowDate: String): Int = if (nowDate == date) today else 0

    /** 예: "오늘 구급차 1대에 길을 열어줬습니다". 여러 대면 "오늘 긴급차량 2대 … · 누적 5대" */
    fun message(nowDate: String, vehicleName: String): String {
        val count = todayCount(nowDate)
        return if (count <= 1 && total <= 1) {
            "오늘 $vehicleName ${count}대에 길을 열어줬습니다"
        } else {
            "오늘 긴급차량 ${count}대에 길을 열어줬습니다 · 누적 ${total}대"
        }
    }
}
