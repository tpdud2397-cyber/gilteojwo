package com.gilteojwo.send

import android.os.Handler
import android.os.Looper
import com.gilteojwo.receive.ServerConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** 등록 코드로 확인한 긴급차량 (사양서 S-1) */
data class SenderVehicle(val id: String, val typeCode: String, val agency: String?)

/**
 * 송신용 서버 기능 부르기 (Supabase). 모두 등록 코드가 맞을 때만 동작하는 서버 기능이다.
 * - check_vehicle: 등록 코드로 차량 확인 (S-1)
 * - start_dispatch / end_dispatch: 긴급출동 시작·끝 (S-2)
 * - send_position: 현재 위치 보내기 (S-3)
 * - dispatch_status: 알림 받은 차·양보한 차 수 (S-8)
 * 결과는 항상 메인 스레드에서 알려준다. 폰 앱과 차량 앱의 Pleos 송신 모드가 함께 쓴다.
 */
class SenderServer {
    private val http = OkHttpClient()
    private val mainThread = Handler(Looper.getMainLooper())

    /** 등록 코드가 맞으면 차량 정보, 등록되지 않은 코드면 null */
    fun checkVehicle(regCode: String, onDone: (SenderVehicle?) -> Unit, onError: (String) -> Unit) {
        call("check_vehicle", JSONObject().put("p_reg_code", regCode), onError) { body ->
            val rows = JSONArray(body)
            if (rows.length() == 0) {
                onDone(null)
            } else {
                val row = rows.getJSONObject(0)
                onDone(SenderVehicle(row.getString("vehicle_id"), row.getString("vehicle_type"), row.optString("agency").ifEmpty { null }))
            }
        }
    }

    /** 긴급출동을 시작하고 출동 번호를 받는다 */
    fun startDispatch(regCode: String, onDone: (String) -> Unit, onError: (String) -> Unit) {
        call("start_dispatch", JSONObject().put("p_reg_code", regCode), onError) { body ->
            onDone(body.trim().trim('"')) // 서버는 출동 번호를 "따옴표 글자"로 돌려준다
        }
    }

    fun sendPosition(regCode: String, dispatchId: String, fix: SenderFix, onDone: () -> Unit, onError: (String) -> Unit) {
        val args = JSONObject()
            .put("p_reg_code", regCode)
            .put("p_dispatch_id", dispatchId)
            .put("p_latitude", fix.latitude)
            .put("p_longitude", fix.longitude)
            .put("p_heading", fix.headingDegrees)
            .put("p_speed_kmh", fix.speedKmh)
            .put("p_accuracy_m", fix.accuracyMeters)
        call("send_position", args, onError) { onDone() }
    }

    /** endType: "manual"(직접 끔) 또는 "auto"(출동 확인에 답이 없어 자동 종료, S-7) */
    fun endDispatch(regCode: String, dispatchId: String, endType: String, onDone: () -> Unit, onError: (String) -> Unit) {
        val args = JSONObject().put("p_reg_code", regCode).put("p_dispatch_id", dispatchId).put("p_end_type", endType)
        call("end_dispatch", args, onError) { onDone() }
    }

    /** 이 출동에서 알림 받은 차 수와 양보한 차 수 */
    fun dispatchStatus(regCode: String, dispatchId: String, onDone: (notified: Int, yielded: Int) -> Unit, onError: (String) -> Unit) {
        val args = JSONObject().put("p_reg_code", regCode).put("p_dispatch_id", dispatchId)
        call("dispatch_status", args, onError) { body ->
            val row = JSONArray(body).optJSONObject(0)
            onDone(row?.optInt("notified_count") ?: 0, row?.optInt("yielded_count") ?: 0)
        }
    }

    /** 서버 기능 하나를 부른다. 성공하면 받은 글자를 onSuccess로, 실패하면 이유를 onError로 (메인 스레드에서) */
    private fun call(function: String, args: JSONObject, onError: (String) -> Unit, onSuccess: (String) -> Unit) {
        val request = Request.Builder()
            .url("${ServerConfig.URL}/rest/v1/rpc/$function")
            .header("apikey", ServerConfig.PUBLISHABLE_KEY)
            .header("Authorization", "Bearer ${ServerConfig.PUBLISHABLE_KEY}")
            .post(args.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                val ok = response.isSuccessful
                response.close()
                mainThread.post {
                    if (ok) {
                        runCatching { onSuccess(body) }.onFailure { onError("응답을 읽지 못함 (${it.message})") }
                    } else {
                        // 서버가 보낸 이유 (예: "등록되지 않은 코드입니다")
                        val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                        onError(message?.ifEmpty { null } ?: "서버 오류 (${response.code})")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                mainThread.post { onError("인터넷 연결 실패 (${e.message})") }
            }
        })
    }
}

/** 보낼 위치 한 번 (사양서 S-3: 위도, 경도, 진행 방향, 속도, 정확도) */
data class SenderFix(
    val latitude: Double,
    val longitude: Double,
    val headingDegrees: Double,
    val speedKmh: Double,
    val accuracyMeters: Double,
)
