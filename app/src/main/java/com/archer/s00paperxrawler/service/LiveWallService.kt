package com.archer.s00paperxrawler.service

import android.database.Cursor
import android.provider.BaseColumns
import android.util.Log
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.gl.GLRenderer
import com.archer.s00paperxrawler.gl.OpenGLES2WallpaperService
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "LiveWallService"

class LiveWallService : OpenGLES2WallpaperService() {

    private lateinit var pageTimer: Disposable

    private val myRender by lazy { MyRenderer() }

    override fun getNewRenderer(): GLRenderer {
        return myRender
    }

    override fun getRenderMode() = GLEngine.RENDERMODE_WHEN_DIRTY

    override fun onCreateEngine(): Engine = MyEngine().also {
        DownloadService.startLoadPhotosUrl()
        pageTimer = Observable.timer(1, TimeUnit.DAYS).subscribe {
            prefs().currentPage = 1
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        if (::pageTimer.isInitialized && !pageTimer.isDisposed) pageTimer.dispose()
    }


    inner class MyEngine : OpenGLES2Engine(), androidx.loader.content.Loader.OnLoadCompleteListener<Cursor> {
        private val loader: androidx.loader.content.CursorLoader = androidx.loader.content.CursorLoader(this@LiveWallService, PaperInfoContract.UNUSED_PHOTOS_URI, arrayOf(PaperInfoColumns.PHOTO_ID, PaperInfoColumns.ASPECT_RATIO), null, null, "${BaseColumns._ID} LIMIT 1")
        private lateinit var timer: Disposable

        init {
            loader.registerListener(1, this)
            loader.startLoading()
        }

        override fun onLoadComplete(loader: androidx.loader.content.Loader<Cursor>, data: Cursor?) {
            Log.d(TAG, "onLoadComplete() called with: loader = [ ${loader.id} ], data = [ ${data?.count}, ${Arrays.toString(data?.columnNames)} ]")
            if (data != null && data.count > 0) {
                data.moveToNext()
                val photoId = data.getLong(0)
                val aspect = data.getFloat(1)
                data.close()
                loader.unregisterListener(this)
                loader.stopLoading()
                startDrawPaper(photoId, aspect)
                refreshWallpaper()
            }
        }

        private fun startDrawPaper(photoId: Long, aspect: Float) {
            queueEvent { myRender.picPath = "${prefs().photosCachePath}/$photoId" }
            requestRender()
            ResolverHelper.INSTANCE.setPhotoUsed(photoId)
        }

        private fun refreshWallpaper() {
            timer = Observable.timer(prefs().updateInterval, TimeUnit.SECONDS).subscribe {
                val limit = 2
                val unusedPhotos = ResolverHelper.INSTANCE.getUnusedPhotos(limit)
                unusedPhotos.use { cur ->
                    val count = cur.count
                    if (count > 0) {
                        cur.moveToNext()
                        val id = cur.getLong(cur.getColumnIndex(PaperInfoColumns.PHOTO_ID))
                        val aspect = cur.getFloat(cur.getColumnIndexOrThrow(PaperInfoColumns.ASPECT_RATIO))
                        Log.d(TAG, "refreshWallpaper() called with: id = [ $id ]")
                        startDrawPaper(id, aspect)
                    }
                    if (count < limit) {//数据库中可用图片即将耗罄，提前下载新图片
                        val unDown = ResolverHelper.INSTANCE.getUnDownPhotos().use { it.count }
                        if (unDown < prefs().maxCacheSize) DownloadService.startLoadPhotosUrl()
                        else DownloadService.startPhotosDownload()
                    }
                }
                refreshWallpaper()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            if (loader.isStarted) {
                loader.unregisterListener(this)
                loader.stopLoading()
            }
            if (::timer.isInitialized && !timer.isDisposed) timer.dispose()
        }

    }
}
