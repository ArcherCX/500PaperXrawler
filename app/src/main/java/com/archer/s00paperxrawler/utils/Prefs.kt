package com.archer.s00paperxrawler.utils

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.text.TextUtils
import android.view.WindowManager
import androidx.preference.PreferenceManager
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
        get() = pref.getString("csrf_token", "")!!
        set(value) = pref.edit().putString("csrf_token", value).apply()

    var refreshInterval: Int
        get() = pref.getInt(getMyString(R.string.refresh_interval_key), getMyString(R.string.refresh_interval_default).toInt()) * 1800
        set(value) = pref.edit().putInt(getMyString(R.string.refresh_interval_key), value).apply()

    /**Valid wallpaper view area ratio: w/h */
    var wallPaperViewRatio: Float
        get() {
            var aspect = pref.getFloat("wallpaper_view_ratio", -1F)
            if (aspect == -1F) {
                val wm = getMyAppCtx().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val point = Point()
                wm.defaultDisplay.getRealSize(point)
                val w = point.x.toFloat()
                val h = point.y
                aspect = w / h
                pref.edit().putFloat("wallpaper_view_ratio", aspect).apply()
            }
            return aspect
        }
        set(value) = pref.edit().putFloat("wallpaper_view_ratio", value).apply()

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
            else if (!value && downloadViaWifi) DownloadService.cancelDownload()
        }

    /**暂停执行的联网操作*/
    var pendingDownloadAction: MutableSet<String>
        get() = pref.getStringSet("pending_download_action", mutableSetOf<String>())!!
        set(value) = pref.edit().putStringSet("pending_download_action", value).apply()

    var isFirstLaunch: Boolean
        get() = pref.getBoolean("is_first_launch", true)
        set(value) = pref.edit().putBoolean("is_first_launch", value).apply()

    /**是否当前壁纸应用*/
    val isCurrentWallPaper: Boolean
        get() = WallpaperManager.getInstance(getMyAppCtx()).wallpaperInfo?.packageName == getMyAppCtx().packageName ?: false/*pref.getBoolean("is_current_wall_paper", false)*/
//        set(value) = pref.edit().putBoolean("is_current_wall_paper", value).apply()

    val showNSFW: Boolean
        get() = pref.getBoolean(getMyString(R.string.show_nsfw_key), getMyString(R.string.show_nsfw_default).toBoolean())

    /**当前web壁纸的[com.archer.s00paperxrawler.db.PaperInfoContract.Columns.PHOTO_ID]*/
    var currentPhotoId: Long
        get() = pref.getLong("current_photo_id", -1L)
        set(value) = pref.edit().putLong("current_photo_id", if (isCurrentWallPaper) value else -1L).apply()

    /**Current local photo id in db*/
    var currentLocalPhotoId: Long
        get() = pref.getLong("local_photo_id", -1L)
        set(value) = pref.edit().putLong("local_photo_id", value).apply()

    /**Current Mode, true:web, false:local*/
    var currentMode: Boolean
        get() = pref.getBoolean(getMyString(R.string.mode_key), true)
        set(value) = pref.edit().putBoolean(getMyString(R.string.mode_key), value).apply()

    val parallaxEffectEnabled: Boolean
        get() = pref.getBoolean(getMyString(R.string.parallax_effect_key), true)

    /**Is first in the [DoubleTapPhotoDetailFragment][com.archer.s00paperxrawler.view.PhotoDetailFragment]*/
    var isFirstInDoubleTapDetail: Boolean
        get() = pref.getBoolean("first_in_double_tap_detail_fragment", true)
        set(value) = pref.edit().putBoolean("first_in_double_tap_detail_fragment", value).apply()

    var temporarilyEnableCustomOffset: Boolean
        get() = pref.getBoolean("temporarily_enable_custom_offset", false)
        set(value) = pref.edit().putBoolean("temporarily_enable_custom_offset", value).apply()

    /**the offset axis, true: X, false: Y*/
    var temporarilyCustomOffsetAxis: Boolean
        get() = pref.getBoolean("temporarily_custom_offset_axis", true)
        set(value) = pref.edit().putBoolean("temporarily_custom_offset_axis", value).apply()

    /**the value of the offset, it's percentage of photo's width(or height) */
    var temporarilyCustomOffsetValue: Float
        get() = pref.getFloat("custom_offset_value", 0F)
        set(value) = pref.edit().putFloat("custom_offset_value", value).apply()

    var permanentlyEnableCustomOffset: Boolean
        get() = pref.getBoolean("permanently_enable_custom_offset", false)
        set(value) = pref.edit().putBoolean("permanently_enable_custom_offset", value).apply()

    /**the offset axis, true: X, false: Y*/
    var permanentCustomOffsetAxis: Boolean
        get() = pref.getBoolean("permanent_custom_offset_axis", true)
        set(value) = pref.edit().putBoolean("permanent_custom_offset_axis", value).apply()

    /**the value of the offset, it's percentage of photo's width(or height) */
    var permanentCustomOffsetValue: Float
        get() = pref.getFloat("permanent_custom_offset_value", 0F)
        set(value) = pref.edit().putFloat("permanent_custom_offset_value", value).apply()

    var currentPhotoWidth: Int
        get() = pref.getInt("current_photo_width", 0)
        set(value) = pref.edit().putInt("current_photo_width", value).apply()

    var currentPhotoHeight: Int
        get() = pref.getInt("current_photo_height", 0)
        set(value) = pref.edit().putInt("current_photo_height", value).apply()

}