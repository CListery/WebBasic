package io.clistery.webbasic.demo

import io.clistery.webbasic.ext.capture
import io.clistery.webbasic.ext.eval
import io.clistery.webbasic.x5.X5BridgeWebView

fun X5BridgeWebView.testCapture() {
    val clean = "try{ document.getElementById('shower-img').remove(); }catch (e){}"
    eval(clean)
    capture(elementQuery = "body", option = "{allowTaint: true, useCORS: true}") {
        if (!it.isNullOrEmpty()) {
            eval(
                """
                let body = document.body;
                let divElement = document.createElement('div');
                divElement.id = 'shower-img';
                divElement.style = 'background: rgba(0, 0, 0, 0.5); position: fixed; top: 0px; left: 0px; z-index: 99999998; width: 100%; height: 100%;';
                
                let imgCloseElement = document.createElement('img');
                divElement.appendChild(imgCloseElement);
                imgCloseElement.src = 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjZmZmZmZmIiBoZWlnaHQ9IjI0IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICAgIDxwYXRoIGQ9Ik0xOSA2LjQxTDE3LjU5IDUgMTIgMTAuNTkgNi40MSA1IDUgNi40MSAxMC41OSAxMiA1IDE3LjU5IDYuNDEgMTkgMTIgMTMuNDEgMTcuNTkgMTkgMTkgMTcuNTkgMTMuNDEgMTJ6Ii8+CiAgICA8cGF0aCBkPSJNMCAwaDI0djI0SDB6IiBmaWxsPSJub25lIi8+Cjwvc3ZnPgo=';
                imgCloseElement.style = 'position: absolute; right: 20px; top: 20px; cursor: pointer;';
                imgCloseElement.id = 'shower-close';
                
                let imgElement = document.createElement('img');
                divElement.appendChild(imgElement);
                imgElement.src = '$it';
                imgElement.style = 'height: 100%; width: auto; position: fixed; top: 0px; left: 0px; opacity: 1; transform: scale(0.8); z-index: 99999999; transition: transform 0.3s cubic-bezier(0.42, 0, 0.58, 1) 0s, opacity 0.3s cubic-bezier(0.42, 0, 0.58, 1) 0s, -webkit-transform 0.3s cubic-bezier(0.42, 0, 0.58, 1) 0s;';
                body.appendChild(divElement);
                
                setTimeout(function() {
                    document.getElementById('shower-close').addEventListener('click', function() {
                        $clean
                    });
                }, 0);
                """
            )
        }
    }
}