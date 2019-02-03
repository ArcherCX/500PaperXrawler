package com.archer.s00paperxrawler.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.WindowManager
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.getMyAppCtx
import com.archer.s00paperxrawler.getMyString
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

    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getMyAppCtx())

    var baseUri: String
        get() = pref.getString("base_uri", "https://500px.com")!!
        set(value) = pref.edit().putString("base_uri", value).apply()

    var baseApiUri: String
        get() = pref.getString("base_uri", "https://api.500px.com/v1/photos?")!!
        set(value) = pref.edit().putString("base_uri", value).apply()

    var feature: String
        get() = pref.getString(getMyString(R.string.feature_key), getMyString(R.string.feature_default))!!
        set(value) {
            pref.edit().putString(getMyString(R.string.feature_key), value).apply()
            currentPage = 1
        }

    var categories: Set<String>
        get() = pref.getStringSet("categories", emptySet())!!
        set(value) {
            pref.edit().putStringSet("categories", value).apply()
            currentPage = 1
        }

    var minCacheSize: Int
        get() = pref.getInt("min_cache_size", 10)
        set(value) = pref.edit().putInt("min_cache_size", if (value < 10) 10 else value).apply()

    var maxCacheSize: Int
        get() = pref.getInt("max_cache_size", 20)
        set(value) = pref.edit().putInt("max_cache_size", if (value < 20) 20 else value).apply()

    val photosCachePath: String
        get() {
            var path = pref.getString("default_cache_path", "")
            if (TextUtils.isEmpty(path)) {
                path = File(getMyAppCtx().cacheDir, "photos").apply { if (!exists()) mkdirs() }.absolutePath
                pref.edit().putString("default_cache_path", path).apply()
            }
            return path!!
        }

    val photosHistoryPath: String
        get() {
            var path = pref.getString("history_photo_path", "")
            if (TextUtils.isEmpty(path)) {
                path = File(getMyAppCtx().cacheDir, "history").apply { if (!exists()) mkdirs() }.absolutePath
                pref.edit().putString("history_photo_path", path).apply()
            }
            return path!!
        }

    var isCacheEnough: Boolean
        get() = pref.getBoolean("is_cache_enough", false)
        set(value) {
            pref.edit().putBoolean("is_cache_enough", value).apply()
            if (!value) DownloadService.startPhotosDownload()
        }

    var csrfToken: String
        get() = pref.getString("csrf_token", "")
        set(value) = pref.edit().putString("csrf_token", value).apply()

    var refreshInterval: Int
        get() = pref.getInt(getMyString(R.string.refresh_interval_key), getMyString(R.string.refresh_interval_default).toInt()) * 1800
        set(value) = pref.edit().putInt(getMyString(R.string.refresh_interval_key), value).apply()

    val screenAspectRatio: Float
        get() {
            var aspect = pref.getFloat("screen_aspect_ratio", -1F)
            if (aspect == -1F) {
                val wm = getMyAppCtx().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val point = Point()
                wm.defaultDisplay.getRealSize(point)
                val w = point.x.toFloat()
                val h = point.y
                aspect = w / h
                pref.edit().putFloat("screen_aspect_ratio", aspect).apply()
            }
            return aspect
        }

    /**当前要查询的图片所在页数，每天重置*/
    var currentPage: Int
        get() = pref.getInt("current_page", 1)
        set(value) = pref.edit().putInt("current_page", if (value < 1) 1 else value).apply()

    /**应用可存储的最大图片数量*/
    val storageCache: Int
        get() = pref.getInt(getMyString(R.string.storage_cache_key), getMyString(R.string.storage_cache_default).toInt())

    /**仅通过Wifi下载图片*/
    val downloadViaWifi: Boolean
        get() = pref.getBoolean(getMyString(R.string.download_via_wifi_key), getMyString(R.string.download_via_wifi_default).toBoolean())

    var wifiAvailable: Boolean
        get() = pref.getBoolean("wifi_available", false)
        set(value) {
            pref.edit().putBoolean("wifi_available", value).apply()
            if (value) DownloadService.startPendingDownloadAction()
        }

    /**暂停执行的联网操作*/
    var pendingDownloadAction: MutableSet<String>
        get() = pref.getStringSet("pending_download_action", mutableSetOf<String>())
        set(value) = pref.edit().putStringSet("pending_download_action", value).apply()

    var isFirstLaunch: Boolean
        get() = pref.getBoolean("is_first_launch", true)
        set(value) = pref.edit().putBoolean("is_first_launch", value).apply()

    /**是否当前壁纸应用*/
    var isCurrentWallPaper: Boolean
        get() = pref.getBoolean("is_current_wall_paper", false)
        set(value) = pref.edit().putBoolean("is_current_wall_paper", value).apply()

    val showNSFW: Boolean
        get() = pref.getBoolean(getMyString(R.string.show_nsfw_key), getMyString(R.string.show_nsfw_default).toBoolean())

    /**当前壁纸的[com.archer.s00paperxrawler.db.PaperInfoContract.Columns.PHOTO_ID]*/
    var currentPhotoId: Long
        get() = pref.getLong("current_photo_id", -1L)
        set(value) = pref.edit().putLong("current_photo_id", if (isCurrentWallPaper) value else -1L).apply()
}