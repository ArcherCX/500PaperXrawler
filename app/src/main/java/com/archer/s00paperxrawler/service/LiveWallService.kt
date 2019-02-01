package com.archer.s00paperxrawler.service

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.SystemClock
import android.provider.BaseColumns
import android.util.Log
import android.view.MotionEvent
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.getLocalBroadcastManager
import com.archer.s00paperxrawler.gl.GLRenderer
import com.archer.s00paperxrawler.gl.OpenGLES2WallpaperService
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "LiveWallService"

const val ACTION_REFRESH_WALLPAPER = "refresh_wallpaper"
const val ACTION_MONITOR_DB = "monitor_db"

class LiveWallService : OpenGLES2WallpaperService() {

    private val myRender by lazy { MyRenderer() }

    override fun getNewRenderer(): GLRenderer {
        return myRender
    }

    override fun getRenderMode() = GLEngine.RENDERMODE_WHEN_DIRTY

    override fun onCreateEngine(): Engine = MyEngine().also {
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        prefs().isCurrentWallPaper = false
    }


    inner class MyEngine : OpenGLES2Engine(), Loader.OnLoadCompleteListener<Cursor> {
        private val loader: CursorLoader = CursorLoader(this@LiveWallService, PaperInfoContract.UNUSED_PHOTOS_URI, arrayOf(PaperInfoColumns.PHOTO_ID, PaperInfoColumns.ASPECT_RATIO), ResolverHelper.INSTANCE.getNSFWSelection(), null, "${BaseColumns._ID} LIMIT 1")
        private lateinit var timer: Disposable
        private val receiver: BroadcastReceiver
        private var timestamp = SystemClock.elapsedRealtime()

        init {
            val prefs = prefs()
            prefs.isCurrentWallPaper = WallpaperManager.getInstance(applicationContext).wallpaperInfo?.packageName == applicationContext.packageName ?: false
            prefs.currentPage = 1
            loader.registerListener(1, this)
            loader.startLoading()
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.action!!.let {
                        when (it) {
                            ACTION_REFRESH_WALLPAPER -> refreshWallpaper(0L)
                            ACTION_MONITOR_DB -> {
                                loader.selection = ResolverHelper.INSTANCE.getNSFWSelection()
                                loader.registerListener(1, this@MyEngine)
                                if (!loader.isStarted) loader.startLoading()
                            }
                        }
                    }
                }
            }
            getLocalBroadcastManager().registerReceiver(receiver, IntentFilter().apply {
                addAction(ACTION_REFRESH_WALLPAPER)
                addAction(ACTION_MONITOR_DB)
            })
        }

        override fun onLoadComplete(loader: androidx.loader.content.Loader<Cursor>, data: Cursor?) {
            Log.d(TAG, "onLoadComplete() called with: loader = [ ${loader.id} ], data = [ ${data?.count}, ${Arrays.toString(data?.columnNames)} ]")
            val count = data?.count ?: 0
            when {
                count > 0 -> {
                    if (data?.moveToNext() == true) {
                        val photoId = data.getLong(0)
                        val aspect = data.getFloat(1)
                        loader.reset()
                        loader.unregisterListener(this)
                        refreshWallpaper()
                        startDrawPaper(photoId, aspect)
                    }
                }
                ResolverHelper.INSTANCE.getUnDownPhotos().use { it.count } < prefs().maxCacheSize -> DownloadService.startLoadPhotosUrl()
                else -> DownloadService.startPhotosDownload()
            }
        }

        private fun startDrawPaper(photoId: Long, aspect: Float) {
            queueEvent { myRender.picPath = "${prefs().photosCachePath}/$photoId" }
            requestRender()
            prefs().currentPhotoId = photoId
        }

        private fun refreshWallpaper(interval: Long = prefs().refreshInterval.toLong()) {
            Log.d(TAG, "refreshWallpaper() called with: interval = [ $interval ] , timestamp = $timestamp")
            timer = Observable.timer(interval, TimeUnit.SECONDS).subscribe {
                refreshWallpaper()
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
                        startDrawPaper(id, aspect)
                    }
                    if (count < limit) {//数据库中可用图片少于最小缓存数量，提前下载新图片
                        if (count == 0) getLocalBroadcastManager().sendBroadcast(Intent(ACTION_MONITOR_DB))
                        val unDown = ResolverHelper.INSTANCE.getUnDownPhotos().use { it.count }
                        if (unDown < prefs.maxCacheSize) DownloadService.startLoadPhotosUrl()
                        else DownloadService.startPhotosDownload()
                    }
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
//            Log.d(TAG, "onTouchEvent ${MotionEvent.actionToString(event.action)}: count = ${event.pointerCount} , idx = ${event.actionIndex} ,id = ${event.getPointerId(event.actionIndex)}")

        }

        override fun onDestroy() {
            super.onDestroy()
            getLocalBroadcastManager().unregisterReceiver(receiver)
            if (loader.isStarted) {
                loader.unregisterListener(this)
                loader.stopLoading()
            }
            if (::timer.isInitialized && !timer.isDisposed) timer.dispose()
        }

    }
}
