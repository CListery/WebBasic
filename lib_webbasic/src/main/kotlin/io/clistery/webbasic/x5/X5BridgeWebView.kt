package io.clistery.webbasic.x5

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.yh.appbasic.logger.logD
import com.yh.appbasic.share.AppBasicShare
import io.clistery.webbasic.WebBasicSettings
import io.clistery.webbasic.bridge.*
import io.clistery.webbasic.ext.SimpleWebViewClient
import io.clistery.webbasic.ext.basicSettings
import org.json.JSONObject
import java.util.*

private typealias OnceBridgeJSCallback = Pair<IBridgeJSCallback, Boolean>

@Suppress("unused")
class X5BridgeWebView : WebView, IBridge {
    
    // WebView client
    private var client: X5BridgeWebViewClient? = null
    
    // default callback for js
    private var defaultJSCallback: IBridgeJSCallback? = null
    // callbacks for js
    private val bridgeJSCallbacks = hashMapOf<String, OnceBridgeJSCallback>()
    // response callback for js
    private val bridgeJSResponseCallback = hashMapOf<String, IBridgeJSCallback>()
    // bridge connect ready flag
    private var bridgeReady = false
    // bridge unique token
    private var bridgeToken: String? = null
    
    // <editor-fold desc="WebView">
    constructor(p0: Context?, p1: Boolean) : super(p0, p1) {
        init()
    }
    
    constructor(p0: Context?) : super(p0) {
        init()
    }
    
    constructor(p0: Context?, p1: AttributeSet?) : super(p0, p1) {
        init()
    }
    
    constructor(p0: Context?, p1: AttributeSet?, p2: Int) : super(p0, p1, p2) {
        init()
    }
    
    @Suppress("DEPRECATION")
    constructor(p0: Context?, p1: AttributeSet?, p2: Int, p3: Boolean) : super(p0, p1, p2, p3) {
        init()
    }
    
    constructor(
        p0: Context?,
        p1: AttributeSet?,
        p2: Int,
        p3: MutableMap<String, Any>?,
        p4: Boolean,
    ) : super(p0, p1, p2, p3, p4) {
        init()
    }
    
    override fun loadUrl(url: String?) {
        logD("loadUrl: $url", loggable = WebBasicSettings.WebLogger)
        super.loadUrl(url)
    }
    
    override fun loadUrl(url: String?, additionalHttpHeaders: Map<String?, String?>?) {
        logD("loadUrl: $url , $additionalHttpHeaders", loggable = WebBasicSettings.WebLogger)
        super.loadUrl(url, additionalHttpHeaders)
    }
    
    override fun loadDataWithBaseURL(
        data: String?,
        baseUrl: String?,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?,
    ) {
        logD("loadDataWithBaseURL: $baseUrl")
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }
    
    override fun setWebViewClient(p0: WebViewClient?) {
        logD("setWebViewClient: $p0", loggable = WebBasicSettings.WebLogger)
        client?.client = p0
    }
    
    override fun getWebViewClient(): WebViewClient? {
        return client?.client
    }
    
    override fun destroy() {
        logD("destroy[$bridgeToken]", loggable = WebBasicSettings.WebLogger)
        super.destroy()
        BridgeController.closeBridge(bridgeToken)
        client = null
    }
    // </editor-fold>
    
    private fun init() {
        clearCache(true)
        basicSettings()
        settings.apply {
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            @Suppress("DEPRECATION")
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setWebContentsDebuggingEnabled(WebBasicSettings.DEBUG)
        }
        val wvc = SimpleWebViewClient()
        client = wvc
        super.setWebViewClient(wvc)
        
        BridgeController.openBridge(this, wvc)
    }
    
    // <editor-fold desc="Bridge">
    /**
     * bridge ready flag
     */
    val isBridgeReady get() = bridgeReady && !bridgeToken.isNullOrEmpty()
    
    /**
     * send message to js
     */
    @JvmOverloads
    fun sendToJSReceiver(
        jsReceiverName: String? = null,
        data: JSONObject,
        callback: BridgeJSCallbackWithOutResp? = null,
    ) {
        val messageJson = JSONObject()
        messageJson.put("cate", BridgeMessageCate.NativeSendToJS.cateName)
        messageJson.put("container", data)
        if (null != callback) {
            val callbackId = "native_id_" + UUID.randomUUID().toString()
            messageJson.put("callback_id", callbackId)
            bridgeJSCallbacks[callbackId] = Pair(callback, true)
        }
        if (!jsReceiverName.isNullOrEmpty()) {
            messageJson.put("call_js_name", jsReceiverName)
        }
        BridgeController.sendToJS(bridgeToken, messageJson)
    }
    
    /**
     * register once callback for js
     */
    fun onceJSCallback(callNameForJS: String, callback: IBridgeJSCallback) {
        bridgeJSCallbacks[callNameForJS] = Pair(callback, true)
    }
    
    /**
     * register callback for js
     */
    fun registerJSCallback(callNameForJS: String, callback: IBridgeJSCallback) {
        bridgeJSCallbacks[callNameForJS] = Pair(callback, false)
    }
    
    /**
     * unregister callback for js
     */
    fun unRegisterJSCallback(callNameForJS: String) {
        bridgeJSCallbacks.remove(callNameForJS)
    }
    
    /**
     * register default callback for js
     */
    fun registerDefaultJSCallback(callback: IBridgeJSCallback) {
        defaultJSCallback = callback
    }
    
    /**
     * unregister default callback for js
     */
    fun unRegisterDefaultJSCallback() {
        defaultJSCallback = null
    }
    
    override fun onBridgeCreate(token: String) {
        logD("onBridgeCreate: $token", loggable = WebBasicSettings.WebLogger)
        bridgeToken = token
    }
    
    override fun onBridgeReady() {
        logD("onBridgeReady", loggable = WebBasicSettings.WebLogger)
        bridgeReady = true
    }
    
    override fun onCallJSCallback(
        nativeCallName: String,
        jsCallbackId: String?,
        data: Any?,
    ): Boolean {
        val (callback, isOnce) = bridgeJSCallbacks[nativeCallName] ?: return false
        if (isOnce) {
            bridgeJSCallbacks.remove(nativeCallName)
        }
        AppBasicShare.runOnUiThread({
            val dataJson = callback.onCall(data)
            if (!jsCallbackId.isNullOrEmpty()) {
                val responseJson = JSONObject()
                responseJson.put("cate", BridgeMessageCate.NativeResponse.cateName)
                responseJson.put("callback_id", jsCallbackId)
                responseJson.put("container", dataJson)
                BridgeController.sendToJS(bridgeToken, responseJson)
            }
        })
        return true
    }
    
    override fun onReceiveJSMessage(jsCallbackId: String?, data: Any?): Boolean {
        val callback = defaultJSCallback ?: return false
        AppBasicShare.runOnUiThread({
            val dataJson = callback.onCall(data)
            if (!jsCallbackId.isNullOrEmpty()) {
                val responseJson = JSONObject()
                responseJson.put("cate", BridgeMessageCate.NativeResponse.cateName)
                responseJson.put("callback_id", jsCallbackId)
                responseJson.put("container", dataJson)
                BridgeController.sendToJS(bridgeToken, responseJson)
            }
        })
        return true
    }
    // </editor-fold>
}