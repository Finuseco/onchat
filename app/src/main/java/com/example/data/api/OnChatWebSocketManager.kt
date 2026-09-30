package com.example.data.api

import android.util.Log
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class WsConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class OnChatWebSocketManager private constructor() {

    companion object {
        private const val TAG = "OnChatWebSocket"
        const val WS_URL_GLOB_PRIMARY = "wss://glob.cashpay-all.com/ws"
        const val WS_URL_GLOB_REALTIME = "wss://glob.cashpay-all.com/ws/realtime"
        const val WS_URL_GLOB_ROOT = "wss://glob.cashpay-all.com"

        @Volatile
        private var instance: OnChatWebSocketManager? = null

        fun getInstance(): OnChatWebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: OnChatWebSocketManager().also { instance = it }
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val envelopeAdapter = moshi.adapter(WsEventEnvelope::class.java)
    private val typingAdapter = moshi.adapter(WsOutgoingTyping::class.java)
    private val readAdapter = moshi.adapter(WsOutgoingRead::class.java)
    private val callSignalingAdapter = moshi.adapter(WsOutgoingCallSignaling::class.java)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep open indefinitely
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private var shouldReconnect = true
    private var reconnectAttempt = 0
    private var currentUrlIndex = 0
    private var heartbeatJob: Job? = null

    // State flows
    private val _connectionStatus = MutableStateFlow(WsConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<WsConnectionStatus> = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Pair<String, ApiMessage>>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Pair<String, ApiMessage>> = _incomingMessages.asSharedFlow()

    private val _incomingCalls = MutableSharedFlow<IncomingCallNotificationState>(extraBufferCapacity = 16)
    val incomingCalls: SharedFlow<IncomingCallNotificationState> = _incomingCalls.asSharedFlow()

    private val _callSignalingEvents = MutableSharedFlow<WsEventEnvelope>(extraBufferCapacity = 32)
    val callSignalingEvents: SharedFlow<WsEventEnvelope> = _callSignalingEvents.asSharedFlow()

    // Map of conversationId -> Typing user name
    private val _typingStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val typingStatus: StateFlow<Map<String, String>> = _typingStatus.asStateFlow()

    // Callback when remote audio chunk is received
    var onRemoteAudioReceived: ((ByteArray) -> Unit)? = null

    // Set of online user/wallet IDs
    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    val onlineUsers: StateFlow<Set<String>> = _onlineUsers.asStateFlow()

    // Typing clear jobs
    private val typingJobs = mutableMapOf<String, Job>()

    var onReconnected: (() -> Unit)? = null

    /**
     * Connect to the central CashPay Realtime Engine (glob.cashpay-all.com)
     */
    fun connect() {
        if (_connectionStatus.value == WsConnectionStatus.CONNECTED || isConnecting.get()) {
            return
        }

        val token = ApiClient.authToken ?: SessionTokenStore.getToken()
        val walletId = ApiClient.activeWalletId.ifBlank { SessionTokenStore.getWalletId() }
        val effectiveToken = if (!token.isNullOrBlank()) token else walletId

        isConnecting.set(true)
        _connectionStatus.value = WsConnectionStatus.CONNECTING
        shouldReconnect = true

        val urls = listOf(WS_URL_GLOB_PRIMARY, WS_URL_GLOB_REALTIME, WS_URL_GLOB_ROOT)
        val targetBaseUrl = urls[currentUrlIndex % urls.size]
        val sep = if (targetBaseUrl.contains("?")) "&" else "?"
        val targetUrl = "$targetBaseUrl${sep}walletId=$walletId&userId=${ApiClient.currentUserId ?: 1}${if (!token.isNullOrBlank()) "&token=$token" else ""}"

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "OnChat-Mobile/1.0 (Android; CashPay)")
            .header("Cookie", "cashpay-token=$effectiveToken")
            .header("Authorization", "Bearer $effectiveToken")
            .header("x-wallet-id", walletId)

        val request = requestBuilder.build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to $targetUrl")
                isConnecting.set(false)
                val wasReconnecting = reconnectAttempt > 0
                reconnectAttempt = 0
                _connectionStatus.value = WsConnectionStatus.CONNECTED
                
                // Register identity and subscribe to real-time events
                try {
                    val regJson = """{"event":"register","type":"register","token":"$effectiveToken","walletId":"$walletId","userId":${ApiClient.currentUserId ?: 1}}"""
                    ws.send(regJson)
                    ws.send("""{"type":"authenticate","token":"$effectiveToken","walletId":"$walletId","userId":${ApiClient.currentUserId ?: 1}}""")
                    if (walletId.isNotBlank()) {
                        ws.send("""{"event":"subscribe","channel":"user_$walletId"}""")
                        ws.send("""{"event":"join","room":"$walletId"}""")
                    }
                } catch (_: Exception) {}

                startHeartbeat()
                if (wasReconnecting) {
                    onReconnected?.invoke()
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                isConnecting.set(false)
                _connectionStatus.value = WsConnectionStatus.DISCONNECTED
                stopHeartbeat()
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure on $targetUrl: ${t.message}")
                isConnecting.set(false)
                _connectionStatus.value = WsConnectionStatus.DISCONNECTED
                stopHeartbeat()
                currentUrlIndex++
                scheduleReconnect()
            }
        })
    }

    /**
     * Send periodic heartbeat ping
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = coroutineScope.launch {
            while (isActive && _connectionStatus.value == WsConnectionStatus.CONNECTED) {
                delay(15000)
                try {
                    webSocket?.send("""{"event":"ping","type":"ping","timestamp":"${System.currentTimeMillis()}"}""")
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat failed: ${e.message}")
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Automatic reconnection with exponential backoff
     */
    private fun scheduleReconnect() {
        if (!shouldReconnect) return

        reconnectAttempt++
        val delayMillis = when (reconnectAttempt) {
            1 -> 1000L
            2 -> 2500L
            3 -> 5000L
            4 -> 10000L
            else -> 15000L
        }

        coroutineScope.launch {
            delay(delayMillis)
            if (shouldReconnect && _connectionStatus.value != WsConnectionStatus.CONNECTED) {
                Log.d(TAG, "Attempting reconnect (#$reconnectAttempt)...")
                connect()
            }
        }
    }

    /**
     * Parse and dispatch real-time events received from WebSocket
     */
    private fun handleIncomingMessage(text: String) {
        try {
            val envelope = try {
                envelopeAdapter.fromJson(text)
            } catch (_: Exception) { null }

            // Extract event type from Moshi envelope or raw JSON
            var eventType = envelope?.resolvedEvent ?: ""
            var dataMap: Map<String, Any?>? = envelope?.data
            var rawJsonObj: org.json.JSONObject? = null

            if (eventType.isBlank()) {
                try {
                    rawJsonObj = org.json.JSONObject(text)
                    eventType = (rawJsonObj.optString("event").ifBlank {
                        rawJsonObj.optString("type").ifBlank {
                            rawJsonObj.optString("action")
                        }
                    }).lowercase()
                } catch (_: Exception) {}
            }

            val innerEvent = (dataMap?.get("event") ?: rawJsonObj?.optJSONObject("data")?.optString("event"))?.toString()?.lowercase()
            val effectiveEvent = if ((eventType == "notification" || eventType == "punch" || eventType == "push") && !innerEvent.isNullOrBlank()) innerEvent else eventType

            when (effectiveEvent) {
                // 1. New Chat Message
                "chat.message.created", "chat.message", "new_message", "message.created", "message", "new-message", "message_received" -> {
                    val convId = envelope?.resolvedConversationId
                        ?: rawJsonObj?.optString("conversationId")
                        ?: rawJsonObj?.optString("conversation_id")
                        ?: rawJsonObj?.optJSONObject("data")?.optString("conversationId")
                    var msg = envelope?.message
                    if (msg == null) {
                        val d = dataMap ?: rawJsonObj?.optJSONObject("data")?.let { jo ->
                            val map = mutableMapOf<String, Any?>()
                            jo.keys().forEach { k -> map[k] = jo.opt(k) }
                            map
                        }
                        val content = d?.get("content")?.toString() ?: rawJsonObj?.optString("content") ?: ""
                        if (content.isNotEmpty()) {
                            val sId = d?.get("senderId")?.toString()?.toLongOrNull() ?: envelope?.resolvedUserId
                            val sName = d?.get("senderName")?.toString() ?: envelope?.userName ?: "Contact"
                            msg = ApiMessage(
                                id = d?.get("id")?.toString() ?: "msg_${System.currentTimeMillis()}",
                                conversationId = convId ?: "conv_1",
                                senderId = sId,
                                senderName = sName,
                                content = content,
                                timestamp = envelope?.timestamp ?: System.currentTimeMillis().toString(),
                                createdAt = envelope?.timestamp ?: System.currentTimeMillis().toString(),
                                status = "delivered",
                                type = d?.get("type")?.toString() ?: "text"
                            )
                        }
                    }
                    if (convId != null && msg != null) {
                        coroutineScope.launch {
                            _incomingMessages.emit(Pair(convId, msg))
                        }
                    }
                }

                // 2. Typing indicator
                "chat.typing", "typing" -> {
                    val convId = envelope?.resolvedConversationId ?: rawJsonObj?.optString("conversationId")
                    val name = envelope?.userName ?: rawJsonObj?.optString("userName") ?: "Votre correspondant"
                    if (convId != null) {
                        val current = _typingStatus.value.toMutableMap()
                        current[convId] = name
                        _typingStatus.value = current

                        // Auto-clear typing status after 4 seconds
                        typingJobs[convId]?.cancel()
                        typingJobs[convId] = coroutineScope.launch {
                            delay(4000)
                            val updated = _typingStatus.value.toMutableMap()
                            updated.remove(convId)
                            _typingStatus.value = updated
                        }
                    }
                }

                // 3. Read receipt
                "chat.message.read", "chat.read" -> {
                    if (envelope != null) {
                        coroutineScope.launch {
                            _callSignalingEvents.emit(envelope)
                        }
                    }
                }

                // 4. Online / Offline presence
                "presence.online", "user.online" -> {
                    val uid = envelope?.resolvedWalletId ?: envelope?.resolvedUserId?.toString() ?: rawJsonObj?.optString("walletId")
                    if (uid != null) {
                        _onlineUsers.value = _onlineUsers.value + uid
                    }
                }
                "presence.offline", "user.offline" -> {
                    val uid = envelope?.resolvedWalletId ?: envelope?.resolvedUserId?.toString() ?: rawJsonObj?.optString("walletId")
                    if (uid != null) {
                        _onlineUsers.value = _onlineUsers.value - uid
                    }
                }

                // 5. Incoming Call or Relayed Call Offer (Rings the phone!)
                "call.incoming", "incoming_call", "incoming-call", "call:incoming", "call_incoming", "call.offer", "call:offer", "call_offer", "call", "ring", "call.ring", "call_ringing", "call.start", "call:start", "webrtc.offer", "webrtc:offer" -> {
                    val d = dataMap ?: rawJsonObj?.optJSONObject("data")?.let { jo ->
                        val map = mutableMapOf<String, Any?>()
                        jo.keys().forEach { k -> map[k] = jo.opt(k) }
                        map
                    }
                    val callerWallet = d?.get("callerWalletId")?.toString()
                        ?: d?.get("caller_wallet_id")?.toString()
                        ?: d?.get("senderWalletId")?.toString()
                        ?: envelope?.callerWalletId
                        ?: envelope?.resolvedWalletId
                        ?: rawJsonObj?.optString("callerWalletId")
                        ?: rawJsonObj?.optString("senderWalletId")
                        ?: ""

                    val isIncomingFromPeer = !effectiveEvent.contains("offer") ||
                            (callerWallet.isNotEmpty() && !callerWallet.equals(ApiClient.activeWalletId, ignoreCase = true))

                    if (isIncomingFromPeer) {
                        val callId = envelope?.callId
                            ?: envelope?.resolvedCallId
                            ?: d?.get("callId")?.toString()
                            ?: d?.get("call_id")?.toString()
                            ?: rawJsonObj?.optString("callId")
                            ?: rawJsonObj?.optString("call_id")
                            ?: "call_${System.currentTimeMillis()}"

                        val callerName = d?.get("callerName")?.toString()
                            ?: d?.get("caller_name")?.toString()
                            ?: d?.get("peerName")?.toString()
                            ?: envelope?.callerName
                            ?: rawJsonObj?.optString("callerName")
                            ?: "Appel OnChat"

                        val callType = d?.get("callType")?.toString()
                            ?: d?.get("call_type")?.toString()
                            ?: envelope?.callType
                            ?: rawJsonObj?.optString("callType")
                            ?: "audio"

                        val isVideo = callType.equals("video", ignoreCase = true) || d?.get("isVideo")?.toString() == "true"
                        val callAvatar = envelope?.callerAvatar ?: d?.get("callerAvatar")?.toString()

                        val callState = IncomingCallNotificationState(
                            callId = callId,
                            callerName = callerName,
                            callerWalletId = callerWallet,
                            callerAvatar = callAvatar,
                            isVideo = isVideo,
                            timestamp = System.currentTimeMillis()
                        )
                        coroutineScope.launch {
                            _incomingCalls.emit(callState)
                        }
                    }

                    // Forward to signaling events stream
                    if (envelope != null) {
                        coroutineScope.launch {
                            _callSignalingEvents.emit(envelope)
                        }
                    }
                }

                // 6. WebRTC Call Signaling (answer, candidate, accepted, rejected, ended)
                "call.answer", "call.ice_candidate", "call.accepted", "call.rejected", "call.ended", "conference.invite", "call:answer", "call:rejected", "call:ended", "call:hangup" -> {
                    if (envelope != null) {
                        coroutineScope.launch {
                            _callSignalingEvents.emit(envelope)
                        }
                    }
                }

                // 7. Real-Time Audio voice packets
                "call.audio", "voice_packet" -> {
                    val rawAudio = dataMap?.get("audio") ?: dataMap?.get("data") ?: rawJsonObj?.optJSONObject("data")?.optString("audio")
                    val audioBase64: String? = rawAudio?.toString()
                    if (!audioBase64.isNullOrBlank()) {
                        try {
                            val audioBytes = android.util.Base64.decode(audioBase64, android.util.Base64.NO_WRAP)
                            onRemoteAudioReceived?.invoke(audioBytes)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to decode incoming voice packet", e)
                        }
                    }
                }

                "pong" -> {
                    // Heartbeat response
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WS message: ${e.message} text: $text")
        }
    }

    // --- Outgoing WebSocket Events ---

    fun sendTyping(conversationId: String) {
        if (_connectionStatus.value != WsConnectionStatus.CONNECTED) return
        try {
            val json = typingAdapter.toJson(WsOutgoingTyping(conversationId = conversationId))
            webSocket?.send(json)
        } catch (e: Exception) {
            Log.w(TAG, "Error sending typing: ${e.message}")
        }
    }

    fun sendReadReceipt(conversationId: String) {
        if (_connectionStatus.value != WsConnectionStatus.CONNECTED) return
        try {
            val json = readAdapter.toJson(WsOutgoingRead(conversationId = conversationId))
            webSocket?.send(json)
        } catch (e: Exception) {
            Log.w(TAG, "Error sending read receipt: ${e.message}")
        }
    }

    fun sendCallSignaling(event: String, callId: String, conversationId: String? = null, data: Map<String, String>? = null) {
        if (_connectionStatus.value != WsConnectionStatus.CONNECTED) {
            connect()
        }
        try {
            val root = org.json.JSONObject().apply {
                put("event", event)
                put("type", event)
                put("callId", callId)
                put("call_id", callId)
                conversationId?.let {
                    put("conversationId", it)
                    put("conversation_id", it)
                }
                data?.forEach { (k, v) ->
                    put(k, v)
                }
                val dataObj = org.json.JSONObject()
                data?.forEach { (k, v) -> dataObj.put(k, v) }
                put("data", dataObj)
                put("callerWalletId", ApiClient.activeWalletId)
                put("senderWalletId", ApiClient.activeWalletId)
                put("timestamp", System.currentTimeMillis().toString())
            }
            webSocket?.send(root.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error sending call signaling: ${e.message}")
        }
    }

    fun disconnect() {
        shouldReconnect = false
        stopHeartbeat()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = WsConnectionStatus.DISCONNECTED
    }
}
