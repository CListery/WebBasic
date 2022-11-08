package io.clistery.webbasic.demo.feature;

import static io.clistery.webbasic.demo.utils.PermissionUtil.REQUEST_EXTERNAL_STORAGE;
import static io.clistery.webbasic.demo.utils.PermissionUtil.REQUEST_GEOLOCATION;
import static io.clistery.webbasic.demo.utils.PermissionUtil.verifyLocationPermissions;
import static io.clistery.webbasic.demo.utils.PermissionUtil.verifyStoragePermissions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import io.clistery.webbasic.demo.R;
import io.clistery.webbasic.demo.utils.WebViewJavaScriptFunction;

/**
 * 纯Android系统 android.webkit.WebView 基础WebViewActivity
 *
 * 作用：
 * 在对比（X5内核、SDK系统WebView）与 android.webkit.WebView 差异时，
 * 可通过继承该 Activity ，在initWebView时对相应功能进行开发
 */
public class WebKitWebViewActivity extends AppCompatActivity {
    private final String TAG = "WebKitWebViewActivity";
    private ImageButton mBackBtn;
    private ImageButton mForwardBtn;

    protected android.webkit.WebView mWebView;
    private static final int DISABLE_ALPHA = 120;
    private static final int ENABLE_ALPHA = 255;
    private static final int FILE_CHOOSER_REQUEST = 100;

    private long mClickBackTime = 0;
    private android.webkit.ValueCallback<Uri[]> mFilePathCallback;

    private Callback mGeolocationCallback;
    private String locationPermissionUrl;

    private static final String mHomeUrl = "file:///android_asset/webpage/homePage.html";

    private EditText mUrlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_webview);
        initWebView();
        initButtons();
    }

    /**
     * 自定义初始化WebView设置，此处为默认 WebKitWebViewActivity 初始化
     * 可通过继承该 Activity Override 该方法做自己的实现
     */
    protected void initWebView() {

        Context context = this;

        mWebView = new android.webkit.WebView(context);
        ViewGroup mContainer = findViewById(R.id.webViewContainer);
        mContainer.addView(mWebView);

        android.webkit.WebSettings webSetting = mWebView.getSettings();
        webSetting.setJavaScriptEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setSupportZoom(true);
        webSetting.setDatabaseEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setDomStorageEnabled(true);

        initWebViewClient();
        initWebChromeClient();

        mWebView.loadUrl(mHomeUrl);
    }

    private void initWebViewClient() {
        mWebView.setWebViewClient(new android.webkit.WebViewClient() {

            @Override
            public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
                Log.i(TAG, "onPageStarted, view:" + view + ", url:" + url);
                mUrlEditText.setText(url);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                Log.i(TAG, "onPageFinished, view:" + view + ", url:" + url);
                changGoForwardButton(view);
            }
        });
    }

    private void initWebChromeClient() {
        final Context context = this;
        final Activity activity = this;
        mWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
            /**
             * 具体接口使用细节请参考文档：
             * https://x5.tencent.com/docs/webview.html
             * 或 Android WebKit 官方：
             * https://developer.android.com/reference/android/webkit/WebChromeClient
             */

            @Override
            public void onProgressChanged(android.webkit.WebView view, int newProgress) {
                Log.i(TAG, "onProgressChanged, newProgress:" + newProgress + ", view:" + view);
                changGoForwardButton(view);
            }

            @Override
            public boolean onJsAlert(android.webkit.WebView webView, String url, String message, android.webkit.JsResult result) {
                new AlertDialog.Builder(context).setTitle("JS弹窗Override")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialogInterface, i) -> result.confirm())
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(android.webkit.WebView webView, String url, String message, android.webkit.JsResult result) {
                new AlertDialog.Builder(context).setTitle("JS弹窗Override")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialogInterface, i) -> result.confirm())
                        .setNegativeButton("Cancel", (dialogInterface, i) -> result.cancel())
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsBeforeUnload(android.webkit.WebView webView, String url, String message, android.webkit.JsResult result) {
                new AlertDialog.Builder(context).setTitle("页面即将跳转")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialogInterface, i) -> result.confirm())
                        .setNegativeButton("Cancel", (dialogInterface, i) -> result.cancel())
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsPrompt(android.webkit.WebView webView, String url, String message, String defaultValue, android.webkit.JsPromptResult result) {
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                new AlertDialog.Builder(context).setTitle("JS弹窗Override")
                        .setMessage(message)
                        .setView(input)
                        .setPositiveButton("OK", (dialogInterface, i) -> result.confirm(input.getText().toString()))
                        .setCancelable(false)
                        .show();
                return true;
            }

            /**
             * Return value usage see FILE_CHOOSE_REQUEST in
             * {@link BaseWebViewActivity#onActivityResult(int, int, Intent)}
             */
            @Override
            public boolean onShowFileChooser(android.webkit.WebView webView,
                    android.webkit.ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                Log.i(TAG, "openFileChooser: " + fileChooserParams.getMode());
                mFilePathCallback = filePathCallback;
                openFileChooseProcess(fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);

                if (verifyLocationPermissions(activity)) {
                    callback.invoke(origin, true, false);
                } else {
                    locationPermissionUrl = origin;
                    mGeolocationCallback = callback;
                }
            }

        });
    }

    private void initJavaScriptInterface() {
        final Activity context = this;
        mWebView.addJavascriptInterface(new WebViewJavaScriptFunction() {
            @Override
            public void onJsFunctionCalled(String tag) {

            }

            @JavascriptInterface
            public void openQRCodeScan() {
                new IntentIntegrator(context).initiateScan();
            }

            @JavascriptInterface
            public void openDebugX5() {
                mWebView.loadUrl("http://debugx5.qq.com");
            }


        }, "Android");
    }

    /* Don't care about the Base UI Logic below ^_^ */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            initWebView();
        }

        if (mGeolocationCallback != null && requestCode == REQUEST_GEOLOCATION) {
            boolean allow = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            mGeolocationCallback.invoke(locationPermissionUrl, allow,false);
            mGeolocationCallback = null;
            locationPermissionUrl = "";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (mFilePathCallback != null) {
                    if(data != null && data.getClipData() != null) {
                        //有选择多个文件
                        int count = data.getClipData().getItemCount();
                        Log.i(TAG, "url count ：  " + count);
                        Uri[] uris = new Uri[count];
                        int currentItem = 0;
                        while(currentItem < count) {
                            Uri fileUri = data.getClipData().getItemAt(currentItem).getUri();
                            uris[currentItem] = fileUri;
                            currentItem = currentItem + 1;
                        }
                        mFilePathCallback.onReceiveValue(uris);
                    } else {
                        Uri result = data == null ? null : data.getData();
                        Log.e(TAG, "" + result);
                        mFilePathCallback.onReceiveValue(new Uri[]{result});
                    }
                    mFilePathCallback = null;
                }
            }
        }
        else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "扫描结果为空", Toast.LENGTH_SHORT).show();
                } else {
                    String str = result.getContents();
                    if (mWebView != null) {
                        mWebView.loadUrl(str);
                    }
                }
            }
        }
    }

    private void openFileChooseProcess(boolean isMulti) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("*/*");
        if (isMulti) {
            Log.e(TAG, "putExtra");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(Intent.createChooser(intent, "FileChooser"), FILE_CHOOSER_REQUEST);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
                changGoForwardButton(mWebView);
                return true;
            }
            long currentTime = System.currentTimeMillis();
            // 3秒内连按两次后退按钮，退出应用
            if (currentTime - mClickBackTime < 3000) {
                android.os.Process.killProcess(android.os.Process.myPid());
            } else {
                Toast.makeText(getApplicationContext(), "再按一次返回键退出", Toast.LENGTH_SHORT).show();
                mClickBackTime = currentTime;
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        // 获取布局文件
        popupMenu.getMenuInflater().inflate(R.menu.website_menu, popupMenu.getMenu());
        popupMenu.show();
        final Activity fContext = this;
        // 通过上面这几行代码，就可以把控件显示出来了
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 控件每一个item的点击事件
                String url = "";
                switch (item.getItemId()) {
                    case R.id.debugtbs:
                        url = "http://debugtbs.qq.com";
                        break;
                    case R.id.debugx5:
                        url = "http://debugx5.qq.com";
                        break;
                    case R.id.qrcode:
                        new IntentIntegrator(fContext).initiateScan();
                        break;
                    default:
                        url = mHomeUrl;
                        break;
                }
                if (mWebView != null && !"".equals(url)) {
                    mWebView.loadUrl(url);
                }
                return true;
            }
        });
    }

    private void initButtons() {
        final Context context = this.getApplicationContext();
        mBackBtn = findViewById(R.id.btn_back);
        mBackBtn.setImageAlpha(DISABLE_ALPHA);
        mBackBtn.setEnabled(false);
        mBackBtn.setOnClickListener(view -> {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
            }
        });

        mForwardBtn = findViewById(R.id.btn_forward);
        mForwardBtn.setEnabled(false);
        mForwardBtn.setImageAlpha(DISABLE_ALPHA);
        mForwardBtn.setOnClickListener(view -> {
            if (mWebView != null && mWebView.canGoForward()) {
                mWebView.goForward();
            }
        });

        findViewById(R.id.btn_more).setOnClickListener(this::showPopupMenu);
        findViewById(R.id.btn_reload).setOnClickListener(view -> {
            if (mWebView != null) {
                mWebView.reload();
            }
        });
        findViewById(R.id.btn_exit).setOnClickListener(view -> finish());

        mUrlEditText = findViewById(R.id.urlEdit);
        mUrlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String url = mUrlEditText.getEditableText().toString();
                if (!url.contains("://") && !url.startsWith("javascript:")) {
                    url = "https://" + url;
                }
                mWebView.loadUrl(url);
                if (mUrlEditText != null) {
                    mUrlEditText.clearFocus();
                }
            }
            return true;
        });
        mUrlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        findViewById(R.id.urlLoad).setOnClickListener(v->{
            if (mWebView != null) {
                String url = mUrlEditText.getEditableText().toString();
                if (!url.contains("://") && !url.startsWith("javascript:")) {
                    url = "https://" + url;
                }
                mWebView.loadUrl(url);
            }
            if (mUrlEditText != null) {
                mUrlEditText.clearFocus();
            }
        });
    }

    private void changGoForwardButton(android.webkit.WebView view) {
        try {

            if (view.canGoBack()) {
                mBackBtn.setImageAlpha(ENABLE_ALPHA);
                mBackBtn.setEnabled(true);
            } else {
                mBackBtn.setImageAlpha(DISABLE_ALPHA);
                mBackBtn.setEnabled(false);
            }
            if (view.canGoForward()) {
                mForwardBtn.setImageAlpha(ENABLE_ALPHA);
                mForwardBtn.setEnabled(true);
            } else {
                mForwardBtn.setImageAlpha(DISABLE_ALPHA);
                mForwardBtn.setEnabled(false);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Exception: " + t);
        }
    }

}
