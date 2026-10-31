package com.gilteojwo.sdktest

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 로그 이름. 집 PC에서 `adb logcat -s Gilteojwo`로 시험 결과를 모아 볼 수 있다. */
const val LOG_TAG = "Gilteojwo"

/** 시험 항목 하나가 지켜야 할 약속: 시작과 정리 */
interface SdkTest {
    /** 값 받기 시작. 권한 요청이 끝난 뒤 한 번 불린다. */
    fun start()

    /** 화면을 닫을 때 정리한다. */
    fun stop()
}

/** 지금 시각을 "15:30:05" 모양으로 */
fun timeText(millis: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(millis))

/**
 * 시험 화면의 칸 하나. 제목, 결과 글자, 버튼 줄로 되어 있다.
 * show()는 어느 스레드에서 불러도 된다. (SDK 결과는 다른 스레드에서 올 수 있어서
 * 화면 글자는 항상 메인 스레드에서 바꾼다.)
 */
class TestCard(context: Context, private val title: String) {
    val view = LinearLayout(context)
    private val resultText = TextView(context)
    private val mainThread = Handler(Looper.getMainLooper())

    init {
        view.orientation = LinearLayout.VERTICAL
        view.setPadding(28, 20, 28, 20)
        view.setBackgroundColor(Color.parseColor("#26FFFFFF"))

        val titleText = TextView(context)
        titleText.text = title
        titleText.textSize = 22f
        titleText.setTypeface(null, Typeface.BOLD)
        view.addView(titleText)

        resultText.text = "시작 전"
        resultText.textSize = 17f
        resultText.setPadding(0, 8, 0, 8)
        view.addView(resultText)
    }

    /** 결과 글자를 바꾸고 로그에도 남긴다. */
    fun show(text: String) {
        Log.i(LOG_TAG, "[$title] $text")
        mainThread.post { resultText.text = text }
    }

    /** 버튼 한 줄을 추가한다. 예: addButtons("켜기" to { ... }, "끄기" to { ... }) */
    fun addButtons(vararg buttons: Pair<String, () -> Unit>) {
        val row = LinearLayout(view.context)
        row.orientation = LinearLayout.HORIZONTAL
        for ((label, onClick) in buttons) {
            val button = Button(view.context)
            button.text = label
            button.textSize = 16f
            button.isAllCaps = false
            // 버튼 하나가 오류를 내도 앱 전체가 꺼지지 않게, 오류는 이 칸에 글자로 보여준다
            button.setOnClickListener {
                try {
                    onClick()
                } catch (e: Exception) {
                    show("[$label] 오류: ${e.javaClass.simpleName} ${e.message}")
                }
            }
            row.addView(button)
        }
        view.addView(row)
    }
}
