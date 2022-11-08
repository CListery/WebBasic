package io.clistery.webbasic.ext

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.webkit.WebViewAssetLoader
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.yh.appbasic.logger.logD
import com.yh.appbasic.logger.logE
import com.yh.appbasic.logger.logW
import com.yh.appbasic.share.AppBasicShare
import io.clistery.webbasic.WebBasicSettings
import io.clistery.webbasic.bridge.IBridgeJSCallback
import io.clistery.webbasic.x5.X5BridgeWebView
import io.clistery.webbasic.x5.X5BridgeWebViewClient
import org.json.JSONObject

/**
 * auto load assets sources
 */
open class SimpleWebViewClient(
    private val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
        .setDomain(WebBasicSettings.WebViewLoader.DOMAIN)
        .setHttpAllowed(true)
        // https://appassets.device/assets/xxx
        .addPathHandler(
            "/${WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH}/",
            WebViewAssetLoader.AssetsPathHandler(AppBasicShare.context)
        )
        // https://appassets.device/res/xxx
        .addPathHandler(
            "/${WebBasicSettings.WebViewLoader.WEB_RES_PATH}/",
            WebViewAssetLoader.ResourcesPathHandler(AppBasicShare.context)
        )
        // https://appassets.device/internal_storage/xxx
        .addPathHandler(
            "/${WebBasicSettings.WebViewLoader.WEB_INTERNAL_STORAGE_PATH}/",
            WebViewAssetLoader.InternalStoragePathHandler(
                AppBasicShare.context,
                AppBasicShare.context.filesDir
            )
        )
        .build(),
) : X5BridgeWebViewClient() {
    @CallSuper
    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?,
    ) {
        logD("onReceivedSslError", loggable = WebBasicSettings.WebLogger)
        if (WebBasicSettings.DEBUG) {
            handler?.proceed()
        } else {
            super.onReceivedSslError(view, handler, error)
        }
    }
    
    @CallSuper
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        logD("onPageFinished: $url", loggable = WebBasicSettings.WebLogger)
    }
    
    @CallSuper
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        logD("shouldInterceptRequest: $url", loggable = WebBasicSettings.WebLogger)
        return try {
            shouldInterceptRequest(view, Uri.parse(url))
        } catch (e: Exception) {
            shouldInterceptRequest(view, Uri.EMPTY)
        }
    }
    
    @CallSuper
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url
        logD("shouldInterceptRequest: $url", loggable = WebBasicSettings.WebLogger)
        return shouldInterceptRequest(view, request?.url)
    }
    
    @CallSuper
    override fun shouldInterceptRequest(
        p0: WebView?,
        p1: WebResourceRequest?,
        p2: Bundle?,
    ): WebResourceResponse? {
        return shouldInterceptRequest(p0, p1?.url)
    }
    
    private fun shouldInterceptRequest(
        view: WebView?,
        url: Uri?,
    ): WebResourceResponse? {
        return if (url == null) {
            super.shouldInterceptRequest(view, url?.toString())
        } else {
            val response = assetLoader.shouldInterceptRequest(url) ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                WebResourceResponse(response.mimeType,
                    response.encoding,
                    response.statusCode,
                    response.reasonPhrase,
                    response.responseHeaders,
                    response.data)
            } else {
                WebResourceResponse(response.mimeType, response.encoding, response.data)
            }
        }
    }
}

open class SimpleWebChromeClient : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage.log()
        return true
    }
    
    private fun ConsoleMessage?.log() {
        if (null == this) return
        val msg = "${sourceId()}:${lineNumber()}\n ${message()}"
        when (messageLevel()) {
            ConsoleMessage.MessageLevel.WARNING -> logW(msg,
                loggable = WebBasicSettings.WebConsoleLogger)
            ConsoleMessage.MessageLevel.ERROR -> logE(msg,
                loggable = WebBasicSettings.WebConsoleLogger)
            else -> logD(msg, loggable = WebBasicSettings.WebConsoleLogger)
        }
    }
}

/**
 * auto setup WebView
 */
@JvmOverloads
fun WebView.basicSettings(
    ua: String? = WebBasicSettings.WebViewLoader.UA,
    wcc: WebChromeClient = SimpleWebChromeClient(),
    wvc: WebViewClient? = null,
) {
    WebView.setWebContentsDebuggingEnabled(WebBasicSettings.DEBUG)
    settings.useWideViewPort = true
    if (!ua.isNullOrEmpty()) {
        settings.userAgentString = ua
    }
    settings.javaScriptEnabled = true
    settings.allowFileAccess = true
    settings.setSupportZoom(true)
    settings.databaseEnabled = true
    settings.domStorageEnabled = true
    
    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
    
    webChromeClient = wcc
    if (null != wvc) {
        webViewClient = wvc
    }
}

/**
 * disable WebView context menu
 */
fun WebView.disableContextMenu() {
    setOnLongClickListener { true }
}

/**
 * https://github.com/liriliri/eruda
 * https://cdn.jsdelivr.net/npm/eruda@2/eruda.min.js
 */
fun WebView.eruda() = loadAssetsJS(
    jsFileName = "eruda.js",
    groupVarName = "eruda",
    initInvoke = "eruda.init();",
)

/**
 * https://github.com/Tencent/vConsole
 * https://cdn.jsdelivr.net/npm/vconsole@3/dist/vconsole.min.js
 */
fun WebView.vConsole() = loadAssetsJS(
    jsFileName = "vconsole.js",
    groupVarName = "VConsole",
    initInvoke = "new VConsole();",
)

/**
 * https://github.com/liriliri/chii
 * https://cdn.jsdelivr.net/npm/chii@1.6.2/server/index.min.js
 *
 * @param chiiServerUrl default: https://chii.liriliri.io/
 */
@JvmOverloads
fun WebView.chii(chiiServerUrl: String = "https://chii.liriliri.io") {
    eval("window.ChiiServerUrl='$chiiServerUrl'")
    loadAssetsJS(
        jsFileName = "chii.js",
        groupVarName = "chii",
    )
//    eval("javascript:(function () { var script = document.createElement('script'); script.src=\"//192.168.3.223:9099/target.js\"; document.body.appendChild(script); })();")
}

/**
 * https://github.com/niklasvh/html2canvas
 * https://cdn.jsdelivr.net/npm/html2canvas@1/dist/html2canvas.min.js
 *
 * @param option
 *          {
 *          allowTaint: true, // 允许跨域图片
 *          useCORS: true, // 是否尝试使用CORS从服务器加载图像
 *          }
 */
@JvmOverloads
fun X5BridgeWebView.capture(
    elementQuery: String = "body",
    option: String = "{}",
    callback: (imgData: String?) -> Unit,
) {
    onceJSCallback("receiver_html2canvas_img", object : IBridgeJSCallback {
        override fun onCall(data: Any?): JSONObject? {
            callback(data?.toString())
            return JSONObject().put("data", "ok")
        }
    })
    loadAssetsJS(
        jsFileName = "html2canvas.js",
        groupVarName = "html2canvas",
        execInvoke = """
            let select = document.querySelector("$elementQuery");
            html2canvas(select, $option).then(canvas => {
                const base64ImgData = canvas.toDataURL('image/jpeg');
                window.Webridge.callNative("receiver_html2canvas_img", base64ImgData);
            });
        """,
    )
}

/**
 * load javascript file from assets
 */
@JvmOverloads
fun WebView.loadAssetsJS(
    jsFileName: String,
    groupVarName: String,
    initInvoke: String = "console.log('init...');",
    execInvoke: String = "console.log('exec...');",
) {
    fun convFunction(code: String) = if (code.isEmpty()) "" else "{ $code }"
    
    val onload = """
        function () {
          if ("function" === typeof define && define.amd) {
            require(["$groupVarName"], function ($groupVarName) {
              window["$groupVarName"] = $groupVarName;
              ${convFunction(initInvoke)}
              ${convFunction(execInvoke)}
            });
          } else {
            window["$groupVarName"] = $groupVarName;
            ${convFunction(initInvoke)}
            ${convFunction(execInvoke)}
          }
        };
        """
    eval("""
        if ("undefined" !== typeof window["$groupVarName"]) {
          ${convFunction(execInvoke)}
        } else {
          let elementScript = document.createElement("script");
          elementScript.src="https://${WebBasicSettings.WebViewLoader.DOMAIN}/${WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH}/${jsFileName}";
          document.body.appendChild(elementScript);
          elementScript.onload = $onload
        }
        """)
}

/**
 * safe exec js command
 */
fun WebView.eval(jsCommand: String) {
    var completeCommand = jsCommand.lines()
        .joinToString("") { it.replace(Regex("(/\\*).*(\\*/)"), "").trimIndent() }
    logD("eval: $completeCommand", loggable = WebBasicSettings.WebLogger)
    if (!completeCommand.startsWith("javascript", true)) {
        completeCommand = "javascript:(function(){$jsCommand})();"
    }
    AppBasicShare.runOnUiThread({
        // 2097152: loadUrl can exec max length js command
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && completeCommand.length >= 2097152) {
            evaluateJavascript(completeCommand, null)
        } else {
            loadUrl(completeCommand)
        }
    })
}
