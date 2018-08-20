package com.archer.s00paperxrawler.db

import android.content.ContentValues
import android.text.TextUtils
import android.util.Log
import com.archer.s00paperxrawler.MyApp

private const val TAG = "ResolverHelper"
/**
 * Created by Chen Xin on 2018/8/16.
 * ContentResolver Helper
 */
enum class ResolverHelper {
    INSTANCE;

    fun addPhotoDetail(url: String) {
        Log.d(TAG, "addPhotoDetail() called with: url = [ $url ]")
        val uri = PaperInfoContract.PHOTO_DETAIL_CONTENT_URI
        MyApp.AppCtx.contentResolver.insert(uri, ContentValues().apply { put(PaperInfoContract.Columns.PHOTO_DETAIL_URL, url) })
    }
}