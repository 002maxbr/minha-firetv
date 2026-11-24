package com.example.myfiretv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var web: WebView
    private val handler = Handler(Looper.getMainLooper())

    private fun fitStage() {
        val w = web.width
        val h = web.height
        if (w == 0 || h == 0) return
        val s = minOf(w / 1920f, h / 1080f)
        val left = (w - 1920 * s) / 2f
        val top = (h - 1080 * s) / 2f
        web.evaluateJavascript(
            "(function(){var e=document.getElementById('stage');if(e){e.style.transform='scale($s)';e.style.left='${left}px';e.style.top='${top}px';}})()",
            null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(0xFF0B0F1A.toInt())
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.useWideViewPort = false
        web.settings.loadWithOverviewMode = false
        web.setInitialScale(100)
        web.addJavascriptInterface(Bridge(), "Launcher")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                for (d in longArrayOf(0, 300, 1000, 3000)) {
                    handler.postDelayed({ fitStage() }, d)
                }
            }
        }
        web.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitStage() }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onResume() {
        super.onResume()
        web.requestFocus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        web.evaluateJavascript("location.hash='#home'", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript(
            "if(location.hash&&location.hash!=='#home')location.hash='#home'", null
        )
    }

    inner class Bridge {
        @JavascriptInterface
        fun apps(): String {
            val pm = packageManager
            val seen = HashSet<String>()
            val arr = JSONArray()
            for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                val i = Intent(Intent.ACTION_MAIN).addCategory(cat)
                for (r in pm.queryIntentActivities(i, 0)) {
                    val p = r.activityInfo.packageName
                    if (p == packageName || !seen.add(p)) continue
                    arr.put(JSONObject().put("pkg", p).put("label", r.loadLabel(pm).toString()))
                }
            }
            return arr.toString()
        }

        @JavascriptInterface
        fun open(pkg: String) {
            val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                ?: packageManager.getLaunchIntentForPackage(pkg)
            intent?.let { startActivity(it) }
        }
    }
}
