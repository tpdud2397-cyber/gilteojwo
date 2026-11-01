package com.gilteojwo.alert

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.sin

/**
 * 알림음 (사양서 R-12): 소리 파일 없이 코드로 "삐삐" 소리를 만들어, 긴급차량이 오는 쪽 스피커에서만 낸다.
 * 스테레오 소리는 [왼쪽, 오른쪽, 왼쪽, 오른쪽 ...] 순서로 숫자를 담는다. 한쪽을 0으로 채우면 그쪽은 소리가 안 난다.
 * 시험 화면 ⑤(StereoBeepTest)에서 확인한 방법이다.
 * - LEFT·RIGHT·BOTH: 높은 "삐삐" 두 번
 * - DONE(해제): "띵-동" 낮은 음에서 높은 음
 */
class SoundPlayer {
    private val mainThread = Handler(Looper.getMainLooper())

    fun play(cue: SoundCue) {
        val left = cue != SoundCue.RIGHT
        val right = cue != SoundCue.LEFT
        val pcm = if (cue == SoundCue.DONE) {
            tone(660.0, 0.15, left, right) + silence(0.05) + tone(990.0, 0.25, left, right)
        } else {
            tone(880.0, 0.18, left, right) + silence(0.08) + tone(880.0, 0.18, left, right)
        }
        playPcm(pcm)
    }

    /** hz 높이의 소리를 seconds초 동안. left/right가 false인 쪽은 0(무음)으로 채운다 */
    private fun tone(hz: Double, seconds: Double, left: Boolean, right: Boolean): ShortArray {
        val frames = (SAMPLE_RATE * seconds).toInt()
        val pcm = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val sample = (sin(2 * PI * hz * i / SAMPLE_RATE) * Short.MAX_VALUE * VOLUME).toInt().toShort()
            pcm[i * 2] = if (left) sample else 0 // 왼쪽 칸
            pcm[i * 2 + 1] = if (right) sample else 0 // 오른쪽 칸
        }
        return pcm
    }

    private fun silence(seconds: Double) = ShortArray((SAMPLE_RATE * seconds).toInt() * 2)

    private fun playPcm(pcm: ShortArray) {
        // 내비 안내와 같은 종류의 소리로 낸다 (음악보다 앞에 들린다)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        mainThread.postDelayed({ runCatching { track.release() } }, 2000)
    }

    companion object {
        private const val SAMPLE_RATE = 44100 // 1초에 담는 소리 숫자 개수
        private const val VOLUME = 0.6
    }
}
