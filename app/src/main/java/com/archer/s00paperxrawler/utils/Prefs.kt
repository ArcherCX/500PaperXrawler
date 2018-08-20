package com.archer.s00paperxrawler.utils

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.archer.s00paperxrawler.MyApp
import kotlin.reflect.KProperty

fun Pref(): Prefs {
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

}