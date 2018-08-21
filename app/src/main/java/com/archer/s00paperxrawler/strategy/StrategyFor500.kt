package com.archer.s00paperxrawler.strategy

import android.util.Log
import android.webkit.WebView
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.Pref
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.File

private const val TAG = "StrategyFor500"

/**
 * Created by Chen Xin on 2018/8/21.
 * 500px图片爬取策略
 */
class StrategyFor500 : ICrawlStrategy {
    override fun evaluateJSBeforeParse(): Observable<String> =
            Observable.create<String> {
                val input = MyApp.AppCtx.assets.open("dom_listener.js")
                it.onNext(input.bufferedReader().use(BufferedReader::readText))
                it.onComplete()
            }.subscribeOn(Schedulers.io())

    override fun parseHTML(): Function<File, Observable<String>> = Function {
        val parse = Jsoup.parse(it, "utf-8", Pref().baseUri)
        val elements = parse.select("div.photo_thumbnail")
        if (elements.isEmpty()) {
            Log.i(TAG, "onCreate: elements.isEmpty()")
            Observable.just("")
        } else {
            Log.w(TAG, "onCreate: elements.is Not Empty()")
            val urls = arrayOfNulls<String>(elements.size)
            for ((index, item) in elements.withIndex()) {
                urls[index] = item.getElementsByClass("photo_link ").attr("href")
            }
            Observable.fromArray(*urls)
        }
    }

    override fun handleResult(): Function<String, Any> = Function {
        ResolverHelper.INSTANCE.addPhotoDetail(it)
    }

    override fun evaluateJSBeforeDestroy(webView: WebView) {
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