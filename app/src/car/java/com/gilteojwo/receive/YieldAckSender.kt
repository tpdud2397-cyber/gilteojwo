package com.gilteojwo.receive

import android.util.Log
import com.gilteojwo.alert.YieldStatus
import com.gilteojwo.geo.GeoPoint
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.math.roundToLong

/**
 * 양보 확인 보내기 (사양서 R-16): 서버의 yield_acks 표에 '알림 받음'·'양보함'을 한 줄 넣는다.
 * 차량 ID는 보내지 않는다(익명). 위치도 소수 셋째 자리(약 100m)까지만 보내 대략적인 위치만 남긴다.
 * 공개 키로는 이 표에 쓰기만 할 수 있다(읽기 불가, 잠금 규칙). 출동이 끝나면 서버가 지운다.
 * 긴급차량 쪽은 이것을 모아 '알림 N대 · 양보 N대'로 본다 (S-8).
 */
class YieldAckSender {
    private val http = OkHttpClient()

    fun send(dispatchId: String, status: YieldStatus, myPoint: GeoPoint) {
        val body = JSONObject()
            .put("dispatch_id", dispatchId)
            .put("status", status.code)
            .put("approx_lat", approximate(myPoint.latitude))
            .put("approx_lon", approximate(myPoint.longitude))
        val request = Request.Builder()
            .url("${ServerConfig.URL}/rest/v1/yield_acks")
            .header("apikey", ServerConfig.PUBLISHABLE_KEY)
            .header("Authorization", "Bearer ${ServerConfig.PUBLISHABLE_KEY}")
            .header("Prefer", "return=minimal") // 넣은 줄을 돌려받지 않는다 (읽기 권한이 없으므로)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.i(LOG_TAG, "양보 확인 보냄: ${status.code} (서버 응답 ${response.code})")
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.w(LOG_TAG, "양보 확인 보내기 실패: ${status.code} (${e.message})")
            }
        })
    }

    /** 대략 위치: 소수 셋째 자리까지 (위도 0.001도 ≈ 111m) */
    private fun approximate(degrees: Double): Double = (degrees * 1000).roundToLong() / 1000.0

    companion object {
        private const val LOG_TAG = "Gilteojwo"
    }
}
