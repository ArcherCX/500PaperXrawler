package com.archer.s00paperxrawler.db

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import com.archer.s00paperxrawler.getMyAppCtx
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import java.io.File

private const val TAG = "ResolverHelper"
typealias MyUri = PaperInfoContract.URI

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

    private fun getCR(): ContentResolver = getMyAppCtx().contentResolver

    private fun getDBBoolConstant(boolean: Boolean) = if (boolean) DB_CONSTANT.TRUE else DB_CONSTANT.FALSE

    fun getNSFWSelection(): String? {
        val nsfw = prefs().showNSFW
        return if (nsfw.not()) "${PaperInfoColumns.NSFW} == ${getDBBoolConstant(false)}" else null
    }

    /**获取已下载到本地但未使用的照片*/
    fun getUnusedPhotos(limit: Int = -1): Cursor {
        val sLimit = if (limit > 0) " LIMIT $limit" else ""
        return getCR().queryAdapter(MyUri.UNUSED_PHOTOS_URI,
                selection = getNSFWSelection(),
                sortOrder = "${BaseColumns._ID}$sLimit")
    }

    /**
     * 未下载的图片信息
     */
    fun getUnDownPhotos(): Cursor {
        return getCR().queryAdapter(MyUri.UNDOWNLOAD_PHOTOS_URI, selection = getNSFWSelection())
    }

    /**
     * 缓存的图片是否足够
     * @param unusedPhotos 当前的有效缓存图片数量
     */
    fun isCacheEnough(unusedPhotos: Int = getUnusedPhotos().use { it.count }): Boolean = unusedPhotos >= prefs().minCacheSize

    /**
     * 是否应该加载更多图片信息到本地：已缓存的图片数量小于最小缓存数量，即缓存不足，且可下载的图片信息小于最大可缓存数量与已缓存图片的差值
     */
    fun shouldLoadMoreInfo(): Boolean {
        val unusedPhotos = getUnusedPhotos().use { it.count }
        val unDown = getUnDownPhotos().use { it.count }
        return !isCacheEnough(unusedPhotos) && unDown < prefs().maxCacheSize - unusedPhotos
    }


    /**获取未下载的照片地址，只获取一定数量的条目*/
    fun getUndownloadPhotosUrl(): Observable<DownloadInfo> {
        return Observable.create<DownloadInfo> { emitter ->
            val prefs = prefs()
            val size = File(prefs.photosCachePath).list().size
            val limit =
                    if (size == 0) prefs.maxCacheSize//如果一张未下，下载最大缓存数量的图片
                    else {//如果下载过图片，则检查数据库中下载但未使用的图片数量unusedCount，小于minCacheSize就下载（maxCacheSize-unusedCount）张，否则不下载
                        val unusedCount = getUnusedPhotos().use { it.count }
                        if (isCacheEnough(unusedCount)) 0
                        else prefs.maxCacheSize - unusedCount
                    }
            if (limit <= 0) {
                emitter.onComplete()
            } else {
                val cursor = getCR().query(MyUri.UNDOWNLOAD_PHOTOS_URI,
                        arrayOf(BaseColumns._ID, PaperInfoColumns.PHOTO_URL, PaperInfoColumns.PHOTO_ID),
                        getNSFWSelection(), null, "${BaseColumns._ID} LIMIT $limit")
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
    fun setPhotoDownloaded(id: Int, isDownload: Int = DB_CONSTANT.TRUE): Int {
        val values = ContentValues(1).apply { put(PaperInfoColumns.DOWNLOAD, isDownload) }
        return getCR().update(MyUri.UNDOWNLOAD_PHOTOS_URI, values, "${BaseColumns._ID} == $id", null)
    }

    /**设置图片为已使用*/
    fun setPhotoUsed(id: Long): Int {
        val prefs = prefs()
        if (!prefs.isCurrentWallPaper) return 0
        Runtime.getRuntime().exec("mv ${prefs.photosCachePath}/$id ${prefs.photosHistoryPath}")
        prefs.isCacheEnough = isCacheEnough()
        val values = ContentValues().apply {
            put(PaperInfoColumns.USED, DB_CONSTANT.TRUE)
            put(PaperInfoColumns.SETTLED_DATE, System.currentTimeMillis())
        }
        return getCR().update(MyUri.UNUSED_PHOTOS_URI, values, "${PaperInfoColumns.PHOTO_ID} == $id", null)
    }

    /**
     * 添加照片信息
     * @param detailUrl 照片
     */
    fun addPhotoInfo(detailUrl: String, id: Long, name: String, url: String, ph: String, aspect: Float, nsfw: Boolean) {
//        Log.d(TAG, "addPhotoInfo() called with: detailUrl = [ $detailUrl ], id = [ $id ], name = [ $name ], url = [ $url ], ph = [ $ph ], aspect = [ $aspect ]")
        ContentValues(6).apply {
            put(PaperInfoColumns.USED, DB_CONSTANT.FALSE)
            if (!TextUtils.isEmpty(detailUrl)) put(PaperInfoColumns.PHOTO_DETAIL_URL, detailUrl)
            if (id > 0) put(PaperInfoColumns.PHOTO_ID, id)
            if (!TextUtils.isEmpty(name)) put(PaperInfoColumns.PHOTO_NAME, name)
            if (!TextUtils.isEmpty(url)) put(PaperInfoColumns.PHOTO_URL, url)
            if (!TextUtils.isEmpty(ph)) put(PaperInfoColumns.PH, ph)
            if (aspect > 0) put(PaperInfoColumns.ASPECT_RATIO, aspect)
            put(PaperInfoColumns.NSFW, nsfw)
        }.let { getCR().insert(MyUri.PAPER_INFO_URI, it) }
    }

    /**指定图片是否是nsfw图片*/
    fun isNsfwPhoto(photoId: Long): Boolean {
        if (photoId < 0) return false
        getCR().queryAdapter(MyUri.PAPER_INFO_URI, arrayOf(PaperInfoColumns.NSFW), "${PaperInfoColumns.PHOTO_ID} == $photoId").use {
            return if (it.count > 0) {
                it.moveToNext()
                it.getInt(0) == DB_CONSTANT.TRUE
            } else false
        }
    }

    /**清理指定uri对应表或视图的数据*/
    fun clearTable(uri: Uri): Unit {
        getCR().delete(uri, null, null)
    }

    /**是否有本地图片信息*/
    fun hasLocalPhotosInfo(): Boolean {
        return getCR().queryAdapter(MyUri.LOCAL_PHOTO_INFO_URI, arrayOf(BaseColumns._ID))?.use { it.count > 0 }
                ?: false

    }

    /**添加本地图片信息*/
    fun addLocalPhotoInfo(uri: String, name: String?, isDir: Boolean = false, ownerId: Long = -1): Uri? {
        ContentValues(2).apply {
            put(PaperInfoColumns.LOCAL_PHOTO_URI, uri)
            put(PaperInfoColumns.PHOTO_NAME, name)
            put(PaperInfoColumns.IS_DIR, if (isDir) DB_CONSTANT.TRUE else DB_CONSTANT.FALSE)
            put(PaperInfoColumns.LOCAL_FILE_OWNER, ownerId)
        }.run {
            return getCR().insert(MyUri.LOCAL_PHOTO_INFO_URI, this)
        }
    }

    /**重置所有本地图片的"使用"标识字段为false*/
    fun resetAllLocalPhotoUsedFlag() =
            getCR().update(MyUri.LOCAL_PHOTO_INFO_URI, ContentValues().apply {
                put(PaperInfoColumns.USED, DB_CONSTANT.FALSE)
            }, "${PaperInfoColumns.IS_DIR} == ${DB_CONSTANT.FALSE}", null)

    /**将目标本地图片设置为已使用*/
    fun setLocalPhotoUsed(id: Long) =
            getCR().update(MyUri.LOCAL_PHOTO_INFO_URI, ContentValues().apply {
                put(PaperInfoColumns.USED, DB_CONSTANT.TRUE)
            }, "${BaseColumns._ID} == $id", null)

    /**获取本地未使用的图片*/
    @SuppressLint("Recycle")
    fun getUnusedLocalPhoto(): Cursor =
            getCR().query(MyUri.LOCAL_PHOTO_INFO_URI,
                    arrayOf(BaseColumns._ID, PaperInfoColumns.LOCAL_PHOTO_URI),
                    "${PaperInfoColumns.USED} == ${DB_CONSTANT.FALSE} and ${PaperInfoColumns.IS_DIR} == ${DB_CONSTANT.FALSE}",
                    null,
                    "${BaseColumns._ID} LIMIT 1")!!

    /**删除数据库中已不再原地的本地图片信息*/
    fun deleteUnExistLocalPhoto(id: Long) {
        getCR().delete(MyUri.LOCAL_PHOTO_INFO_URI, "${BaseColumns._ID} == $id", null)
    }

    /**
     * Does this directory already in the db
     * @return The id of this directory or -1 if it's not exist
     */
    fun doesDirExist(uri: String): Long {
        getCR().query(MyUri.LOCAL_PHOTO_INFO_URI, arrayOf(BaseColumns._ID),
                "${PaperInfoColumns.IS_DIR} == ${DB_CONSTANT.TRUE} AND ${PaperInfoColumns.LOCAL_PHOTO_URI} == ?",
                arrayOf(uri), null, null)?.use {
            if (it.moveToNext()) return it.getLong(0)
        }
        return -1L
    }

    fun getAllLocalDirs() =
            getCR().queryAdapter(MyUri.LOCAL_PHOTO_INFO_URI, arrayOf(PaperInfoColumns.LOCAL_PHOTO_URI, PaperInfoColumns.PHOTO_NAME, BaseColumns._ID), "${PaperInfoColumns.IS_DIR} == ${DB_CONSTANT.TRUE}")

    /**
     * @return an array which contains two list, [0] is nameArray, [1] is corresponding idArray
     */
    fun getAllLocalDirNames(): Array<ArrayList<String>>? {
        getAllLocalDirs().use {
            val count = it.count
            if (count <= 0) return null
            val nameArray = arrayListOf<String>()
            val idArray = arrayListOf<String>()
            val retArray = arrayOf(nameArray, idArray)
            while (it.moveToNext()) {
                val name = it.getString(1)
                val id = it.getLong(2).toString()
                nameArray.add(name)
                idArray.add(id)
            }
            return retArray
        }
    }

    fun deleteLocalDirsAndPhotos(dirIds: ArrayList<String>): Unit {
        val cr = getCR()
        val targetIds = dirIds.toString().removePrefix("[").removeSuffix("]")
        cr.delete(MyUri.LOCAL_PHOTO_INFO_URI,
                "${BaseColumns._ID} in ($targetIds) OR ${PaperInfoColumns.LOCAL_FILE_OWNER} in ($targetIds)",
                null)
    }
}