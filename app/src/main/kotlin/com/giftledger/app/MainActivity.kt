package com.giftledger.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * 앱 화면은 assets/index.html 하나로 되어 있고, 이 Activity는 그것을 띄우는 껍데기입니다.
 * 데이터는 WebView의 localStorage에 저장되며 앱 데이터 폴더 안에만 남습니다(기기 로컬).
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bg = Color.parseColor("#F5F4F0")
        window.statusBarColor = bg
        window.navigationBarColor = Color.WHITE

        // Android 15(targetSdk 35)부터는 화면이 상태바·내비게이션 바 아래까지 그려집니다.
        // 시스템 바(와 키보드) 높이만큼 안쪽 여백을 줘서 앱 화면이 가려지지 않게 합니다.
        webView = WebView(this)
        val root = FrameLayout(this).apply {
            setBackgroundColor(bg)
            addView(
                webView,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )
            setOnApplyWindowInsetsListener { v, insets -> v.applySystemBarPadding(insets); insets }
        }
        setContentView(root)
        // DecorView는 setContentView 이후에 생기므로, 시스템 바 아이콘 설정은 반드시 이 다음에 호출
        useDarkSystemBarIcons()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            textZoom = 100
        }
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true // 외부 페이지로 이동하지 않음
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel")
                    )
                }
                @Suppress("DEPRECATION")
                startActivityForResult(Intent.createChooser(intent, "CSV 파일 선택"), PICK_FILE)
                return true
            }
        }
        webView.addJavascriptInterface(Bridge(), "Android")
        webView.loadDataWithBaseURL(BASE_URL, readAsset("index.html"), "text/html", "utf-8", null)
    }

    @Suppress("DEPRECATION")
    private fun View.applySystemBarPadding(insets: WindowInsets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
        } else {
            setPadding(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom
            )
        }
    }

    /** 밝은 배경 위에서 상태바·내비게이션 바 아이콘이 보이도록 어두운 아이콘 사용 */
    @Suppress("DEPRECATION")
    private fun useDarkSystemBarIcons() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(flags, flags)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    private fun readAsset(name: String): String =
        runCatching { assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() } }
            .getOrElse { "<p>화면 파일을 불러오지 못했어요: ${it.message}</p>" }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_FILE && fileCallback != null) {
            val uri = data?.data
            fileCallback?.onReceiveValue(if (resultCode == RESULT_OK && uri != null) arrayOf(uri) else null)
            fileCallback = null
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        webView.evaluateJavascript("window.handleBack ? window.handleBack() : false") { value ->
            if (value != "true") super.onBackPressed()
        }
    }

    /** JS에서 window.Android.saveFile(이름, 내용)으로 호출: 다운로드 폴더에 CSV 저장 */
    private inner class Bridge {
        @JavascriptInterface
        fun saveFile(name: String, content: String): String = try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                "저장하지 못했어요"
            } else {
                contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                "다운로드 폴더에 $name 저장"
            }
        } catch (e: Exception) {
            "저장 실패: ${e.message}"
        }
    }

    companion object {
        private const val PICK_FILE = 1001
        // 고정 출처(origin)를 줘야 localStorage가 앱을 다시 켜도 유지됩니다.
        private const val BASE_URL = "https://giftledger.local/"
    }
}
