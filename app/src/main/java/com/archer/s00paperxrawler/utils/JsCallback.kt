package com.archer.s00paperxrawler.utils

import android.content.ContentValues
import android.util.Log
import android.webkit.JavascriptInterface
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper

private const val TAG = "JsCallback"
/**
 * Created by Chen Xin on 2018/8/15.
 * Js回调类
 */
enum class JsCallback {
    INSTANCE;

    @JavascriptInterface
    fun addPhotoDetail(url: String) {
        ResolverHelper.INSTANCE.addPhotoDetail(url)
    }
}