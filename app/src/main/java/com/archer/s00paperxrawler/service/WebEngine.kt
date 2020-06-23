package com.archer.s00paperxrawler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.os.SystemClock
import android.provider.BaseColumns
import android.util.Log
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.registerLocalBCR
import com.archer.s00paperxrawler.sendLocalBroadcast
import com.archer.s00paperxrawler.unregisterLocalBCR
import com.archer.s00paperxrawler.utils.prefs
import java.util.*

private const val TAG = "WebEngine"

/**
 * Created by Chen Xin on 2020/6/19.
 */
class WebEngine(ctx: Context) : IEngineImpl {
    private var receiver: BroadcastReceiver? = null
    override val loader: CursorLoader = CursorLoader(ctx, PaperInfoContract.URI.UNUSED_PHOTOS_URI, arrayOf(PaperInfoColumns.PHOTO_ID, PaperInfoColumns.ASPECT_RATIO), ResolverHelper.INSTANCE.getNSFWSelection(), null, "${BaseColumns._ID} LIMIT 1")
    private var listenerRegistered = false
    private var timestamp = SystemClock.elapsedRealtime()

    override fun init() {
        registerLoaderListener(1, this)
        loader.startLoading()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.action!!.let {
                    when (it) {
                        ACTION_MONITOR_DB -> {
                            loader.selection = ResolverHelper.INSTANCE.getNSFWSelection()
                            registerLoaderListener(1, this@WebEngine)
                            if (!loader.isStarted) loader.startLoading()
                        }
                    }
                }
            }
        }
        registerLocalBCR(receiver!!, IntentFilter().apply {
            addAction(ACTION_MONITOR_DB)
        })
    }

    override fun onDestroy() {
        unregisterLocalBCR(receiver!!)
        receiver = null
        if (loader.isStarted) {
            unregisterLoaderListener(this)
            loader.reset()
        }
    }

    override fun onLoadComplete(loader: Loader<Cursor>, data: Cursor?) {
        Log.d(TAG, "onLoadComplete() called with: loader = [ ${loader.id} ], data = [ ${data?.count}, ${Arrays.toString(data?.columnNames)} ]")
        val count = data?.count ?: 0
        when {
            count > 0 -> {
                if (data?.moveToNext() == true) {
                    val photoId = data.getLong(0)
                    val aspect = data.getFloat(1)
                    unregisterLoaderListener(this)
                    this.loader.reset()
                    //trigger refresh timer
                    sendLocalBroadcast(Intent(ACTION_REFRESH_WALLPAPER).apply { putExtra(INTENT_EXTRA_KEY_REFRESH_INTERVAL, prefs().refreshInterval.toLong()) })
                    //draw current wallpaper
                    doPreDrawPaper(photoId)
                }
            }
            ResolverHelper.INSTANCE.getUnDownPhotos().use { it.count } < prefs().maxCacheSize -> DownloadService.startLoadPhotosUrl()
            else -> DownloadService.startPhotosDownload()
        }
    }

    private fun doPreDrawPaper(photoId: Long) {
        onPreDrawPaper(Intent().apply {
            putExtras(Bundle().apply {
                putString(BUNDLE_EXTRA_KEY_PATH_URI, "${prefs().photosCachePath}/$photoId")
                putLong(BUNDLE_EXTRA_KEY_PHOTO_ID, photoId)
            })
        })
    }

    override fun onPostDrawPaper(extras: Bundle) {
        prefs().currentPhotoId = extras.getLong(BUNDLE_EXTRA_KEY_PHOTO_ID, -1)
    }

    override fun onRefreshWallpaper() {
        val prefs = prefs()
        //将上一张图片放入历史记录
        val currentPhotoId = prefs.currentPhotoId
        if (currentPhotoId > -1) ResolverHelper.INSTANCE.setPhotoUsed(currentPhotoId)
        //每天重置page
        if (SystemClock.elapsedRealtime() - timestamp > 86400000/*60*60*24*1000,1 day*/) {
            prefs.currentPage = 1
            timestamp = SystemClock.elapsedRealtime()
        }
        //展示下一张图片
        val limit = prefs.minCacheSize
        val unusedPhotos = ResolverHelper.INSTANCE.getUnusedPhotos(limit)
        unusedPhotos.use { cur ->
            val count = cur.count
            if (cur.moveToNext()) {
                val id = cur.getLong(cur.getColumnIndex(PaperInfoColumns.PHOTO_ID))
                val aspect = cur.getFloat(cur.getColumnIndexOrThrow(PaperInfoColumns.ASPECT_RATIO))
                Log.d(TAG, "refreshWallpaper() called with : id = [ $id ] ")
                doPreDrawPaper(id)
            }
            if (count < limit) {//数据库中可用图片少于最小缓存数量，提前下载新图片
                if (count == 0) sendLocalBroadcast(Intent(ACTION_MONITOR_DB))
                val unDown = ResolverHelper.INSTANCE.getUnDownPhotos().use { it.count }
                if (unDown < prefs.maxCacheSize) DownloadService.startLoadPhotosUrl()
                else DownloadService.startPhotosDownload()
            }
        }
    }

    private fun registerLoaderListener(id: Int, listener: Loader.OnLoadCompleteListener<Cursor>) {
        if (!listenerRegistered) {
            listenerRegistered = true
            loader.registerListener(id, listener)
        }
    }

    private fun unregisterLoaderListener(listener: Loader.OnLoadCompleteListener<Cursor>) {
        if (listenerRegistered) {
            listenerRegistered = false
            loader.unregisterListener(listener)
        }
    }

}