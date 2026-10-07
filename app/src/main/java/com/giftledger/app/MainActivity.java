package com.giftledger.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 앱 화면은 assets/index.html 하나로 되어 있고, 이 Activity는 그것을 띄우는 껍데기입니다.
 * 데이터는 WebView의 localStorage에 저장되며 앱 데이터 폴더 안에만 남습니다(기기 로컬).
 */
public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    // 고정 출처(origin)를 줘야 localStorage가 앱을 다시 켜도 유지됩니다.
    private static final String BASE_URL = "https://giftledger.local/";

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#F5F4F0"));

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setTextZoom(100);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true; // 외부 페이지로 이동하지 않음
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel"});
                startActivityForResult(Intent.createChooser(i, "CSV 파일 선택"), PICK_FILE);
                return true;
            }
        });
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.loadDataWithBaseURL(BASE_URL, readAsset("index.html"), "text/html", "utf-8", null);
    }

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "<p>화면 파일을 불러오지 못했어요: " + e.getMessage() + "</p>";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE && fileCallback != null) {
            Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null)
                    ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("window.handleBack ? window.handleBack() : false", value -> {
            if (!"true".equals(value)) MainActivity.super.onBackPressed();
        });
    }

    /** JS에서 window.Android.saveFile(이름, 내용)으로 호출: 다운로드 폴더에 CSV 저장 */
    private class Bridge {
        @JavascriptInterface
        public String saveFile(String name, String content) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return "저장하지 못했어요";
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                }
                return "다운로드 폴더에 " + name + " 저장";
            } catch (Exception e) {
                return "저장 실패: " + e.getMessage();
            }
        }
    }
}
