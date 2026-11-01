package com.gilteojwo.report

import android.content.Context
import com.gilteojwo.receive.VehicleType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 양보 리포트 저장 (사양서 R-17): 길을 열어준 기록을 차 안(앱 저장 공간)에만 남긴다. 서버에는 보내지 않는다.
 * 기록 한 건 = 한 줄 ("2026-11-01|15:31|AMBULANCE"). 위치는 남기지 않는다.
 */
class YieldReportStore(context: Context) {
    private val prefs = context.getSharedPreferences("yield_report", Context.MODE_PRIVATE)

    fun load(): YieldTally {
        val lines = prefs.getString(KEY_RECORDS, "") ?: ""
        return YieldTally(lines.lines().mapNotNull { YieldRecord.fromLine(it) })
    }

    /** 양보 기록을 한 건 더하고 저장한 뒤 새 집계를 돌려준다 */
    fun addYield(type: VehicleType): YieldTally {
        val now = Date()
        val tally = load().add(YieldRecord(DATE.format(now), TIME.format(now), type))
        prefs.edit().putString(KEY_RECORDS, tally.records.joinToString("\n") { it.toLine() }).apply()
        return tally
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private val DATE = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        private val TIME = SimpleDateFormat("HH:mm", Locale.KOREA)

        /** 오늘 날짜 글자. 예: "2026-11-01" */
        fun today(): String = DATE.format(Date())
    }
}
