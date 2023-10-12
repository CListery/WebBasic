# WebBasic

[中文文档](./doc/README-zh-CN.md)

> WebView - JS bridge scaffold

1. Create a communication bridge through WebSocket to realize message passing, and no longer rely on the JavascriptInterface of the system
2. Optional Tencent X5 tbs
3. One line code to load js

## Dependencies

### Gradle

```kts
implementation("io.github.clistery:webbasic:1.1.0")
```

## USE

### Android

- Configuration (requires operation when Application's onCreate)

  - Debug switch (Will affect log output, packet capture, debugging and other functions)

    ```kotlin
    WebBasicSettings.DEBUG = true
    ```

  - Modify bridge port

    ```kotlin
    WebBasicSettings.BridgeConfig.bridgePort = 23231
    ```

  - X5 switch

    ```kotlin
    WebBasicSettings.X5LoaderConfig.enable = false
    ```

  - X5 core initialization delay

    ```kotlin
    WebBasicSettings.X5LoaderConfig.loadDelay = 10000L
    ```

  - UA

    ```kotlin
    WebBasicSettings.WebViewLoader.UA = "Mozilla/5.0 (Linux; ${Build.VERSION.SDK_INT}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Mobile Safari/537.36"
    ```

  - Resource loading URL domain name

    ```kotlin
    WebBasicSettings.WebViewLoader.DOMAIN = "assets.demo"
    ```

  - Define the URL path to access assets resources

    ```kotlin
    WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH = "assets"
    ```

  - Define the URL path to access res resources

    ```kotlin
    WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH = "res"
    ```

  - Define the URL path to access internal storage

    ```kotlin
    WebBasicSettings.WebViewLoader.WEB_ASSETS_PATH = "internal_storage"
    ```

- Create Bridge WebView

    ```kotlin
    val web = X5BridgeWebView(context)
    ```

- Send data to JS

  - Send to the default receiver

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

  - Send to the specified receiver

    ```kotlin
    web.sendToJSReceiver("functionInJs",
        JSONObject().put("data", "data from Native"),
        object : BridgeJSCallbackWithOutResp() {
            override fun onCallWithOutResp(data: Any?) {
                logD("reponse data from js: $data")
            }
        })
    ```

- Receive data sent by JS

  - Register the default receiver

    ```kotlin
    web.registerDefaultJSCallback(object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("default: ${JSONObject(data.toString())}")
                return JSONObject().put("data", "default response")
            }
        })
    ```

  - Register specified name receiver

    ```kotlin
    web.registerJSCallback("submitFromWeb", object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("submitFromWeb: ${JSONObject(data.toString())}")
                return JSONObject().put("data", "ok!")
            }
        })
    ```

  - Register a one-time receiver

    ```kotlin
    web.onceJSCallback("once_hello", object : IBridgeJSCallback {
            override fun onCall(data: Any?): JSONObject? {
                logD("once_hello: $data")
                return JSONObject().put("data", "once ok!")
            }
        })
    ```

- Load the js file under assets

    ```kotlin
    web.loadAssetsJS("demo.js")
    ```

### JS

- monitor bridge status

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

- call native callback

  - Call the callback with the specified name

    ```js
    window.Webridge.callNative(
        "once_hello",
        { data: "你好" },
        function (responseData) {
            console.log("repsonseData from native, data = " + JSON.stringify(responseData));
        }
      );
    ```

- Send message to native

    ```js
    Webridge.sendToNative(data, function (responseData) {
        console.log("repsonseData from native, data = " + JSON.stringify(responseData););
      });
    ```

- Register receiver

  - Default receiver

    ```js
    window.Webridge.registerDefaultReceiver(function (data, callback) {
        console.log("[receiver] default: ", data);
        if (callback) {
          callback("js response hi~");
        }
      });
    ```

  - Register a receiver with the specified name

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

## Enhancements

> Some sites may have restrictions due to js scripting capabilities

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
