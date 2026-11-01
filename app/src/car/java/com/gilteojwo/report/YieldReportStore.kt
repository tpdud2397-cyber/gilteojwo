package com.gilteojwo.report

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 양보 리포트 저장 (사양서 R-17): 길을 열어준 횟수를 차 안(앱 저장 공간)에만 남긴다. 서버에는 보내지 않는다.
 */
class YieldReportStore(context: Context) {
    private val prefs = context.getSharedPreferences("yield_report", Context.MODE_PRIVATE)

    fun load(): YieldTally = YieldTally(
        total = prefs.getInt(KEY_TOTAL, 0),
        date = prefs.getString(KEY_DATE, "") ?: "",
        today = prefs.getInt(KEY_TODAY, 0),
    )

    /** 양보를 한 번 더하고 저장한 뒤 새 집계를 돌려준다 */
    fun addYield(): YieldTally {
        val tally = load().add(today())
        prefs.edit()
            .putInt(KEY_TOTAL, tally.total)
            .putString(KEY_DATE, tally.date)
            .putInt(KEY_TODAY, tally.today)
            .apply()
        return tally
    }

    companion object {
        private const val KEY_TOTAL = "total"
        private const val KEY_DATE = "date"
        private const val KEY_TODAY = "today"

        /** 오늘 날짜 글자. 예: "2026-11-01" */
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
    }
}
