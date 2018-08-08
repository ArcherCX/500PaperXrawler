package com.archer.s00paperxrawler

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)
//        setContentView(R.layout.activity_main)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.blockNetworkImage = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "onPageFinished() called with: view = [ $view ], url = [ $url ]")
                val file = File(externalCacheDir, "ret.html")
                file.deleteOnExit()
                view!!.evaluateJavascript("document.children[0].innerHTML") {
                    //利用Properties进行字符串的转义字符恢复
                    val properties = Properties()
                    properties.load(StringReader("key=$it"))
                    val ret = properties.getProperty("key")
                    file.writeText(ret)
                    Log.d(TAG, "${Thread.currentThread()} , evaluateJavascript: $ret")
                    val baseUri = "https://500px.com/"
                    val parse = Jsoup.parse(file, "utf-8", baseUri)
                    val thumb = parse.selectFirst("div.photo_thumbnail")
                    if (thumb == null) {
                        val isJs = assets.open("dom_listener.js")
                        val content = isJs.bufferedReader().use(BufferedReader::readText)
                        view.evaluateJavascript(content) {}
                    } else {
                        val value = thumb.getElementsByClass("photo_link ").attr("href")

                        Log.w(TAG, "get photo link : $value")
                    }
                }
            }

        }
        webView.loadUrl("https://500px.com/popular/people-uncategorized")
    }
}
