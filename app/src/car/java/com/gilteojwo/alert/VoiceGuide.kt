package com.gilteojwo.alert

import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale
import ai.pleos.playground.tts.TextToSpeech as PleosTts
import android.speech.tts.TextToSpeech as AndroidTts

/**
 * 음성 안내 (사양서 R-12): Pleos 음성(TTS, ON_DEVICE)으로 말한다. 안 되면 안드로이드 기본 음성으로 말한다.
 * 시험 화면 ④(SpeechTest)에서 확인한 방법이다. Pleos TTS는 initialize()를 먼저 불러야 한다 (R2에서 꺼진 원인).
 * 새 문장은 하던 말을 끊고 바로 말한다 (양보 안내가 늦게 나오지 않게).
 */
class VoiceGuide(private val context: Context) {
    private val mainThread = Handler(Looper.getMainLooper())
    private var pleosTts: PleosTts? = null
    private var pleosReady = false
    private var androidTts: AndroidTts? = null
    private var androidReady = false

    private val pleosEvents = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = Unit
        override fun onStop() = Unit
        override fun onError(errorType: ErrorType) {
            Log.w(LOG_TAG, "Pleos 음성 오류: $errorType")
        }
    }

    fun start() {
        // 1) Pleos 음성: 만들기 → 초기화 → (성공하면) 한국어
        val tts = PleosTts(context, Mode.ON_DEVICE)
        pleosTts = tts
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                tts.setLocale(Locale.KOREA)
                pleosReady = true
            }

            override fun onFailure(errorType: ErrorType) {
                Log.w(LOG_TAG, "Pleos 음성 초기화 실패: $errorType")
            }
        })
        tts.addEventListener(pleosEvents)

        // 2) 예비: 안드로이드 기본 음성
        androidTts = AndroidTts(context) { status ->
            if (status == AndroidTts.SUCCESS) {
                androidTts?.setLanguage(Locale.KOREAN)
                androidReady = true
            }
        }
    }

    /** afterBeep: 알림음과 같이 나올 때는 알림음이 끝난 뒤(0.6초 뒤)에 말한다 */
    fun speak(text: String, afterBeep: Boolean) {
        val delay = if (afterBeep) BEEP_MILLIS else 0L
        mainThread.postDelayed({ speakNow(text) }, delay)
    }

    private fun speakNow(text: String) {
        val pleos = pleosTts
        when {
            pleosReady && pleos != null -> pleos.speak(text, true)
            androidReady -> androidTts?.speak(text, AndroidTts.QUEUE_FLUSH, null, "gilteojwo")
            else -> Log.w(LOG_TAG, "음성 준비 전이라 말하지 못함: $text")
        }
        Log.i(LOG_TAG, "음성: $text")
    }

    fun stop() {
        mainThread.removeCallbacksAndMessages(null)
        runCatching {
            pleosTts?.removeEventListener(pleosEvents)
            pleosTts?.release()
        }
        runCatching { androidTts?.shutdown() }
    }

    companion object {
        private const val LOG_TAG = "Gilteojwo"
        private const val BEEP_MILLIS = 600L
    }
}
