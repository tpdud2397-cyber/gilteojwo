package com.gilteojwo.report

import com.gilteojwo.receive.VehicleType

/** 길을 열어준 기록 한 건. 위치는 남기지 않는다 (날짜 "2026-11-01", 시각 "15:31", 차종) */
data class YieldRecord(val date: String, val time: String, val type: VehicleType) {
    /** 저장용 한 줄. 예: "2026-11-01|15:31|AMBULANCE" */
    fun toLine(): String = "$date|$time|${type.name}"

    companion object {
        /** 저장된 한 줄을 읽는다. 모양이 틀리면 null (그 줄은 버린다) */
        fun fromLine(line: String): YieldRecord? {
            val parts = line.split("|")
            if (parts.size != 3) return null
            val type = VehicleType.entries.find { it.name == parts[2] } ?: return null
            return YieldRecord(parts[0], parts[1], type)
        }
    }
}

/** 하루치 기록 (리포트 화면 한 줄) */
data class DaySummary(val date: String, val records: List<YieldRecord>) {
    /** 예: "2026-11-02 · 2대 (구급차 1 · 소방차 1)" */
    fun text(): String = "$date · ${records.size}대 (${typeCounts(records)})"
}

/**
 * 양보 리포트 (사양서 R-17): 길을 열어준 기록. 오늘 몇 대, 지금까지 몇 대, 날짜별·차종별.
 * 기록은 차 안에만 저장한다 (YieldReportStore). 날짜는 "2026-11-01" 글자로 비교한다.
 */
data class YieldTally(val records: List<YieldRecord> = emptyList()) {
    val total: Int get() = records.size

    /** 기록을 한 건 더한다 */
    fun add(record: YieldRecord): YieldTally = YieldTally(records + record)

    fun todayCount(nowDate: String): Int = records.count { it.date == nowDate }

    /** 예: "오늘 구급차 1대에 길을 열어줬습니다". 여러 대면 "오늘 긴급차량 2대 … · 누적 5대" */
    fun message(nowDate: String, vehicleName: String): String {
        val count = todayCount(nowDate)
        return if (count <= 1 && total <= 1) {
            "오늘 $vehicleName ${count}대에 길을 열어줬습니다"
        } else {
            "오늘 긴급차량 ${count}대에 길을 열어줬습니다 · 누적 ${total}대"
        }
    }

    /** 날짜별로 묶는다. 최근 날짜부터 */
    fun days(): List<DaySummary> =
        records.groupBy { it.date }.map { (date, list) -> DaySummary(date, list) }.sortedByDescending { it.date }

    /** 차종별 누적. 예: "구급차 2 · 경찰차 1" */
    fun byTypeText(): String = typeCounts(records)
}

/** 차종별 수를 한 줄로 (구급차·소방차·경찰차 순서, 0대인 차종은 뺀다) */
private fun typeCounts(records: List<YieldRecord>): String =
    VehicleType.entries
        .map { type -> type to records.count { it.type == type } }
        .filter { it.second > 0 }
        .joinToString(" · ") { (type, count) -> "${type.koreanName} $count" }
