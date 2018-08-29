package com.archer.s00paperxrawler.js

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.archer.s00paperxrawler.strategy.getCrawlStrategy
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

private const val TAG = "JsCallback"

/**
 * Created by Chen Xin on 2018/8/15.
 * Js回调类
 */
enum class JsCallback : JsInterface {
    INSTANCE;

    private val map = mutableMapOf<String, Disposable>()

    override fun add(webView: WebView) {
        webView.addJavascriptInterface(this, TAG)
    }

    override fun remove(webView: WebView) {
        for (entry in map) {
            if (!entry.value.isDisposed) entry.value.dispose()
        }
        map.clear()
        webView.removeJavascriptInterface(TAG)
    }

    @JavascriptInterface
    fun addPhotoInfo(url: String) {
        Observable.just(url)
                .observeOn(Schedulers.io())
                .map(getCrawlStrategy().handleResult()).doOnSubscribe {
                    Log.i(TAG, "addPhotoInfo onSubscribe() called")
                    map[url] = it
                }.doOnComplete {
                    Log.d(TAG, "addPhotoInfo onComplete() called")
                    map.remove(url)
                }.subscribe()
    }

}