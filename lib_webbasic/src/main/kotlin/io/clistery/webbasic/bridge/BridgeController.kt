package io.clistery.webbasic.bridge

import android.os.Handler
import android.os.HandlerThread
import com.kotlin.runCatchingSafety
import com.yh.appbasic.logger.logD
import com.yh.appbasic.logger.logE
import com.yh.appbasic.share.AppBasicShare
import io.clistery.webbasic.WebBasicSettings
import io.clistery.webbasic.ext.eval
import io.clistery.webbasic.x5.IWebPageStateChangeListener
import io.clistery.webbasic.x5.X5BridgeWebView
import io.clistery.webbasic.x5.X5BridgeWebViewClient
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.*

/**
 * storage bridge information
 */
private class BridgeInfo(
    // bridge unique token
    val token: String,
    // bridge web view
    private var bridgeWebView: X5BridgeWebView?,
) {
    // bridge socket by js
    private var socket: WebSocket? = null
    // bridge ready state flag
    val isReady: Boolean get() = socket?.isOpen ?: false
    
    // message queue
    private val messageQueue = arrayListOf<JSONObject>()
    
    fun ready(bridgeSocket: WebSocket) {
        socket = bridgeSocket
        bridgeWebView?.onBridgeReady()
        sendAll()
    }
    
    fun sendAll() {
        if (isReady) {
            messageQueue.forEach {
                internalSendBridgeMessage(it)
            }
            messageQueue.clear()
        }
    }
    
    fun send(containerJson: JSONObject) {
        if (isReady) {
            internalSendBridgeMessage(containerJson)
        } else {
            messageQueue.add(containerJson)
        }
    }
    
    private fun internalSendBridgeMessage(containerJson: JSONObject) {
        socket?.send(containerJson.toString())
    }
    
    fun release() {
        bridgeWebView = null
        socket?.runCatchingSafety {
            close()
        }
    }
    
    fun receiveJSMessage(jsCallbackId: String?, data: String) {
        bridgeWebView?.onReceiveJSMessage(jsCallbackId, data)
    }
    
    fun receiveJSCall(nativeCall: String, jsCallbackId: String, data: String) {
        bridgeWebView?.onCallJSCallback(nativeCall, jsCallbackId, data)
    }
    
    fun setupWVC(wvc: X5BridgeWebViewClient) {
        wvc.pageStateChangeListener = object : IWebPageStateChangeListener {
            override fun onPageStarted() {
            
            }
            
            override fun onPageFinished() {
                AppBasicShare.runOnUiThread({
                    bridgeWebView?.eval(
                        """
                        let elementScript = document.createElement("script");
                        elementScript.src="https://${WebBasicSettings.WebViewLoader.DOMAIN}/${WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH}/WebBridge.js";
                        document.body.appendChild(elementScript);
                        elementScript.onload = function(){
                            Webridge.init('${WebBasicSettings.BridgeConfig.bridgeHost}', '${WebBasicSettings.BridgeConfig.bridgePort}', '$token');
                        };
                        """
                    )
                })
            }
        }
    }
    
}

/**
 * bridge controller
 */
internal object BridgeController {
    
    // bridge socket by native
    private var wss: WebSocketServer? = null
    
    // bridge message handler
    private val handler: Handler
    
    // server socket ready
    private const val MSG_BRIDGE_SERVER_READY = 0x1
    // open bridge connect
    private const val MSG_CREATE_BRIDGE = 0x2
    // close bridge connect
    private const val MSG_CLOSE_BRIDGE = 0x3
    // bridge connect ready state
    private const val MSG_BRIDGE_READY = 0x4
    // send bridge message to js
    private const val MSG_SEND_TO_JS = 0x5
    // receive js call native
    private const val MSG_RECEIVE_JS_CALL = 0x6
    // receive js message
    private const val MSG_RECEIVE_JS_MESSAGE = 0x7
    
    // bridge server ready flag
    var bridgeServerReady = false
    // bridge connects pool
    private val bridgeContainers = hashMapOf<String, BridgeInfo>()
    
    // bridge message handler callback
    private val handlerCallback by lazy {
        Handler.Callback { msg ->
            val msgObj = msg.obj
            when (msg.what) {
                MSG_BRIDGE_SERVER_READY -> {
                    bridgeServerReady = true
                }
                MSG_CREATE_BRIDGE -> {
                    if (msgObj is Pair<*, *>) {
                        val bridgeWebView = msgObj.first as X5BridgeWebView
                        val wvc = msgObj.second as X5BridgeWebViewClient
                        val bridgeToken = createBridgeToken()
                        val bridgeInfo = BridgeInfo(bridgeToken, bridgeWebView)
                        bridgeInfo.setupWVC(wvc)
                        bridgeContainers[bridgeToken] = bridgeInfo
                        bridgeWebView.onBridgeCreate(bridgeToken)
                    }
                    internalCreateSocket()
                }
                MSG_CLOSE_BRIDGE -> {
                    if (msgObj is String) {
                        bridgeContainers.remove(msgObj)?.release()
                    }
                    if (bridgeContainers.isEmpty()) {
                        wss?.stop()
                        wss = null
                    }
                }
                MSG_BRIDGE_READY -> {
                    if (msgObj is Pair<*, *>) {
                        val bridgeToken = msgObj.first as String
                        val bridgeSocket = msgObj.second as WebSocket
                        val bridgeInfo = bridgeContainers[bridgeToken]
                        if (null == bridgeInfo) {
                            bridgeSocket.runCatchingSafety {
                                bridgeSocket.close()
                            }
                        } else {
                            bridgeInfo.ready(bridgeSocket)
                        }
                    }
                }
                MSG_SEND_TO_JS -> {
                    if (msgObj is Pair<*, *>) {
                        val bridgeToken = msgObj.first as String
                        val bridgeInfo = bridgeContainers[bridgeToken]
                        if (null != bridgeInfo) {
                            val containerJson = msgObj.second as JSONObject
                            bridgeInfo.send(containerJson)
                        }
                    }
                }
                MSG_RECEIVE_JS_CALL -> {
                    if (msgObj is Pair<*, *>) {
                        val bridgeToken = msgObj.first as String
                        val bridgeInfo = bridgeContainers[bridgeToken]
                        if (null != bridgeInfo) {
                            val containerJson = msgObj.second as JSONObject
                            val nativeCall = containerJson.optString("native_call")
                            if (!nativeCall.isNullOrEmpty()) {
                                val jsCallbackId = containerJson.optString("callback_id")
                                val data = containerJson.optString("data")
                                bridgeInfo.receiveJSCall(nativeCall, jsCallbackId, data)
                            }
                        }
                    }
                }
                MSG_RECEIVE_JS_MESSAGE -> {
                    if (msgObj is Pair<*, *>) {
                        val bridgeToken = msgObj.first as String
                        val bridgeInfo = bridgeContainers[bridgeToken]
                        if (null != bridgeInfo) {
                            val containerJson = msgObj.second as JSONObject
                            val jsCallbackId = containerJson.optString("callback_id")
                            val data = containerJson.optString("data")
                            bridgeInfo.receiveJSMessage(jsCallbackId, data)
                        }
                    }
                }
            }
            true
        }
    }
    
    /**
     * create bridge unique token
     */
    private fun createBridgeToken(): String = UUID.randomUUID().toString()
    
    init {
        val handlerThread = HandlerThread("bridge-controller")
        handlerThread.start()
        handler = Handler(handlerThread.looper, handlerCallback)
    }
    
    /**
     * open bridge connect
     */
    fun openBridge(callback: IBridge, client: X5BridgeWebViewClient) {
        logD("openBridge")
        handler.obtainMessage(MSG_CREATE_BRIDGE, Pair(callback, client)).sendToTarget()
    }
    
    /**
     * close bridge connect
     */
    fun closeBridge(token: String?) {
        logD("closeBridge: $token")
        if (token.isNullOrEmpty()) {
            return
        }
        handler.obtainMessage(MSG_CLOSE_BRIDGE, token).sendToTarget()
    }
    
    /**
     * send message to js
     */
    fun sendToJS(token: String?, containerJson: JSONObject) {
        logD("sendToJS: $token")
        if (token.isNullOrEmpty()) {
            return
        }
        handler.obtainMessage(MSG_SEND_TO_JS, Pair(token, containerJson)).sendToTarget()
    }
    
    /**
     * create server socket
     */
    private fun internalCreateSocket() {
        fun internalCreateBridge(): WebSocketServer {
            val bridgeAddress = InetSocketAddress(
                WebBasicSettings.BridgeConfig.bridgeHost,
                WebBasicSettings.BridgeConfig.bridgePort,
            )
            val wss = object : WebSocketServer(bridgeAddress) {
                override fun onStart() {
                    logD("onStart", loggable = WebBasicSettings.WebLogger)
                    handler.sendEmptyMessage(MSG_BRIDGE_SERVER_READY)
                }
                
                override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                    logD("onOpen: ${conn.printInfo()} ${handshake?.content?.decodeToString()}",
                        loggable = WebBasicSettings.WebLogger)
                }
                
                override fun onClose(
                    conn: WebSocket?,
                    code: Int,
                    reason: String?,
                    remote: Boolean,
                ) {
                    logD("onClose ${conn.printInfo()} $code $reason $remote",
                        loggable = WebBasicSettings.WebLogger)
                }
                
                override fun onMessage(conn: WebSocket?, message: String?) {
                    logD("onMessage: ${conn.printInfo()} $message",
                        loggable = WebBasicSettings.WebLogger)
                    if (null == conn) {
                        return
                    }
                    if (message.isNullOrEmpty()) {
                        return
                    }
                    if (!message.contains(WebBasicSettings.BridgeConfig.SCHEME_SPLIT)) {
                        return
                    }
                    val result = message.split(WebBasicSettings.BridgeConfig.SCHEME_SPLIT)
                    if (result.size != 2) {
                        return
                    }
                    
                    if (WebBasicSettings.BridgeConfig.BRIDGE_PROTOCOL_SCHEME.equals(result[0],
                            true)
                    ) {
                        internalHandleBridgeMessage(conn, result)
                    }
                }
                
                override fun onError(conn: WebSocket?, ex: Exception?) {
                    logD("onError: ${conn.printInfo()} ${ex?.stackTraceToString()}",
                        loggable = WebBasicSettings.WebLogger)
                }
            }
            wss.isReuseAddr = true
            wss.start()
            return wss
        }
        
        wss = wss ?: internalCreateBridge()
    }
    
    /**
     * handle bridge message
     */
    private fun internalHandleBridgeMessage(bridgeSocket: WebSocket, result: List<String>) {
        fun handle(cate: BridgeMessageCate, containerJson: JSONObject, bridgeToken: String) {
            when (cate) {
                BridgeMessageCate.UNKNOWN,
                BridgeMessageCate.NativeResponse,
                BridgeMessageCate.NativeSendToJS,
                -> {
                }
                BridgeMessageCate.StateChange -> {
                    if (1 == containerJson.optInt("code", -1)) {
                        handler.obtainMessage(
                            MSG_BRIDGE_READY,
                            Pair(bridgeToken, bridgeSocket),
                        ).sendToTarget()
                    }
                }
                BridgeMessageCate.JSCallNative -> {
                    handler.obtainMessage(
                        MSG_RECEIVE_JS_CALL,
                        Pair(bridgeToken, containerJson),
                    ).sendToTarget()
                }
                BridgeMessageCate.JSSendToNative -> {
                    handler.obtainMessage(
                        MSG_RECEIVE_JS_MESSAGE,
                        Pair(bridgeToken, containerJson),
                    ).sendToTarget()
                }
            }
        }
        
        val json = JSONObject(result[1])
        val cateName = json.optString(WebBasicSettings.BridgeConfig.KEY_CATE)
        when (val cate = BridgeMessageCate.parse(cateName)) {
            BridgeMessageCate.UNKNOWN -> {
                logE("not support $cateName")
            }
            else -> {
                val containerJson = json.optJSONObject("container")
                val token = json.optString("token")
                if (null != containerJson && !token.isNullOrEmpty()) {
                    handle(cate, containerJson, token)
                }
            }
        }
    }
}

/**
 * bridge message cate
 */
internal enum class BridgeMessageCate(val cateName: String) {
    UNKNOWN(""),
    StateChange("state"),
    JSCallNative("js_call_native"),
    NativeResponse("native_response"),
    JSSendToNative("js_send_to_native"),
    NativeSendToJS("native_send_to_js"),
    ;
    
    companion object {
        @JvmStatic
        fun parse(cn: String): BridgeMessageCate {
            return values().find { cn == it.cateName } ?: UNKNOWN
        }
    }
}

/**
 * println js socket connect information
 */
private fun WebSocket?.printInfo(): String {
    if (null == this) {
        return ""
    }
    return "${remoteSocketAddress.address.hostAddress}:${remoteSocketAddress.port}"
}