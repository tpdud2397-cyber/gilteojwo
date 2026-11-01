package com.gilteojwo.receive

import android.os.Handler
import android.os.Looper
import com.gilteojwo.geo.GeoPoint
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime

/**
 * R-2 긴급차량 수신: 서버(Supabase)의 '긴급차량 현재 위치' 표를 실시간으로 받는다.
 * 1) 켤 때 지금 출동 중인 차 목록을 한 번 받아 온다 (positions_active: 최근 10초 안에 갱신된 차만)
 * 2) 웹소켓(계속 열려 있는 연결)을 열어 두면, 표가 바뀔 때마다 서버가 변화를 보내 준다 (Supabase Realtime)
 * 3) 연결이 끊기면 3초 뒤 다시 연결한다
 * 4) 10초 넘게 새 소식이 없는 차는 목록에서 뺀다 (사양서: 10초 만료)
 * 결과(onChanged)는 메인 스레드에서 부른다. 목록도 메인 스레드에서만 고친다.
 */
class EmergencyVehicleReceiver(
    private val onChanged: (vehicles: List<EmergencyVehicle>, status: String) -> Unit,
) {
    private val http = OkHttpClient()
    private val mainThread = Handler(Looper.getMainLooper())
    private val vehicles = mutableMapOf<String, EmergencyVehicle>() // 차량 ID → 마지막으로 받은 상태
    private var socket: WebSocket? = null
    private var status = "연결 준비"
    private var running = false
    private var reconnectScheduled = false
    private var seconds = 0
    private var ref = 0 // 서버에 보내는 메시지 번호

    fun start() {
        running = true
        connect()
        mainThread.post(everySecond)
    }

    fun stop() {
        running = false
        mainThread.removeCallbacksAndMessages(null)
        socket?.close(1000, "앱 종료")
    }

    // 1초마다: 오래된 차를 빼고, 25초마다 서버에 '연결 살아 있음' 신호를 보낸다
    private val everySecond = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            vehicles.values.removeAll { now - it.updatedMillis > EXPIRE_MILLIS }
            seconds++
            if (seconds % HEARTBEAT_SECONDS == 0) sendHeartbeat()
            notifyChanged()
            mainThread.postDelayed(this, 1000)
        }
    }

    private fun connect() {
        reconnectScheduled = false
        status = "연결 중"
        loadActiveVehicles()
        val url = "${ServerConfig.URL.replace("https://", "wss://")}/realtime/v1/websocket" +
            "?apikey=${ServerConfig.PUBLISHABLE_KEY}&vsn=1.0.0"
        socket = http.newWebSocket(Request.Builder().url(url).build(), socketListener)
    }

    /** 1) 지금 출동 중인 차 목록을 한 번 받아 온다 */
    private fun loadActiveVehicles() {
        val request = Request.Builder()
            .url("${ServerConfig.URL}/rest/v1/positions_active?select=*")
            .header("apikey", ServerConfig.PUBLISHABLE_KEY)
            .header("Authorization", "Bearer ${ServerConfig.PUBLISHABLE_KEY}")
            .build()
        http.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                mainThread.post {
                    if (!response.isSuccessful) {
                        status = "목록 받기 실패 (${response.code}: ${body.take(80)})"
                        return@post
                    }
                    val rows = JSONArray(body)
                    for (i in 0 until rows.length()) put(rows.getJSONObject(i))
                    notifyChanged()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                mainThread.post { status = "목록 받기 실패 (${e.message})" }
            }
        })
    }

    /** 2) 웹소켓으로 오는 메시지 처리 */
    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // positions_live 표의 모든 변화(추가·수정·삭제)를 보내 달라고 등록한다
            val change = JSONObject().put("event", "*").put("schema", "public").put("table", "positions_live")
            val config = JSONObject().put("postgres_changes", JSONArray().put(change))
            send("realtime:gilteojwo", "phx_join", JSONObject().put("config", config))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            mainThread.post { handleMessage(JSONObject(text)) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            mainThread.post { reconnectLater("끊김 (${t.message})") }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            mainThread.post { reconnectLater("닫힘 ($reason)") }
        }
    }

    private fun handleMessage(message: JSONObject) {
        val payload = message.optJSONObject("payload") ?: return
        when (message.optString("event")) {
            // 표가 바뀌었다: 추가·수정이면 목록에 넣고, 삭제(긴급출동 종료)면 뺀다
            "postgres_changes" -> {
                val data = payload.getJSONObject("data")
                when (data.getString("type")) {
                    "INSERT", "UPDATE" -> put(data.getJSONObject("record"))
                    "DELETE" -> vehicles.remove(data.getJSONObject("old_record").optString("vehicle_id"))
                }
            }
            // 등록 결과. 실패하면 3초 뒤 다시 연결해 본다
            "system" -> if (payload.optString("status") == "ok") {
                status = "연결됨 (실시간)"
            } else {
                reconnectLater("실시간 등록 실패 (${payload.optString("message")})")
            }
            "phx_reply" -> if (payload.optString("status") != "ok") status = "연결 거부 (${payload.optJSONObject("response")})"
            "phx_error", "phx_close" -> reconnectLater("서버가 연결을 닫음")
        }
        notifyChanged()
    }

    /** 3) 3초 뒤 다시 연결 */
    private fun reconnectLater(reason: String) {
        if (!running || reconnectScheduled) return
        reconnectScheduled = true
        status = "$reason. 3초 뒤 다시 연결"
        notifyChanged()
        socket?.cancel()
        mainThread.postDelayed({ if (running) connect() }, 3000)
    }

    /** 서버 표 한 줄을 EmergencyVehicle로 바꿔 목록에 넣는다. 받은 시각과 서버에 기록된 시각을 함께 남긴다 */
    private fun put(row: JSONObject) {
        val type = VehicleType.fromCode(row.optString("vehicle_type")) ?: return
        val now = System.currentTimeMillis()
        val vehicle = EmergencyVehicle(
            id = row.getString("vehicle_id"),
            type = type,
            point = GeoPoint(row.getDouble("latitude"), row.getDouble("longitude")),
            headingDegrees = row.optDouble("heading", 0.0),
            speedKmh = row.optDouble("speed_kmh", 0.0),
            updatedMillis = now,
            recordedMillis = serverTimeMillis(row.optString("updated_at")) ?: now,
            dispatchId = row.optString("dispatch_id").ifEmpty { null },
        )
        vehicles[vehicle.id] = vehicle
    }

    /** 서버 시각 글자(예: 2026-11-01T04:12:01.131961+00:00)를 밀리초 숫자로. 읽을 수 없으면 null */
    private fun serverTimeMillis(text: String): Long? =
        runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()

    private fun sendHeartbeat() = send("phoenix", "heartbeat", JSONObject())

    private fun send(topic: String, event: String, payload: JSONObject) {
        val message = JSONObject().put("topic", topic).put("event", event).put("payload", payload).put("ref", "${++ref}")
        socket?.send(message.toString())
    }

    private fun notifyChanged() = onChanged(vehicles.values.toList(), status)

    companion object {
        const val EXPIRE_MILLIS = 10_000L // 10초 넘게 소식이 없으면 목록에서 뺀다
        const val HEARTBEAT_SECONDS = 25 // 서버가 연결을 끊지 않게 25초마다 신호
    }
}
