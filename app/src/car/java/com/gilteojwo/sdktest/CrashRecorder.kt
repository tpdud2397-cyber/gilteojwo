package com.gilteojwo.sdktest

import android.content.Context
import android.util.Log

/**
 * 진단용 도구: 앱이 오류로 꺼지면 원인을 저장해 두었다가, 다음에 켤 때 화면 맨 위에 보여준다.
 * 로그(Logcat)를 보지 않아도 꺼진 원인을 캡처할 수 있게 하려고 만들었다.
 *
 * 저장 내용은 한 칸뿐이다.
 * - 시험을 시작하기 직전에 "○○ 시작 직후 꺼짐"이라고 미리 적어 두고 (markStarting)
 * - 5초 동안 무사하면 지운다 (clearMark)
 * - 그 사이 자바 오류로 꺼지면 오류 내용으로 덮어쓴다 (install에서 등록한 처리기)
 * 그래서 다음에 켰을 때 이 칸에 글자가 남아 있으면 "지난번에 꺼졌다"는 뜻이다.
 */
object CrashRecorder {
    private const val PREFS_NAME = "crash_record"
    private const val KEY_LAST = "last_crash"

    /** 앱이 켜질 때 한 번 부른다. 처리되지 않은 오류가 나면 내용을 저장한 뒤 원래대로 앱을 끈다. */
    fun install(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val original = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val stack = Log.getStackTraceString(error).lines().take(30).joinToString("\n")
            // 앱이 곧 꺼지므로 commit()으로 바로 저장한다
            prefs.edit().putString(KEY_LAST, "${timeText()} 오류로 꺼짐 (스레드: ${thread.name})\n$stack").commit()
            original?.uncaughtException(thread, error)
        }
    }

    /** 시험 시작 직전에 부른다. 이대로 앱이 꺼지면 다음에 이 문구가 보인다. */
    fun markStarting(context: Context, what: String) {
        prefs(context).edit()
            .putString(KEY_LAST, "${timeText()} '$what' 시작 직후 앱이 꺼졌습니다 (자바 오류 기록 없음)")
            .commit()
    }

    /** 시작하고 5초 동안 무사하면 부른다. */
    fun clearMark(context: Context) {
        prefs(context).edit().remove(KEY_LAST).apply()
    }

    /** 지난번에 꺼진 기록을 꺼내고 지운다. 없으면 null */
    fun takeLastCrash(context: Context): String? {
        val text = prefs(context).getString(KEY_LAST, null)
        prefs(context).edit().remove(KEY_LAST).apply()
        return text
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
