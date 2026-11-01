package com.gilteojwo.alert

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 내비 칸 가장자리 빛 (사양서 R-10·R-11, 11/01 사용자 그림).
 * '다른 앱 위에 표시' 권한으로 내비 칸 가장자리에 붉은 빛(그라데이션)을 덮어 그린다. 터치는 그대로 아래(내비)로 통과한다.
 * 어느 면을 얼마나 진하게 그릴지는 EdgeGlow가 정하고, 여기서는 그리기만 한다.
 * 왼쪽 주행 화면(속도·경고등)은 가리지 않는다.
 */
class EdgeGlowOverlay(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val mainThread = Handler(Looper.getMainLooper())
    private var view: GlowView? = null
    private var blinkOn = true

    // 양보 단계: 0.5초마다 밝게·어둡게 바꾼다
    private val blink = object : Runnable {
        override fun run() {
            blinkOn = !blinkOn
            view?.alpha = if (blinkOn) 1f else BLINK_DIM_ALPHA
            mainThread.postDelayed(this, BLINK_MILLIS)
        }
    }

    fun show(glow: EdgeGlow) {
        if (glow.edges.isEmpty()) return stop()
        if (!Settings.canDrawOverlays(appContext)) return // 권한이 없으면 상단 카드만 쓴다
        val glowView = view ?: addView() ?: return
        glowView.glow = glow
        glowView.invalidate()

        mainThread.removeCallbacks(blink)
        if (glow.blinking) {
            mainThread.postDelayed(blink, BLINK_MILLIS)
        } else {
            glowView.alpha = 1f
        }
    }

    fun stop() {
        mainThread.removeCallbacks(blink)
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
    }

    /** 내비 칸 크기의 투명한 덮개를 만든다. 실패하면 null */
    private fun addView(): GlowView? {
        // 화면 크기에서 내비 칸 위치를 계산한다. 에뮬레이터 2560×1440에서
        // 왼쪽 주행 화면이 x 0~853, 내비 칸이 x 853~2535, y 90~1300이었다 (11/01 측정)
        val (screenWidth, screenHeight) = screenSize()
        val left = screenWidth * 853 / 2560
        val right = screenWidth * 2535 / 2560
        val top = screenHeight * 90 / 1440
        val bottom = screenHeight * 1300 / 1440

        val params = WindowManager.LayoutParams(
            right - left,
            bottom - top,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = left
        params.y = top

        val glowView = GlowView(appContext)
        return try {
            windowManager.addView(glowView, params)
            view = glowView
            glowView
        } catch (e: Exception) {
            null
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val metrics = appContext.resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    /**
     * 켜진 면마다 가장자리 가운데에서 안쪽으로 퍼지는 반타원 모양 빛을 그린다 (11/01 사용자 그림).
     * 가운데가 가장 진하고 바깥으로 갈수록 투명해진다(그라데이션).
     * 가까울수록(strength가 클수록) 안쪽으로 깊게 커지고 진하다. 멀면 얇고 연하다.
     */
    private class GlowView(context: Context) : View(context) {
        var glow = EdgeGlow.OFF
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stretch = Matrix()

        override fun onDraw(canvas: Canvas) {
            val strength = glow.strength
            // 안쪽으로 들어오는 깊이: 멀면 5%, 가까우면 20% (내비 칸의 20%까지만 쓴다, 11/01 사용자 요청)
            val depthFraction = MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * strength
            val alpha = (255 * (MIN_ALPHA + (1 - MIN_ALPHA) * strength)).toInt()
            for (edge in glow.edges) drawGlow(canvas, edge, depthFraction, alpha)
        }

        private fun drawGlow(canvas: Canvas, edge: ScreenEdge, depthFraction: Float, alpha: Int) {
            val w = width.toFloat()
            val h = height.toFloat()
            // 위·아래 빛은 세로의, 왼쪽·오른쪽 빛은 가로의 depthFraction만큼 안쪽으로 들어온다
            val depth = if (edge == ScreenEdge.TOP || edge == ScreenEdge.BOTTOM) h * depthFraction else w * depthFraction
            // 빛의 중심(가장자리 가운데)과, 가장자리를 따라 퍼지는 길이(반지름)
            val centerX = when (edge) {
                ScreenEdge.LEFT -> 0f
                ScreenEdge.RIGHT -> w
                else -> w / 2
            }
            val centerY = when (edge) {
                ScreenEdge.TOP -> 0f
                ScreenEdge.BOTTOM -> h
                else -> h / 2
            }
            val alongEdge = if (edge == ScreenEdge.TOP || edge == ScreenEdge.BOTTOM) w / 2 else h / 2

            // 가운데 진하게 → 중간 → 바깥 투명. 원 모양 빛을 만든 뒤 가장자리를 따라 길게 늘여 반타원으로 만든다
            val gradient = RadialGradient(
                centerX, centerY, depth,
                intArrayOf(red(alpha), red(alpha * 6 / 10), red(0)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            stretch.reset()
            if (edge == ScreenEdge.TOP || edge == ScreenEdge.BOTTOM) {
                stretch.setScale(alongEdge / depth, 1f, centerX, centerY)
            } else {
                stretch.setScale(1f, alongEdge / depth, centerX, centerY)
            }
            gradient.setLocalMatrix(stretch)
            paint.shader = gradient
            canvas.drawRect(0f, 0f, w, h, paint)
        }

        private fun red(alpha: Int) = Color.argb(alpha, 255, 59, 48)
    }

    companion object {
        private const val MIN_DEPTH = 0.05f // 가장 멀 때: 내비 칸의 5%만큼 안쪽으로 (얇게)
        private const val MAX_DEPTH = 0.20f // 가장 가까울 때: 내비 칸의 20%까지만 (11/01 사용자 요청)
        private const val MIN_ALPHA = 0.35f // 가장 멀 때 가운데 진하기 (연하게)
        private const val BLINK_MILLIS = 500L
        private const val BLINK_DIM_ALPHA = 0.45f // 깜빡일 때 어두운 순간에도 빛이 남게 (꺼지지 않고 숨 쉬듯)
    }
}
