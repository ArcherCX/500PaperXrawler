package com.archer.s00paperxrawler.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.service.DownloadService
import java.io.File

fun prefs(): Prefs {
    return Prefs.INSTANCE
}

/**
 * Created by Chen Xin on 2018/8/10.
 * SharedPreferences 操作类
 */
enum class Prefs {
    INSTANCE;

    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyApp.AppCtx)

    var baseUri: String
        get() = pref.getString("base_uri", "https://500px.com")
        set(value) = pref.edit().putString("base_uri", value).apply()

    var baseApiUri: String
        get() = pref.getString("base_uri", "https://api.500px.com/v1/photos?")
        set(value) = pref.edit().putString("base_uri", value).apply()

    var feature: String
        get() = pref.getString("feature", "popular")
        set(value) = pref.edit().putString("feature", value).apply()

    var categories: String
        get() = pref.getString("categories", "/")
        set(value) = pref.edit().putString("categories", value).apply()

    var minCacheSize: Int
        get() = pref.getInt("min_cache_size", 1)
        set(value) = pref.edit().putInt("min_cache_size", value).apply()

    var maxCacheSize: Int
        get() = pref.getInt("max_cache_size", 3)
        set(value) = pref.edit().putInt("max_cache_size", value).apply()

    var photosCachePath: String
        get() {
            var path = pref.getString("default_cache_path", "")
            if (TextUtils.isEmpty(path)) {
                path = File(MyApp.AppCtx.cacheDir, "photos").apply { if (!exists()) mkdirs() }.absolutePath
                pref.edit().putString("default_cache_path", path).apply()
            }
            return path
        }
        set(_) {}

    var isCacheEnough: Boolean
        get() = pref.getBoolean("is_cache_enough", false)
        set(value) {
            pref.edit().putBoolean("is_cache_enough", value).apply()
            if (!value) DownloadService.startPhotosDownload()
        }

    var csrfToken: String
        get() = pref.getString("csrf_token", "")
        set(value) = pref.edit().putString("csrf_token", value).apply()

    var updateInterval: Long
        get() = pref.getLong("update_interval", 1800)
        set(value) = pref.edit().putLong("update_interval", value).apply()

    var screenAspectRatio: Float
        get() {
            var aspect = pref.getFloat("screen_aspect_ratio", -1F)
            if (aspect == -1F) {
                val wm = MyApp.AppCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val point = Point()
                wm.defaultDisplay.getRealSize(point)
                val w = point.x.toFloat()
                val h = point.y
                aspect = w / h
                pref.edit().putFloat("screen_aspect_ratio", aspect).apply()
            }
            return aspect
        }
        set(_) {}
}