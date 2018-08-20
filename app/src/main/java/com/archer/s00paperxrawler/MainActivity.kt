package com.archer.s00paperxrawler

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.JsCallback
import com.archer.s00paperxrawler.utils.JsLog
import com.archer.s00paperxrawler.utils.Pref
import com.archer.s00paperxrawler.utils.getLoadUri
import com.github.satoshun.reactivex.webkit.data.OnPageFinished
import com.github.satoshun.reactivex.webkit.events
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
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
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.blockNetworkImage = true
//        setContentView(webView)
        setContentView(R.layout.activity_main)
        webView.addJavascriptInterface(JsLog.INSTANCE, "Log")
        webView.addJavascriptInterface(JsCallback.INSTANCE, "JsCallback")
        Observable.create<String> { emitter: ObservableEmitter<String> ->
            webView.events().ofType(OnPageFinished::class.java).subscribe {
                Log.d(TAG, "onPageFinished() called with: Thread = [ ${Thread.currentThread()} ], url = [ ${it.url} ]")
                webView.evaluateJavascript("document.children[0].innerHTML") { innerHTML ->
                    if (TextUtils.isEmpty(innerHTML)) {
                        emitter.onError(Exception("WebView not loaded anything"))
                    } else {
                        emitter.onNext(innerHTML)
                        emitter.onComplete()
                    }
                }
            }
            webView.loadUrl(getLoadUri())
        }.subscribeOn(AndroidSchedulers.mainThread()).zipWith(Observable.create<String> {
            val input = assets.open("dom_listener.js")
            it.onNext(input.bufferedReader().use(BufferedReader::readText))
            it.onComplete()
        }.subscribeOn(Schedulers.io()), BiFunction<String, String, String> { innerHTML, script ->
            if (Looper.getMainLooper().thread == Thread.currentThread()) webView.evaluateJavascript(script, null)
            else runOnUiThread { webView.evaluateJavascript(script, null) }
            return@BiFunction innerHTML
        }).observeOn(Schedulers.io()).map {
            //使用Properties类来将转义后的字符恢复回来
            val properties = Properties()
            properties.load(StringReader("key=$it"))
            val ret = properties.getProperty("key")
            val file = File(externalCacheDir, "ret.html")
            file.deleteOnExit()
            file.writeText(ret)
            return@map file
        }.observeOn(Schedulers.computation()).flatMap { file: File ->
            val parse = Jsoup.parse(file, "utf-8", Pref().baseUri)
            val elements = parse.select("div.photo_thumbnail")
            if (elements.isEmpty()) {
                Log.i(TAG, "onCreate: elements.isEmpty()")
                return@flatMap Observable.just("")
            } else {
                Log.w(TAG, "onCreate: elements.is Not Empty()")
                val urls = arrayOfNulls<String>(elements.size)
                for ((index, item) in elements.withIndex()) {
                    urls[index] = item.getElementsByClass("photo_link ").attr("href")
                }
                return@flatMap Observable.fromArray(*urls)
            }
        }.map {
            ResolverHelper.INSTANCE.addPhotoDetail(it)
        }.takeLast(1).observeOn(AndroidSchedulers.mainThread()).subscribe {
            Log.d(TAG, "onNext() called ")
            webView.evaluateJavascript("observer != null") { value ->
                Log.d(TAG, "observer check called $value")
            }
        }
    }
}
