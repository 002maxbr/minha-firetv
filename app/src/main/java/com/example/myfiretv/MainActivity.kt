package com.example.myfiretv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(0xFF0B0F1A.toInt())
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Launcher")
        web.webViewClient = WebViewClient()
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
