package com.gilteojwo.sdktest

import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import ai.pleos.playground.tts.TextToSpeech as PleosTts
import android.speech.tts.TextToSpeech as AndroidTts

/**
 * ④ 한국어 음성 시험
 * 같은 문장을 Pleos TTS SDK와 안드로이드 기본 TTS로 읽어서 비교한다.
 * Pleos TTS는 ON_DEVICE 모드를 쓴다 (인터넷과 앱 등록이 필요 없다).
 */
class SpeechTest(private val context: Context, private val card: TestCard) : SdkTest {
    private val sentence = "오른쪽 뒤에서 구급차, 300미터"

    private var pleosTts: PleosTts? = null
    private var androidTts: AndroidTts? = null
    private var pleosState = "준비 전"
    private var androidState = "준비 전"

    // Pleos TTS 진행 상황
    private val pleosEvents = object : EventListener {
        override fun onReady() = setPleos("준비됨")
        override fun onStart() = setPleos("말하는 중")
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = setPleos("끝까지 말함")
        override fun onStop() = setPleos("멈춤")
        override fun onError(errorType: ErrorType) = setPleos("오류 $errorType")
    }

    // 안드로이드 TTS 진행 상황
    private val androidEvents = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = setAndroid("말하는 중")
        override fun onDone(utteranceId: String?) = setAndroid("끝까지 말함")

        @Deprecated("안드로이드에서 폐지 예정이지만 반드시 구현해야 하는 함수")
        override fun onError(utteranceId: String?) = setAndroid("오류")
    }

    init {
        card.addButtons("Pleos TTS로 말하기" to ::speakWithPleos, "안드로이드 TTS로 말하기" to ::speakWithAndroid)
    }

    override fun start() {
        // Pleos TTS: 만들기 → 진행 상황 듣기 → 초기화 → 한국어로 설정
        val tts = PleosTts(context, Mode.ON_DEVICE)
        pleosTts = tts
        tts.addEventListener(pleosEvents)
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                tts.setLocale(Locale.KOREA)
                setPleos("초기화 성공")
            }

            override fun onFailure(errorType: ErrorType) = setPleos("초기화 실패 $errorType")
        })

        // 안드로이드 기본 TTS: 만들기 → 준비되면 한국어로 설정
        androidTts = AndroidTts(context) { status ->
            if (status == AndroidTts.SUCCESS) {
                val result = androidTts?.setLanguage(Locale.KOREAN)
                val noKorean = result == AndroidTts.LANG_MISSING_DATA || result == AndroidTts.LANG_NOT_SUPPORTED
                setAndroid(if (noKorean) "한국어 음성 없음 (결과 $result)" else "준비됨")
            } else {
                setAndroid("초기화 실패 (상태 $status)")
            }
        }
        androidTts?.setOnUtteranceProgressListener(androidEvents)
        refresh()
    }

    override fun stop() {
        runCatching {
            pleosTts?.removeEventListener(pleosEvents)
            pleosTts?.release()
        }
        runCatching { androidTts?.shutdown() }
    }

    private fun speakWithPleos() {
        val tts = pleosTts ?: return setPleos("아직 만들어지지 않음")
        setPleos("말하기 요청")
        tts.speak(sentence, true)
    }

    private fun speakWithAndroid() {
        val tts = androidTts ?: return setAndroid("아직 만들어지지 않음")
        setAndroid("말하기 요청")
        tts.speak(sentence, AndroidTts.QUEUE_FLUSH, null, "gilteojwo-test")
    }

    private fun setPleos(state: String) {
        pleosState = "$state (${timeText()})"
        refresh()
    }

    private fun setAndroid(state: String) {
        androidState = "$state (${timeText()})"
        refresh()
    }

    private fun refresh() = card.show("문장: \"$sentence\"\nPleos TTS: $pleosState\n안드로이드 TTS: $androidState")
}
