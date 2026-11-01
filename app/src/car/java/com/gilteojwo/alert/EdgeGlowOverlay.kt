package com.gilteojwo.alert

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 내비 칸 가장자리 빛 (사양서 R-10·R-11, 11/01 사용자 그림).
 * '다른 앱 위에 표시' 권한으로 내비 칸 테두리에 얇은 붉은 띠를 덮어 그린다. 터치는 그대로 아래(내비)로 통과한다.
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

    /** 네 가장자리 중 켜진 면에 붉은 띠를 그린다. 진할수록 두껍고 불투명하다 */
    private class GlowView(context: Context) : View(context) {
        var glow = EdgeGlow.OFF
        private val paint = Paint()

        override fun onDraw(canvas: Canvas) {
            val strength = glow.strength
            val thickness = THIN_PX + (THICK_PX - THIN_PX) * strength
            // 바깥쪽에 넓고 옅은 띠, 안쪽에 좁고 진한 띠를 겹쳐 빛이 번지는 것처럼 보이게 한다
            drawEdges(canvas, thickness * 2.5f, (90 * strength).toInt())
            drawEdges(canvas, thickness, (255 * strength).toInt())
        }

        private fun drawEdges(canvas: Canvas, thickness: Float, alpha: Int) {
            paint.color = Color.argb(alpha, 244, 67, 54)
            val w = width.toFloat()
            val h = height.toFloat()
            for (edge in glow.edges) {
                when (edge) {
                    ScreenEdge.TOP -> canvas.drawRect(0f, 0f, w, thickness, paint)
                    ScreenEdge.BOTTOM -> canvas.drawRect(0f, h - thickness, w, h, paint)
                    ScreenEdge.LEFT -> canvas.drawRect(0f, 0f, thickness, h, paint)
                    ScreenEdge.RIGHT -> canvas.drawRect(w - thickness, 0f, w, h, paint)
                }
            }
        }
    }

    companion object {
        private const val THIN_PX = 12f // 가장 멀 때 두께 (2560×1440 화면 기준)
        private const val THICK_PX = 36f // 가장 가까울 때 두께
        private const val BLINK_MILLIS = 500L
        private const val BLINK_DIM_ALPHA = 0.2f
    }
}
