package com.archer.s00paperxrawler.utils

import android.util.Log
import android.webkit.JavascriptInterface

private const val TAG = "JsLog"

enum class JsLog {
    INSTANCE;

    @JavascriptInterface
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    @JavascriptInterface
    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    @JavascriptInterface
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    @JavascriptInterface
    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }


    @JavascriptInterface
    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    @JavascriptInterface
    fun e(msg: String) {
        Log.e(TAG, msg)
    }

    @JavascriptInterface
    fun i(msg: String) {
        Log.i(TAG, msg)
    }

    @JavascriptInterface
    fun w(msg: String) {
        Log.w(TAG, msg)
    }
}
