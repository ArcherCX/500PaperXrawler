package com.archer.s00paperxrawler.presenter

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.archer.s00paperxrawler.*
import com.archer.s00paperxrawler.contract.REQUEST_CODE_CHANGE_LIVE_WALLPAPER
import com.archer.s00paperxrawler.contract.SettingsContract
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.service.*
import com.archer.s00paperxrawler.utils.*
import com.archer.s00paperxrawler.view.setNewSummary
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.internal.functions.Functions
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
        clearDirectory(prefs.photosCachePath)
        prefs.currentPage = 1
        prefs.isCacheEnough = false
        ResolverHelper.INSTANCE.clearTable(PaperInfoContract.URI.UNUSED_PHOTOS_URI)
        if (prefs.currentMode) sendLocalBroadcast(Intent(ACTION_MONITOR_DB))
        DownloadService.startLoadPhotosUrl()
    }

    private fun executeClearHistory() {
        clearDirectory(prefs().photosHistoryPath)
        ResolverHelper.INSTANCE.clearTable(PaperInfoContract.URI.PAPER_HISTORY_URI)
    }

    private fun clearDirectory(path: String) {
        val cacheDir = File(path)
        if (!cacheDir.isDirectory) return
        Runtime.getRuntime().exec(arrayOf("rm", "-rf", "$path/*"))
        cacheDir.listFiles().let {
            Log.w(TAG, "clear directory $path by command failed ? ${!it.isEmpty()}")
            if (!it.isEmpty()) it.forEach { file -> file.delete() }
        }
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
                        it.putExtra("SET_LOCKSCREEN_WALLPAPER", true)
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
            if (prefs.isCurrentWallPaper && ResolverHelper.INSTANCE.isNsfwPhoto(prefs.currentPhotoId)) sendLocalBroadcast(Intent(ACTION_REFRESH_WALLPAPER))
        }
        return true
    }

    override fun onDownloadViaWifiChange(enable: Boolean) {
        if (enable) registerWifiCallback(getMyAppCtx()) else {
            unregisterWifiAction(getMyAppCtx())
            DownloadService.startPendingDownloadAction()
        }
    }

    override fun onModeChange(newMode: Boolean): Boolean {
        if (newMode) {//web mode
            view.layoutAdjustForModeSwitch(newMode)
            doModeChange(newMode)
            return true
        } else {//local mode
            if (ResolverHelper.INSTANCE.hasLocalPhotosInfo()) {
                view.layoutAdjustForModeSwitch(newMode)
                doModeChange(newMode)
                return true
            }
            view.showDialog(getMyString(R.string.select_local_photo_title), getMyString(R.string.select_local_photo_msg),
                    DialogInterface.OnClickListener { _, _ -> view.startImagePicker() })
            return false
        }
    }

    private fun doModeChange(newMode: Boolean) {
        prefs().currentMode = newMode
        sendLocalBroadcast(Intent(ACTION_MODE_SWITCH))
    }

    @SuppressLint("CheckResult")
    override fun handlePhotoDir(data: Uri) {
        //take persistent read permission for the uri
        MyApp.AppCtx.contentResolver.takePersistableUriPermission(data, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        var enableLocalMode = false
        val iterateObservable = iterateLocalPhotoDir(data, true) ?: return
        iterateObservable.map {
            if (it) enableLocalMode = true
        }.observeOn(AndroidSchedulers.mainThread()).subscribe(Functions.emptyConsumer(), Functions.ON_ERROR_MISSING, Action {
            if (enableLocalMode || !prefs().currentMode) {
                view.layoutAdjustForModeSwitch(false)
                Log.w(TAG, "handlePhotoDir: onComplete mode = ${prefs().currentMode}")
                doModeChange(false)
            } else {
                view.toast(getMyString(R.string.cannot_find_valid_photo_files))
            }
            sendLocalBroadcast(Intent(ACTION_LOCAL_PHOTO_DIRS_ITERATE_DONE))
        })
    }
}