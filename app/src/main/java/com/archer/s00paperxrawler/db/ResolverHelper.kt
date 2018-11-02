package com.archer.s00paperxrawler.db

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.service.DownloadService
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import java.io.File

private const val TAG = "ResolverHelper"

/**下载信息*/
data class DownloadInfo(val id: Int, val url: String, val photoId: Long)

/**
 * Created by Chen Xin on 2018/8/16.
 * ContentResolver Helper
 */
enum class ResolverHelper {
    INSTANCE;

    private fun ContentResolver.queryAdapter(uri: Uri, projection: Array<String>? = null, selection: String? = null, selectionArgs: Array<String>? = null, sortOrder: String? = null): Cursor {
        return query(uri, projection, selection, selectionArgs, sortOrder)
    }

    private fun getCR(): ContentResolver = MyApp.AppCtx.contentResolver


    /**获取已下载到本地但未使用的照片*/
    fun getUnusedPhotos(limit: Int = -1): Cursor {
        val sLimit = if (limit > 0) " LIMIT $limit" else ""
        return getCR().queryAdapter(PaperInfoContract.UNUSED_PHOTOS_URI, sortOrder = "${BaseColumns._ID}$sLimit")
    }

    /**
     * 未下载的图片信息
     */
    private fun getUnDownPhotos(): Cursor {
        return getCR().queryAdapter(PaperInfoContract.UNDOWNLOAD_PHOTOS_URI)
    }

    /**
     * 缓存的图片是否足够
     * @param unusedPhotos 当前的有效缓存图片数量
     */
    private fun isCacheEnough(unusedPhotos: Int = getUnusedPhotos().use { it.count }): Boolean = unusedPhotos > prefs().minCacheSize

    /**
     * 是否应该加载更多图片信息到本地：已缓存的图片数量小于最小缓存数量，且可下载的图片信息小于最大缓存数量与已缓存图片的差值
     */
    fun shouldLoadMoreInfo(): Boolean {
        val unusedPhotos = getUnusedPhotos().use { it.count }
        val unDown = getUnDownPhotos().use { it.count }
        return !isCacheEnough(unusedPhotos) && unDown < prefs().maxCacheSize - unusedPhotos
    }


    /**获取未下载的照片地址，只获取一定数量的条目*/
    fun getUndownloadPhotos(): Observable<DownloadInfo> {
        return Observable.create<DownloadInfo> { emitter ->
            val prefs = prefs()
            val size = File(prefs.photosCachePath).list().size
            val limit = if (size == 0) prefs.maxCacheSize else {
                val unusedCount = getUnusedPhotos().use { it.count }
                if (isCacheEnough(unusedCount)) 0
                else prefs.maxCacheSize - unusedCount
            }
            if (limit <= 0) {
                emitter.onComplete()
            } else {
                val cursor = getCR().query(PaperInfoContract.UNDOWNLOAD_PHOTOS_URI,
                        arrayOf(BaseColumns._ID, PaperInfoColumns.PHOTO_URL, PaperInfoColumns.PHOTO_ID), null, null, "${BaseColumns._ID} LIMIT $limit")
                cursor.use {
                    while (it.moveToNext()) {
                        emitter.onNext(DownloadInfo(cursor.getInt(0), cursor.getString(1), cursor.getLong(2)))
                    }
                    emitter.onComplete()
                }
            }
        }
    }

    /**标记目标图片已下载到本地*/
    fun setPhotoDownloaded(id: Int): Int {
        val values = ContentValues(1).apply { put(PaperInfoColumns.DOWNLOAD, 1) }
        return getCR().update(PaperInfoContract.UNDOWNLOAD_PHOTOS_URI, values, "${BaseColumns._ID} == $id", null)
    }

    /**设置图片为已使用*/
    fun setPhotoUsed(id: Long): Int {
        prefs().isCacheEnough = isCacheEnough()
        val values = ContentValues().apply {
            put(PaperInfoColumns.USED, 1)
            put(PaperInfoColumns.SETTLED_DATE, System.currentTimeMillis())
        }
        return getCR().update(PaperInfoContract.UNUSED_PHOTOS_URI, values, "${PaperInfoColumns.PHOTO_ID} == $id", null)
    }

    /**添加照片信息
     * @param detailUrl 照片*/
    fun addPhotoInfo(detailUrl: String, id: Long, name: String, url: String, ph: String, aspect: Float) {
        Log.d(TAG, "addPhotoInfo() called with: detailUrl = [ $detailUrl ], id = [ $id ], name = [ $name ], url = [ $url ], ph = [ $ph ], aspect = [ $aspect ]")
        ContentValues(6).apply {
            put(PaperInfoColumns.USED, 0)
            if (!TextUtils.isEmpty(detailUrl)) put(PaperInfoColumns.PHOTO_DETAIL_URL, detailUrl)
            if (id > 0) put(PaperInfoColumns.PHOTO_ID, id)
            if (!TextUtils.isEmpty(name)) put(PaperInfoColumns.PHOTO_NAME, name)
            if (!TextUtils.isEmpty(url)) put(PaperInfoColumns.PHOTO_URL, url)
            if (!TextUtils.isEmpty(ph)) put(PaperInfoColumns.PH, ph)
            if (aspect > 0) put(PaperInfoColumns.ASPECT_RATIO, aspect)
        }.let { getCR().insert(PaperInfoContract.PAPER_INFO_URI, it) }
    }

    /**获取目标图片宽高比*/
//    fun getPhotoAspect(photoId: Long): Float {
//    }
}