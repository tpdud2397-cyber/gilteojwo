package com.gilteojwo.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/**
 * 음악 볼륨 낮춤 (사양서 R-13): 경고·양보 동안 음악 소리를 줄이고, 끝나면 되돌린다.
 * 안드로이드의 '오디오 포커스'를 쓴다. "잠깐 안내를 할 테니 다른 소리는 작게 해 달라"(MAY_DUCK)고 요청하면
 * 음악 앱 소리가 작아지고, 요청을 거두면 원래 크기로 돌아온다.
 */
class MusicDucker(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .build()
    private var lowering = false

    /** true면 음악을 줄이고, false면 되돌린다. 같은 값이 이어지면 아무것도 안 한다 */
    fun lower(on: Boolean) {
        if (on == lowering) return
        lowering = on
        if (on) {
            val result = audioManager.requestAudioFocus(request)
            Log.i(LOG_TAG, "음악 소리 줄임 요청 (결과 ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "허락" else result})")
        } else {
            audioManager.abandonAudioFocusRequest(request)
            Log.i(LOG_TAG, "음악 소리 되돌림")
        }
    }

    companion object {
        private const val LOG_TAG = "Gilteojwo"
    }
}
