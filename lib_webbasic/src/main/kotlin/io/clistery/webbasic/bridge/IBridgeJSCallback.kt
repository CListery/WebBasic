package io.clistery.webbasic.bridge

import org.json.JSONObject

/**
 * Bridge callback for js
 */
interface IBridgeJSCallback {
    fun onCall(data: Any?): JSONObject?
}