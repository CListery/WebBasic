package io.clistery.webbasic.x5

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.yh.appbasic.logger.logD
import io.clistery.webbasic.WebBasicSettings

internal interface IWebPageStateChangeListener {
    fun onPageStarted()
    fun onPageFinished()
}

open class X5BridgeWebViewClient : WebViewClient() {
    
    var client: WebViewClient? = null
    
    private var pageFinished = false
    internal var pageStateChangeListener: IWebPageStateChangeListener? = null
        set(value) {
            field = value
            if (pageFinished) {
                field?.onPageFinished()
            }
        }
    
    override fun onLoadResource(p0: WebView?, p1: String?) {
        logD("onLoadResource: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        client?.onLoadResource(p0, p1) ?: super.onLoadResource(p0, p1)
    }
    
    override fun shouldOverrideUrlLoading(p0: WebView?, p1: String?): Boolean {
        logD("shouldOverrideUrlLoading: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        return client?.shouldOverrideUrlLoading(p0, p1)
            ?: super.shouldOverrideUrlLoading(p0, p1)
    }
    
    override fun shouldOverrideUrlLoading(p0: WebView?, p1: WebResourceRequest?): Boolean {
        return client?.shouldOverrideUrlLoading(p0, p1)
            ?: shouldOverrideUrlLoading(p0, p1?.url?.toString())
    }
    
    override fun onPageStarted(p0: WebView?, p1: String?, p2: Bitmap?) {
        logD("onPageStarted: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        pageFinished = false
        pageStateChangeListener?.onPageStarted()
        client?.onPageStarted(p0, p1, p2) ?: super.onPageStarted(p0, p1, p2)
    }
    
    override fun onPageFinished(p0: WebView?, p1: String?) {
        logD("onPageFinished: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        pageFinished = false
        pageStateChangeListener?.onPageFinished()
        client?.onPageFinished(p0, p1) ?: super.onPageFinished(p0, p1)
    }
    
    override fun onReceivedError(p0: WebView?, p1: Int, p2: String?, p3: String?) {
        logD("onReceivedError: $p1 , $p2 , $p3 , $client", loggable = WebBasicSettings.WebLogger)
        client?.onReceivedError(p0, p1, p2, p3) ?: super.onReceivedError(p0, p1, p2, p3)
    }
    
    override fun onReceivedError(p0: WebView?, p1: WebResourceRequest?, p2: WebResourceError?) {
        logD("onReceivedError: ${p1?.url} , ${p2?.description} , $client",
            loggable = WebBasicSettings.WebLogger)
        client?.onReceivedError(p0, p1, p2) ?: super.onReceivedError(p0, p1, p2)
    }
    
    override fun onReceivedHttpError(
        p0: WebView?,
        p1: WebResourceRequest?,
        p2: WebResourceResponse?,
    ) {
        logD("onReceivedHttpError: $client", loggable = WebBasicSettings.WebLogger)
        client?.onReceivedHttpError(p0, p1, p2) ?: super.onReceivedHttpError(p0, p1, p2)
    }
    
    override fun shouldInterceptRequest(p0: WebView?, p1: String?): WebResourceResponse? {
        logD("shouldInterceptRequest: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        val response = client?.shouldInterceptRequest(p0, p1)
        return response ?: super.shouldInterceptRequest(p0, p1)
    }
    
    override fun shouldInterceptRequest(
        p0: WebView?,
        p1: WebResourceRequest?,
    ): WebResourceResponse? {
        logD("shouldInterceptRequest: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        val response = client?.shouldInterceptRequest(p0, p1)
        return response ?: super.shouldInterceptRequest(p0, p1)
    }
    
    override fun shouldInterceptRequest(
        p0: WebView?,
        p1: WebResourceRequest?,
        p2: Bundle?,
    ): WebResourceResponse? {
        logD("shouldInterceptRequest: $p1 , $client", loggable = WebBasicSettings.WebLogger)
        val response = client?.shouldInterceptRequest(p0, p1, p2)
        return response ?: super.shouldInterceptRequest(p0, p1, p2)
    }
    
    override fun doUpdateVisitedHistory(p0: WebView?, p1: String?, p2: Boolean) {
        logD("doUpdateVisitedHistory: $client", loggable = WebBasicSettings.WebLogger)
        client?.doUpdateVisitedHistory(p0, p1, p2) ?: super.doUpdateVisitedHistory(p0, p1, p2)
    }
    
    override fun onFormResubmission(p0: WebView?, p1: Message?, p2: Message?) {
        logD("onFormResubmission: $client", loggable = WebBasicSettings.WebLogger)
        client?.onFormResubmission(p0, p1, p2) ?: super.onFormResubmission(p0, p1, p2)
    }
    
    override fun onReceivedHttpAuthRequest(
        p0: WebView?,
        p1: HttpAuthHandler?,
        p2: String?,
        p3: String?,
    ) {
        logD("onReceivedHttpAuthRequest: $client", loggable = WebBasicSettings.WebLogger)
        client?.onReceivedHttpAuthRequest(p0, p1, p2, p3) ?: super.onReceivedHttpAuthRequest(p0,
            p1,
            p2,
            p3)
    }
    
    override fun onReceivedSslError(p0: WebView?, p1: SslErrorHandler?, p2: SslError?) {
        logD("onReceivedSslError: $client", loggable = WebBasicSettings.WebLogger)
        client?.onReceivedSslError(p0, p1, p2) ?: super.onReceivedSslError(p0, p1, p2)
    }
    
    override fun onReceivedClientCertRequest(p0: WebView?, p1: ClientCertRequest?) {
        logD("onReceivedClientCertRequest: $client", loggable = WebBasicSettings.WebLogger)
        client?.onReceivedClientCertRequest(p0, p1) ?: super.onReceivedClientCertRequest(p0, p1)
    }
    
    override fun onScaleChanged(p0: WebView?, p1: Float, p2: Float) {
        logD("onScaleChanged: $client", loggable = WebBasicSettings.WebLogger)
        client?.onScaleChanged(p0, p1, p2) ?: super.onScaleChanged(p0, p1, p2)
    }
    
    override fun onUnhandledKeyEvent(p0: WebView?, p1: KeyEvent?) {
        logD("onUnhandledKeyEvent: $client", loggable = WebBasicSettings.WebLogger)
        client?.onUnhandledKeyEvent(p0, p1) ?: super.onUnhandledKeyEvent(p0, p1)
    }
    
    override fun shouldOverrideKeyEvent(p0: WebView?, p1: KeyEvent?): Boolean {
        logD("shouldOverrideKeyEvent: $client", loggable = WebBasicSettings.WebLogger)
        return client?.shouldOverrideKeyEvent(p0, p1) ?: super.shouldOverrideKeyEvent(p0, p1)
    }
    
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onTooManyRedirects(p0: WebView?, p1: Message?, p2: Message?) {
        logD("onTooManyRedirects: $client", loggable = WebBasicSettings.WebLogger)
        client?.onTooManyRedirects(p0, p1, p2) ?: super.onTooManyRedirects(p0, p1, p2)
    }
    
    override fun onReceivedLoginRequest(p0: WebView?, p1: String?, p2: String?, p3: String?) {
        logD("onReceivedLoginRequest: $client", loggable = WebBasicSettings.WebLogger)
        client?.onReceivedLoginRequest(p0, p1, p2, p3) ?: super.onReceivedLoginRequest(p0,
            p1,
            p2,
            p3)
    }
    
    override fun onDetectedBlankScreen(p0: String?, p1: Int) {
        logD("onDetectedBlankScreen: $client", loggable = WebBasicSettings.WebLogger)
        client?.onDetectedBlankScreen(p0, p1) ?: super.onDetectedBlankScreen(p0, p1)
    }
    
    override fun onPageCommitVisible(p0: WebView?, p1: String?) {
        logD("onPageCommitVisible: $client", loggable = WebBasicSettings.WebLogger)
        client?.onPageCommitVisible(p0, p1) ?: super.onPageCommitVisible(p0, p1)
    }
    
    override fun onRenderProcessGone(p0: WebView?, p1: RenderProcessGoneDetail?): Boolean {
        logD("onRenderProcessGone: $client", loggable = WebBasicSettings.WebLogger)
        return client?.onRenderProcessGone(p0, p1) ?: super.onRenderProcessGone(p0, p1)
    }
}