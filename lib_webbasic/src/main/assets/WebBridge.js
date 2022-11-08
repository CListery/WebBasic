(function () {
  if (window.Webridge) {
    if (window.Webridge.inited) {
      return;
    }
  } else {
    window.Webridge = {};
  }

  function _printLog(...data) {
    if (typeof console != "undefined") {
      console.log(data);
    }
  }

  const BRIDAGE_PROTOCOL_SCHEME = "webridge://";

  var isChannelReady = false;
  let penddingSendQueue = [];

  var responseCallbacks = {};

  var socket;
  var readyEvent;

  // 接收 native 发送的消息
  var _receivers = {};

  function _send(data) {
    _printLog(`Webridge: _send[${isChannelReady}]: ${data}`);
    if (isChannelReady) {
      if (socket.OPEN) {
        socket.send(BRIDAGE_PROTOCOL_SCHEME + data);
        return;
      }
    }
    penddingSendQueue.push(data);
  }

  function _createMessageJson(cateName, data) {
    _printLog("Webridge: _createMessageJson: ", cateName + ", d: " + data);
    let responseJson = {};
    responseJson["cate"] = cateName;
    responseJson["token"] = Webridge._token;
    if (typeof data != "object") {
      data = {
        data: data,
      };
    }
    responseJson["container"] = data;
    return JSON.stringify(responseJson);
  }

  function _createBridgeChannel(address, port) {
    _printLog("Webridge: _createBridgeChannel");
    socket = new WebSocket("ws://" + address + ":" + port);
    socket.onopen = function (e) {
      _printLog("Webridge: socket open!");
      isChannelReady = true;
      let queue = penddingSendQueue;
      for (var i = 0; i < queue.length; i++) {
        _send(queue[i]);
        delete penddingSendQueue[i];
      }
      document.dispatchEvent(readyEvent);
    };
    socket.onmessage = function (event) {
      _printLog(`Webridge: [message] Data received from server: ${event.data}`);
      _handleMessage(event.data);
    };
    socket.onclose = function (event) {
      if (event.wasClean) {
        _printLog(
          `[close] Connection closed cleanly, code=${event.code} reason=${event.reason}`
        );
      } else {
        // 例如服务器进程被杀死或网络中断
        // 在这种情况下，event.code 通常为 1006
        _printLog("Webridge: [close] Connection died");
      }
    };
    socket.onerror = function (error) {
      _printLog(`Webridge: [error] ${error.reason}`);
    };
  }

  function _createBridgeEvent() {
    readyEvent = new CustomEvent("BridgeReady");
    _printLog("Webridge: _createBridgeEvent");
    readyEvent.bridge = Webridge;
  }

  function _init(address, port, token) {
    _printLog(
      `Webridge: '_init: address: ${address} , p: ${port} , t: ${token}`
    );
    if (Webridge._token) {
      throw new Error("Webridge.init called twice");
    }

    Webridge._token = token;

    _createBridgeChannel(address, port);
    _createBridgeEvent();

    _send(
      _createMessageJson("state", {
        code: 1,
      })
    );

    Webridge.inited = true;
    _printLog("Webridge: _init done!");
  }

  function _handleMessage(messageJson) {
    _printLog("Webridge: _handleMessage: ", messageJson);
    let message = JSON.parse(messageJson);

    let bridgeCate = message.cate;
    var callbackId = message.callback_id;
    switch (bridgeCate) {
      case "native_response":
        if (callbackId) {
          let containerJson = message.container;
          let responseCallback = responseCallbacks[callbackId];
          responseCallback(containerJson);
          delete responseCallbacks[callbackId];
        }
        break;
      case "native_send_to_js":
        let containerJson = message.container;
        let callJsName = message.call_js_name;
        var callback;
        if (callJsName) {
          callback = _receivers[callJsName];
        } else {
          if (Webridge._defaultReceiver) {
            callback = Webridge._defaultReceiver;
          }
        }
        if (callback) {
          var responseCallback;
          if (callbackId) {
            responseCallback = function (responseData) {
              _send(
                _createMessageJson("js_call_native", {
                  native_call: callbackId,
                  data: responseData,
                })
              );
            };
          }
          try {
            callback(containerJson, responseCallback);
          } catch (exception) {
            _printLog("Webridge: receiver threw.", message, exception);
          }
        } else {
          _printLog("Webridge: not found receiver.");
        }
        break;
      default:
        _printLog("Webridge: not support message!");
        break;
    }
  }

  function _sendToNative(data, callback) {
    _printLog("Webridge: _sendToNative");
    let callbackId;
    if (callback) {
      callbackId = "js_id_" + _uuid();
      responseCallbacks[callbackId] = callback;
    }
    _send(
      _createMessageJson("js_send_to_native", {
        callback_id: callbackId,
        data: data,
      })
    );
  }

  function _callNative(nativeCallName, data, callback) {
    _printLog("Webridge: _callNative");
    // 如果方法不需要参数，只有回调函数，简化JS中的调用
    if (arguments.length == 2 && typeof data == "function") {
      callback = data;
      data = null;
    }
    var callbackId;
    if (callback) {
      callbackId = "js_cb_" + _uuid();
      responseCallbacks[callbackId] = callback;
    }
    _send(
      _createMessageJson("js_call_native", {
        callback_id: callbackId,
        native_call: nativeCallName,
        data: data,
      })
    );
  }

  function _registerDefaultReceiver(receiver) {
    Webridge._defaultReceiver = receiver;
  }

  function _registerReceiver(receiverName, receiver) {
    _receivers[receiverName] = receiver;
  }

  function _uuid() {
    var s = [];
    const hexDigits = [
      "0",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "A",
      "B",
      "C",
      "D",
      "E",
      "F",
      "G",
      "H",
      "I",
      "J",
      "K",
      "L",
      "M",
      "N",
      "O",
      "P",
      "Q",
      "R",
      "S",
      "T",
      "U",
      "V",
      "W",
      "X",
      "Y",
      "Z",
    ];
    for (var i = 0; i < 36; i++) {
      if (i == 8 || i == 13 || i == 18 || i == 23) {
        s[i] = "-";
      } else {
        s[i] = hexDigits[Math.floor(Math.random() * 0x23)];
      }
    }

    return s.join("");
  }

  Webridge.init = _init;
  Webridge.sendToNative = _sendToNative;
  Webridge.callNative = _callNative;
  Webridge.registerDefaultReceiver = _registerDefaultReceiver;
  Webridge.registerReceiver = _registerReceiver;
})();
