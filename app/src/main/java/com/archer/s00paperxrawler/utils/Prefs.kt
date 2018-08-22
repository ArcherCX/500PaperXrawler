package com.archer.s00paperxrawler.utils

import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.TextUtils
import com.archer.s00paperxrawler.MyApp
import java.io.File
import kotlin.reflect.KProperty

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

    var feature: String
        get() = pref.getString("feature", "popular")
        set(value) = pref.edit().putString("feature", value).apply()

    var categories: String
        get() = pref.getString("categories", "/")
        set(value) = pref.edit().putString("categories", value).apply()

    var minCacheSize: Int
        get() = pref.getInt("min_cache_size", 10)
        set(value) = pref.edit().putInt("min_cache_size", value).apply()

    var maxCacheSize: Int
        get() = pref.getInt("max_cache_size", 30)
        set(value) = pref.edit().putInt("max_cache_size", value).apply()

    var defaultCachePath: String
        get() {
            var path = pref.getString("default_cache_path", "")
            if (TextUtils.isEmpty(path)) {
                path = File(MyApp.AppCtx.cacheDir, "photos").apply { if (!exists()) mkdirs() }.absolutePath
                pref.edit().putString("default_cache_path", path).apply()
            }
            return path
        }
        set(_){}

}