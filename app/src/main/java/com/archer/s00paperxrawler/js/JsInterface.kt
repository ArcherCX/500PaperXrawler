package com.archer.s00paperxrawler.js

import android.webkit.WebView

/**
 * Created by Chen Xin on 2018/8/24.
 * WebView添加的JavascriptInterface接口
 */
interface JsInterface {
    /**添加到WebView*/
    fun add(webView: WebView)

    /**从WebView删除*/
    fun remove(webView: WebView)
}