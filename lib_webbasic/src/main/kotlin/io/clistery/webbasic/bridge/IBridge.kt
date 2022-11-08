package io.clistery.webbasic.bridge

/**
 * Bridge interface
 */
internal interface IBridge {
    /**
     * bridge connect create
     */
    fun onBridgeCreate(token: String)
    
    /**
     * bridge connect state ready
     */
    fun onBridgeReady()
    
    /**
     * callback by js call native
     */
    fun onCallJSCallback(nativeCallName: String, jsCallbackId: String?, data: Any?): Boolean
    
    /**
     * receive message from js
     */
    fun onReceiveJSMessage(jsCallbackId: String?, data: Any?): Boolean
}
