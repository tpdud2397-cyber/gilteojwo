package com.gilteojwo.sdktest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.sin

/**
 * ⑤ 좌우 알림음 시험
 * 알림음 파일 없이 "삐" 소리를 코드로 만들어, 왼쪽·오른쪽 스피커 중 한쪽에서만 낸다.
 * 스테레오 소리는 [왼쪽, 오른쪽, 왼쪽, 오른쪽 ...] 순서로 숫자를 담는다.
 * 한쪽을 0으로 채우면 그쪽에서는 소리가 나지 않는다.
 */
class StereoBeepTest(private val card: TestCard) : SdkTest {
    private val sampleRate = 44100 // 1초에 담는 소리 숫자 개수
    private val toneHz = 880.0 // 소리 높이 (라 음)
    private val seconds = 0.6 // 소리 길이
    private val silence: Short = 0
    private val mainThread = Handler(Looper.getMainLooper())

    init {
        card.addButtons(
            "왼쪽에서 삐" to { play(left = true, right = false, label = "왼쪽") },
            "오른쪽에서 삐" to { play(left = false, right = true, label = "오른쪽") },
            "양쪽에서 삐" to { play(left = true, right = true, label = "양쪽") },
        )
    }

    override fun start() = card.show("PC에 이어폰을 끼고 버튼을 눌러, 한쪽에서만 들리는지 확인하세요")

    override fun stop() {}

    private fun play(left: Boolean, right: Boolean, label: String) {
        // 1) 소리 숫자 만들기
        val frameCount = (sampleRate * seconds).toInt()
        val pcm = ShortArray(frameCount * 2)
        for (i in 0 until frameCount) {
            val sample = (sin(2 * PI * toneHz * i / sampleRate) * Short.MAX_VALUE * 0.6).toInt().toShort()
            pcm[i * 2] = if (left) sample else silence // 왼쪽 칸
            pcm[i * 2 + 1] = if (right) sample else silence // 오른쪽 칸
        }

        // 2) 재생기 만들기: 내비 안내와 같은 종류의 소리로 낸다 (음악은 작아지고 안내는 같이 들림)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()

        // 3) 재생하고, 끝나면 정리
        track.write(pcm, 0, pcm.size)
        track.play()
        card.show("$label 소리를 냈습니다 (${timeText()})")
        mainThread.postDelayed({ track.release() }, 1500)
    }
}
