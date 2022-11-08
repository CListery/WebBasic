package io.clistery.webbasic

import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsCommonCode
import com.tencent.smtt.sdk.TbsListener
import com.yh.appbasic.logger.LogOwner
import com.yh.appbasic.logger.impl.TheLogFormatStrategy
import com.yh.appbasic.logger.logD
import com.yh.appbasic.share.AppBasicInitializer
import com.yh.appbasic.share.AppBasicShare

object WebBasicSettings {
    /**
     * WebBasic logger
     */
    @JvmStatic
    val WebLogger by lazy {
        LogOwner { "Web" }.onCreateFormatStrategy {
            TheLogFormatStrategy.newBuilder(it)
                .setMethodCount(1)
                .build()
        }.apply {
            if (DEBUG) {
                on()
            } else {
                off()
            }
        }
    }
    
    /**
     * web console logger
     */
    @JvmStatic
    val WebConsoleLogger by lazy {
        LogOwner { "WebConsole" }.onCreateFormatStrategy {
            TheLogFormatStrategy.newBuilder(it)
                .setMethodCount(0)
                .build()
        }.apply {
            if (DEBUG) {
                on()
            } else {
                off()
            }
        }
    }
    
    /**
     * debug switch
     */
    @JvmStatic
    var DEBUG = false
        set(value) {
            field = value
            if (value) {
                WebLogger.on()
                WebConsoleLogger.on()
            } else {
                WebLogger.off()
                WebConsoleLogger.off()
            }
        }
    
    /**
     * Bridge configs
     */
    object BridgeConfig {
        // bridge socket host (can not modify)
        const val bridgeHost = "127.0.0.1"
        
        // bridge socket port
        var bridgePort = 33399
        
        // bridge message url protocol (can not modify)
        const val BRIDGE_PROTOCOL_SCHEME = "webridge"
        
        // bridge message url split (can not modify)
        const val SCHEME_SPLIT = "://"
        
        // bridge message url cate key (can not modify)
        const val KEY_CATE = "cate"
    }
    
    /**
     * x5 TBS configs
     */
    object X5LoaderConfig {
        // enable switch
        var enable = true
        
        // delay load millisecond value for x5 core
        @JvmStatic
        var loadDelay = 5000L
    }
    
    /**
     * WebView assets loader configs
     *
     * @see io.clistery.webbasic.ext.SimpleWebViewClient
     */
    object WebViewLoader {
        // WebView user agent
        @JvmStatic
        var UA: String? = null
        
        // assets url domain
        @JvmStatic
        var DOMAIN = "appassets.device"
        
        // url access path for assets
        @JvmStatic
        var WEB_ASSETS_PATH = "assets"
        
        // url access path for res
        @JvmStatic
        var WEB_RES_PATH = "res"
        
        // url access path for internal storage
        @JvmStatic
        var WEB_INTERNAL_STORAGE_PATH = "internal_storage"
    }
}

/**
 * WebBasic initializer
 */
internal class WebBasicInit : AppBasicInitializer() {
    
    override fun onCreate(): Boolean {
        super.onCreate()
        
        logD("onCreate[${WebBasicSettings.X5LoaderConfig.enable}]",
            loggable = WebBasicSettings.WebLogger)
        
        if (!WebBasicSettings.X5LoaderConfig.enable) {
            return false
        }
        
        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.setCoreMinVersion(QbSdk.CORE_VER_ENABLE_202112)
        
        // TBS dex2oat优化
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        QbSdk.initTbsSettings(map)
        
        /* SDK内核初始化周期回调，包括 下载、安装、加载 */
        QbSdk.setTbsListener(object : TbsListener {
            /**
             * @param stateCode 用户可处理错误码请参考[com.tencent.smtt.sdk.TbsCommonCode]
             */
            override fun onDownloadFinish(stateCode: Int) {
                logD("onDownloadFinished: $stateCode", loggable = WebBasicSettings.WebLogger)
            }
            
            /**
             * @param stateCode 用户可处理错误码请参考[com.tencent.smtt.sdk.TbsCommonCode]
             */
            override fun onInstallFinish(stateCode: Int) {
                logD("onInstallFinished: $stateCode", loggable = WebBasicSettings.WebLogger)
                if (TbsCommonCode.INSTALL_SUCCESS != stateCode) {
                    logD(QbSdk.getX5CoreLoadHelp(context), loggable = WebBasicSettings.WebLogger)
                }
            }
            
            /**
             * 首次安装应用，会触发内核下载，此时会有内核下载的进度回调。
             * @param progress 0 - 100
             */
            override fun onDownloadProgress(progress: Int) {
                logD("Core Downloading: $progress", loggable = WebBasicSettings.WebLogger)
            }
        })
        
        AppBasicShare.runOnUiThread(
            { QbSdk.initX5Environment(context, null) },
            WebBasicSettings.X5LoaderConfig.loadDelay
        )
        
        return false
    }
    
}