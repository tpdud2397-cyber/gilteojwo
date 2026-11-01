package com.gilteojwo.screen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.gilteojwo.approach.ApproachInfo
import kotlin.math.min

/**
 * 레이더 (사양서 '접근 알림' 화면): 가운데가 내 차(위쪽이 내 진행 방향), 동그라미는 200m·500m·1,000m.
 * 긴급차량은 색 점으로 찍는다. 점의 위치는 RadarMath가 계산한다.
 */
class RadarView(context: Context) : View(context) {
    private var target: ApproachInfo? = null
    private var dotColor = Color.RED

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(140, 150, 150, 150)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 28f
    }
    private val carPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2196F3") }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 알리는 긴급차량과 점 색을 바꾼다 (null이면 점 없음) */
    fun show(info: ApproachInfo?, color: Int) {
        target = info
        dotColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - 40f

        // 거리 동그라미
        for (meters in listOf(200, 500, 1000)) {
            val r = radius * meters / RadarMath.RANGE_METERS.toFloat()
            canvas.drawCircle(centerX, centerY, r, ringPaint)
            canvas.drawText("${meters}m", centerX + 10f, centerY - r + 32f, labelPaint)
        }

        // 내 차: 위쪽(진행 방향)을 가리키는 파란 삼각형
        val car = Path()
        car.moveTo(centerX, centerY - 30f)
        car.lineTo(centerX - 20f, centerY + 22f)
        car.lineTo(centerX + 20f, centerY + 22f)
        car.close()
        canvas.drawPath(car, carPaint)

        // 긴급차량 점
        val info = target ?: return
        val (dx, dy) = RadarMath.offset(info.relativeAngleDegrees, info.distanceMeters, radius)
        dotPaint.color = dotColor
        canvas.drawCircle(centerX + dx, centerY + dy, 26f, dotPaint)
    }
}
