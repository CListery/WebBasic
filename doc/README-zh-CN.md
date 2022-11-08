# WebBasic

> WebView - JS 桥接框架

1. 通过 WebSocket 创建通讯桥梁，实现消息传递，不再依赖系统的 JavascriptInterface
2. 可选的腾讯 X5 tbs
3. 一句代码加载 js

## 依赖

### Gradle

```kts
implementation("io.github.clistery:webbasic:1.0.0")
```

## 使用

### Android

- 配置(需要在 Application 初始化时操作)

  - 调试开关 (会影响日志输出、抓包、调试等功能)

    ```kotlin
    WebBasicSettings.DEBUG = true
    ```

  - 修改 Bridge 端口

    ```kotlin
    WebBasicSettings.BridgeConfig.bridgePort = 23231
    ```

  - 启用或禁用X5

    ```kotlin
    WebBasicSettings.X5LoaderConfig.enable = false
    ```

  - X5 初始化延迟

    ```kotlin
    WebBasicSettings.X5LoaderConfig.loadDelay = 10000L
    ```

  - UA

    ```kotlin
    WebBasicSettings.WebViewLoader.UA = "Mozilla/5.0 (Linux; ${Build.VERSION.SDK_INT}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Mobile Safari/537.36"
    ```

  - 资源加载 URL 域名

    ```kotlin
    WebBasicSettings.WebViewLoader.DOMAIN = "assets.demo"
    ```

  - 定义访问 assets 资源的 URL 路径

    ```kotlin
    WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH = "assets"
    ```

  - 定义访问 res 资源的 URL 路径

    ```kotlin
    WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH = "res"
    ```

  - 定义访问内部存储的 URL 路径

    ```kotlin
    WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH = "internal_storage"
    ```

- 创建 WebView

    ```kotlin
    val web = X5BridgeWebView(context)
    ```

- 发送数据到 JS

  - 发送到默认的接收器

    ```kotlin
    val dataJson = JSONObject()
    dataJson.put("user_name", "CListery")
    dataJson.put("country", "CN")
    web.sendToJSReceiver(data = dataJson,
        callback = object : BridgeJSCallbackWithOutResp() {
            override fun onCallWithOutResp(data: Any?) {
                logD("$data")
            }
        })
    ```

  - 发送到指定的接收器

    ```kotlin
    web.sendToJSReceiver("functionInJs",
        JSONObject().put("data", "data from Native"),
        object : BridgeJSCallbackWithOutResp() {
            override fun onCallWithOutResp(data: Any?) {
                logD("reponse data from js: $data")
            }
        })
    ```

- 接收 JS 发送的数据

  - 注册默认接收器

    ```kotlin
    web.registerDefaultJSCallback(object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("default: ${JSONObject(data.toString())}")
                return JSONObject().put("data", "default response")
            }
        })
    ```

  - 注册特定的接收器

    ```kotlin
    web.registerJSCallback("submitFromWeb", object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("submitFromWeb: ${JSONObject(data.toString())}")
                return JSONObject().put("data", "ok!")
            }
        })
    ```

  - 注册一次性接收器

    ```kotlin
    web.onceJSCallback("once_hello", object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("once_hello: $data")
                return JSONObject().put("data", "once ok!")
            }
        })
    ```

- 加载 assets 下的 js 文件

    ```kotlin
    web.loadAssetsJS("demo.js")
    ```

### JS

- 监听 bridge 状态

    ```js
    function connectWebBridge(callback) {
        console.log("connectWebBridge");
        if (window.Webridge && window.Webridge.inited) {
            callback(Webridge);
        } else {
            document.addEventListener(
                "BridgeReady",
                function () {
                    callback(Webridge);
                },
                false
            );
        }
    }

    connectWebBridge(function (bridge) {
        console.log("connectWebBridge");
        bridge.callNative(
            "once_hello",
            { data: "你好" },
            function (responseData) {
                console.log(JSON.stringify(responseData));
            }
        );
    });
    ```

- 调用 native callback

  - 调用指定名称的 callback

    ```js
    window.Webridge.callNative(
        "once_hello",
        { data: "你好" },
        function (responseData) {
            console.log("repsonseData from native, data = " + JSON.stringify(responseData));
        }
      );
    ```

- 发送消息到 native

    ```js
    Webridge.sendToNative(data, function (responseData) {
        console.log("repsonseData from native, data = " + JSON.stringify(responseData););
      });
    ```

- 注册接收器

  - 默认接收器

    ```js
    window.Webridge.registerDefaultReceiver(function (data, callback) {
        console.log("[receiver] default: ", data);
        if (callback) {
          callback("js response hi~");
        }
      });
    ```

  - 指定名称的接收器

    ```js
    window.Webridge.registerReceiver("functionInJs", function (data, callback) {
        console.log("[receiver] functionInJs: ", data);
        document.getElementById("show").innerHTML =
          Date.now() + "\nreceive native data: = " + JSON.stringify(data);
        if (callback) {
          callback("data received!");
        }
      });
    ```

## 增强功能

> 由于 js 脚本功能，部分网站可能会有限制

- [vConsole](https://github.com/Tencent/vConsole)

    ```kotlin
    web.vConsole()
    ```

- [eruda](https://github.com/liriliri/eruda)

    ```kotlin
    web.eruda()
    ```

- [chii](https://github.com/liriliri/chii)

    ```kotlin
    web.chii()
    ```

- [html2canvas](https://github.com/niklasvh/html2canvas)

    ```kotlin
    web.captureByJS { base64ImgData ->
    }
    ```
