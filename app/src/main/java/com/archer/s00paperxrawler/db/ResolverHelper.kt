package com.archer.s00paperxrawler.db

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import com.archer.s00paperxrawler.MyApp
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

    private fun ContentResolver.query(uri: Uri): Cursor {
        return query(uri, null, null, null, null)
    }

    private fun getCR(): ContentResolver = MyApp.AppCtx.contentResolver


    /**获取已下载到本地但未使用的照片*/
    private fun getUnusedPhotos(): Cursor {
        return getCR().query(PaperInfoContract.UNUSED_PHOTOS_URI)
    }

    /**获取未下载的照片地址，只获取一定数量的条目*/
    fun getUndownloadPhotos(): Observable<DownloadInfo> {
        return Observable.create<DownloadInfo> { emitter ->
            val prefs = prefs()
            val size = File(prefs.photosCachePath).list().size
            val limit = if (size == 0) prefs.maxCacheSize else {
                if (getUnusedPhotos().use { it.count } > prefs.minCacheSize) 0
                else prefs.maxCacheSize - prefs.minCacheSize
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
        val values = ContentValues(1).apply {
            put(PaperInfoColumns.DOWNLOAD, 1)
        }
        return getCR().update(PaperInfoContract.PAPER_INFO_URI, values, "${BaseColumns._ID} == $id", null)
    }

    /**添加照片信息
     * @param detailUrl 照片*/
    fun addPhotoInfo(detailUrl: String, id: Int, name: String, url: String, ph: String, aspect: Float) {
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

    /*fun checkParseResult(html: String): Boolean {
        ContentValues().apply {
            put(PaperInfoColumns.PHOTO_DETAIL_URL, html)
        }.let {
            val ret = getCR().query(PaperInfoContract.PAPER_INFO_URI, null, null, null, null)
            ret.use {
                it.moveToNext()
            }
        }
    }*/
}