package com.archer.s00paperxrawler.strategy

import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.service.DownloadService
import com.archer.s00paperxrawler.service.okClient
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.File
import java.io.InterruptedIOException

private const val TAG = "StrategyFor500"

/**
 * Created by Chen Xin on 2018/8/21.
 * 500px图片爬取策略
 */
class StrategyFor500 : ICrawlStrategy {
    object Singleton {
        val instance = StrategyFor500()
    }

    override fun evaluateJSBeforeParse(): Observable<String> =
            Observable.create<String> {
                val input = MyApp.AppCtx.assets.open("dom_listener.js")
                it.onNext(input.bufferedReader().use(BufferedReader::readText))
                it.onComplete()
            }.subscribeOn(Schedulers.io())

    override fun parseHTML(): Function<File, Observable<String>> = Function {
        val parse = Jsoup.parse(it, "utf-8", prefs().baseUri)
        val elements = parse.select("div.photo_thumbnail")
        if (elements.isEmpty()) {
            Log.i(TAG, "parseHTML: elements.isEmpty()")
            Observable.just("")
        } else {
            Log.w(TAG, "parseHTML: elements.is Not Empty()")
            val urls = arrayOfNulls<String>(elements.size)
            for ((index, item) in elements.withIndex()) {
                urls[index] = item.getElementsByClass("photo_link ").attr("href")
            }
            Observable.fromArray(*urls)
        }
    }

    override fun handleResult(): Function<String, Any> = Function {
        Log.d(TAG, "handleResult() called $it")
        if (!TextUtils.isEmpty(it)) {
            val builder = Request.Builder()
            val request = builder.url("${prefs().baseUri}$it").build()
            val html: String?
            try {
                val response = okClient.newCall(request).execute()
                html = response.body()?.string()
                if (!TextUtils.isEmpty(html)) {
                    getInfoFromDetailHtml(it, html!!)
                } else {
                    Log.e(TAG, "handleResult: html is empty")
                }
            } catch (e: InterruptedIOException) {
            }
        } else {
            Log.e(TAG, "handleResult: handleResult is empty")
        }
    }


    private fun getInfoFromDetailHtml(detailUrl: String, html: String) {
        val index = "window.PxPreloadedData = "
        val start = html.indexOf(index)
        if (start == -1) return
        val json = html.indexOf(";", start).let { html.substring(start + index.length, it) }
        Log.d(TAG, "getInfoFromDetailHtml: $json")
        try {
            JSONObject(json).run { optJSONObject("photo") }?.apply {
                val id = optInt("id", -1)
                val name = optString("name")
                val url = optJSONArray("images")?.let {
                    val last = it.getJSONObject(it.length() - 1)
                    if (last.getInt("size") == 2048) return@let last.optString("url", null) ?: last.getString("https_url")
                    var maxSize = 0
                    var url = ""
                    for (i in 0..it.length()) {
                        val obj = it.getJSONObject(i)
                        val size = obj.getInt("size")
                        if (size > maxSize) {
                            maxSize = size
                            url = obj.optString("url", null) ?: obj.getString("https_url")
                        }
                    }
                    return@let url
                }.let {
                    if (!TextUtils.isEmpty(it)) return@let it!!.replace("\\", "")
                    return@let ""
                }
                val ph = optJSONObject("user")?.run {
                    var ph = "${optString("firstname")} ${optString("lastname")}"
                    if (TextUtils.isEmpty(ph.trim())) ph = optString("username")
                    return@run ph
                }.let {
                    if (!TextUtils.isEmpty(it)) return@let it!!
                    return@let ""
                }
                val w = optInt("width", -1)
                val h = optInt("height", -1)
                val aspect = if (w > 0 && h > 0) w.toFloat() / h else -1F
                ResolverHelper.INSTANCE.addPhotoInfo(detailUrl,id, name, url, ph, aspect)
                DownloadService.startPhotosDownload()
            }
        } catch (e: JSONException) {
        }
    }

    override fun evaluateJSBeforeDestroy(webView: WebView) {
        Log.i(TAG, "evaluateJSBeforeDestroy() called with: webView = [ $webView ]")
        webView.evaluateJavascript("observer != null") { value ->
            Log.d(TAG, "observer check called $value")
            if (java.lang.Boolean.parseBoolean(value)) {
                webView.evaluateJavascript("" +
                        "observer.disconnect()\n" +
                        "observer = null",
                        null)
            }
        }
    }

}