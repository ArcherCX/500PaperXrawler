package com.archer.s00paperxrawler.presenter

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.text.format.Formatter
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.contract.REQUEST_CODE_CHANGE_LIVE_WALLPAPER
import com.archer.s00paperxrawler.contract.SettingsContract
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.getLocalBroadcastManager
import com.archer.s00paperxrawler.getMyAppCtx
import com.archer.s00paperxrawler.getMyString
import com.archer.s00paperxrawler.service.ACTION_MONITOR_DB
import com.archer.s00paperxrawler.service.ACTION_REFRESH_WALLPAPER
import com.archer.s00paperxrawler.service.DownloadService
import com.archer.s00paperxrawler.service.LiveWallService
import com.archer.s00paperxrawler.utils.prefs
import com.archer.s00paperxrawler.utils.registerWifiCallback
import com.archer.s00paperxrawler.utils.unregisterWifiAction
import com.archer.s00paperxrawler.view.setNewSummary
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.nio.charset.Charset

private const val TAG = "SettingsPresenter"
private const val CLEAR_TYPE_CACHE = 1
private const val CLEAR_TYPE_HISTORY = 2

/**
 * Created by Chen Xin on 2019/1/28.
 */
class SettingsPresenter(private val view: SettingsContract.View) : SettingsContract.Presenter {
    init {
        view.presenter = this
    }

    override fun init(fragment: Fragment) {
        val prefs = prefs()
        if (prefs.isFirstLaunch) {
            prefs.isFirstLaunch = false
            view.showDialog(getMyString(R.string.setup_wizard_title), getMyString(R.string.setup_wizard_msg)
                    , DialogInterface.OnClickListener { _, _ -> openLiveWallpaperConfig(fragment) })
        }
    }

    override fun onFeatureCategoryChange(preference: Preference?, newValue: Any?) {
        if (preference is MultiSelectListPreference) {
            val set = newValue as Set<String>
            if (set.size == 1 && set.contains("nude") && !prefs().showNSFW) {
                view.toast(getMyString(R.string.category_conflict_info))
                return
            }
        }
        view.showDialog(getMyString(R.string.hint), getMyString(R.string.feature_category_changed_hint), DialogInterface.OnClickListener { _, _ ->
            when (preference) {
                is ListPreference -> {
                    preference.setNewSummary(newValue)
                    preference.value = newValue as String
                }
                is MultiSelectListPreference -> {
                    preference.setNewSummary(newValue)
                    preference.values = newValue as MutableSet<String>
                }
            }
        })
    }

    override fun onPrepareClearCacheDialog() {
        onPrepareClearDataDialog(CLEAR_TYPE_CACHE, prefs().photosCachePath, R.string.clear_cache_title, R.string.clear_cache_dialog_msg, ::executeClearCache)
    }

    override fun onPrepareClearHistoryDialog() {
        onPrepareClearDataDialog(CLEAR_TYPE_HISTORY, prefs().photosHistoryPath, R.string.clear_history_title, R.string.clear_history_dialog_msg, ::executeClearHistory)
    }

    private fun onPrepareClearDataDialog(type: Int, dataFilePath: String, titleId: Int, msgId: Int, execImpl: () -> Unit) {
        Observable.just("du -d 0 -h $dataFilePath").observeOn(Schedulers.io()).flatMap { cmd ->
            return@flatMap Observable.just(cmd).map {
                return@map getCacheSizeByCommand(it)
            }.map {
                return@map if (it.startsWith("0") && File(dataFilePath).list().isNotEmpty()) getDirSizeByStatistic(dataFilePath)
                else it
            }.onErrorReturn {
                return@onErrorReturn getDirSizeByStatistic(dataFilePath)
            }
        }.observeOn(AndroidSchedulers.mainThread()).map {
            view.showDialog(getMyString(titleId), getMyString(msgId, it)
                    , DialogInterface.OnClickListener { _, _ ->
                execImpl()
            })
        }.subscribe()
    }

    private fun executeClearCache() {
        val prefs = prefs()
        val photosCachePath = prefs.photosCachePath
        Runtime.getRuntime().exec(arrayOf("rm", "-rf", "$photosCachePath/*"))
        val cacheDir = File(photosCachePath)
        cacheDir.listFiles().let {
            Log.w(TAG, "executeClearCache: command rm failed")
            if (!it.isEmpty()) it.forEach { file -> file.delete() }
        }
        prefs.currentPage = 1
        prefs.isCacheEnough = false
        ResolverHelper.INSTANCE.clearTable(PaperInfoContract.UNUSED_PHOTOS_URI)
        getLocalBroadcastManager().sendBroadcast(Intent(ACTION_MONITOR_DB))
        DownloadService.startLoadPhotosUrl()
    }

    private fun executeClearHistory() {
        Runtime.getRuntime().exec("rm -rf ${prefs().photosHistoryPath}/*")
        ResolverHelper.INSTANCE.clearTable(PaperInfoContract.PAPER_HISTORY_URI)
    }

    /**通过命令行计算缓存文件大小*/
    private fun getCacheSizeByCommand(cmd: String): String {
        val process = Runtime.getRuntime().exec(cmd)
        val inputStream = process.inputStream
        val bufferedReader = inputStream.bufferedReader(Charset.forName("UTF-8"))
        var size: String? = null
        bufferedReader.useLines {
            it.forEach { line ->
                var split = line.split("\t")
                if (split.size < 2) split = line.split(File.separator)
                size = split[0].trim()
            }
        }
        return size ?: "0"
    }

    /**通过遍历文件统计文件大小来计算缓存大小*/
    private fun getDirSizeByStatistic(dir: String): String {
        val files = File(dir).listFiles() ?: emptyArray()
        var size = 0L
        for (f in files) {
            size += f.length()
        }
        return Formatter.formatFileSize(getMyAppCtx(), size)
    }

    override fun openLiveWallpaperConfig(fragment: Fragment) {
        fragment.context?.apply {
            fragment.startActivityForResult(
                    Intent().also {
                        it.action = WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                        it.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@apply, LiveWallService::class.java))
                    }, REQUEST_CODE_CHANGE_LIVE_WALLPAPER)
        }
    }

    override fun onNSFWChange(show: Boolean): Boolean {
        val prefs = prefs()
        if (!show) {
            val categories = prefs.categories
            if (categories.size == 1 && categories.contains("nude")) {
                view.toast(getMyString(R.string.nsfw_conflict_info))
                return false
            }
            if (prefs.isCurrentWallPaper && ResolverHelper.INSTANCE.isNsfwPhoto(prefs.currentPhotoId)) getLocalBroadcastManager().sendBroadcast(Intent(ACTION_REFRESH_WALLPAPER))
        }
        return true
    }

    override fun onDownloadViaWifiChange(enable: Boolean) {
        if (enable) registerWifiCallback(getMyAppCtx()) else unregisterWifiAction(getMyAppCtx())
    }
}