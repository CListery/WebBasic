package io.clistery.webbasic.bridge

import org.json.JSONObject

/**
 * Bridge callback without response for js
 */
abstract class BridgeJSCallbackWithOutResp : IBridgeJSCallback {
    final override fun onCall(data: Any?): JSONObject? {
        onCallWithOutResp(data)
        return null
    }
    
    abstract fun onCallWithOutResp(data: Any?)
}