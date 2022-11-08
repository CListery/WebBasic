package io.clistery.webbasic.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.yh.appbasic.logger.logD
import com.yh.appbasic.ui.ViewBindingActivity
import io.clistery.webbasic.bridge.BridgeJSCallbackWithOutResp
import io.clistery.webbasic.bridge.IBridgeJSCallback
import io.clistery.webbasic.demo.databinding.ActMainBinding
import io.clistery.webbasic.ext.*
import org.json.JSONObject

class MainAct : ViewBindingActivity<ActMainBinding>() {
    override fun binderCreator(savedInstanceState: Bundle?) = ActMainBinding.inflate(layoutInflater)
    
    override fun ActMainBinding.onInit(savedInstanceState: Bundle?) {


//        web.loadUrl("http://soft.imtt.qq.com/browser/tes/feedback.html")
//        web.loadUrl("https://debugtbs.qq.com")
//        web.loadUrl("file:///android_asset/webpage/homePage.html")
        web.loadUrl("file:///android_asset/demo.html")
//        web.loadUrl("https://m.fangstar.com")
//        web.loadUrl("https://www.baidu.com")
        
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(p0: WebView?, p1: String?): Boolean {
                return true != p1?.startsWith("http")
            }
            
            override fun shouldOverrideUrlLoading(p0: WebView?, p1: WebResourceRequest?): Boolean {
                return shouldOverrideUrlLoading(p0, p1?.url?.toString())
            }
        }
        
        btnInvokeJs.setOnClickListener {
            web.sendToJSReceiver("functionInJs",
                JSONObject().put("data", "data from Native"),
                object : BridgeJSCallbackWithOutResp() {
                    override fun onCallWithOutResp(data: Any?) {
                        logD("reponse data from js: $data")
                    }
                })
        }
        
        val dataJson = JSONObject()
        dataJson.put("user_name", "CListery")
        dataJson.put("country", "CN")
        web.sendToJSReceiver(data = dataJson)
        
        web.sendToJSReceiver(data = JSONObject().put("data", "hello js bridge"),
            callback = object : BridgeJSCallbackWithOutResp() {
                override fun onCallWithOutResp(data: Any?) {
                    logD("$data")
                }
            })
        
        web.registerDefaultJSCallback(object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("default: ${JSONObject(data.toString())}")
                return JSONObject().put("data", "default response")
            }
        })
        
        web.registerJSCallback("submitFromWeb", object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("submitFromWeb: ${JSONObject(data.toString())}")
                return JSONObject().put("data", "ok!")
            }
        })
        
        web.onceJSCallback("once_hello", object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("once_hello: $data")
                return JSONObject().put("data", "once ok!")
            }
        })
    }
    
    override fun onResume() {
        super.onResume()
        changeBinder {
            Toast.makeText(this@MainAct,
                if (web.isX5Core) "X5内核: " + QbSdk.getTbsVersion(this@MainAct) else "SDK系统内核",
                Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bridge_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.btn_vconsole -> {
                changeBinder {
                    web.vConsole()
                }
            }
            R.id.btn_eruda -> {
                changeBinder {
                    web.eruda()
                }
            }
            R.id.btn_chii -> {
                changeBinder {
                    web.chii()
                }
            }
            R.id.btn_capture -> {
                changeBinder {
                    web.testCapture()
                }
            }
        }
        return true
    }
    
}