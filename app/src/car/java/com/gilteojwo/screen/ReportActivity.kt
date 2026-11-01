package com.gilteojwo.screen

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.gilteojwo.report.YieldReportStore

/**
 * 양보 리포트 화면 (사양서 '화면 목록'의 양보 리포트, R-17): 길을 열어준 기록과 누적 횟수.
 * - 누적 몇 대, 오늘 몇 대, 차종별 수
 * - 날짜별 기록 (최근 날짜부터)
 * 기록은 차 안에만 저장한다. 위치는 남기지 않는다.
 */
class ReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tally = YieldReportStore(this).load()
        val today = YieldReportStore.today()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 24, 48, 24)

        val back = Button(this)
        back.text = "← 돌아가기"
        back.textSize = 20f
        back.setTextColor(Color.WHITE)
        back.setBackgroundColor(Color.parseColor("#424242"))
        back.gravity = Gravity.CENTER
        back.setOnClickListener { finish() }
        root.addView(back, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120))

        addText(root, 32f, bold = true).text = "양보 리포트"
        if (tally.total == 0) {
            addText(root, 26f).text = "아직 기록이 없습니다. 알림 중에 양보하면 여기에 남습니다."
        } else {
            addText(root, 40f, bold = true).apply {
                text = "지금까지 긴급차량 ${tally.total}대에 길을 열어줬습니다"
                setTextColor(GREEN)
            }
            addText(root, 26f).text = "오늘 ${tally.todayCount(today)}대 · ${tally.byTypeText()}"
            addText(root, 26f, bold = true).text = "날짜별 기록"
            for (day in tally.days()) addText(root, 24f).text = day.text()
        }
        addText(root, 18f).apply {
            text = "기록은 이 차 안에만 저장합니다. 위치는 남기지 않습니다."
            setTextColor(Color.GRAY)
        }

        // 기록이 길어지면 아래로 넘겨 볼 수 있게
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun addText(parent: LinearLayout, sizeSp: Float, bold: Boolean = false): TextView {
        val text = TextView(this)
        text.textSize = sizeSp
        text.setPadding(0, 16, 0, 8)
        if (bold) text.setTypeface(null, Typeface.BOLD)
        parent.addView(text)
        return text
    }

    companion object {
        private val GREEN = Color.parseColor("#4CAF50")
    }
}
