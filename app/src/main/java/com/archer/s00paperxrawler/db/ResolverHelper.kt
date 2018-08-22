package com.archer.s00paperxrawler.db

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import android.util.Log
import com.archer.s00paperxrawler.MyApp
import io.reactivex.Observable

private const val TAG = "ResolverHelper"

/**
 * Created by Chen Xin on 2018/8/16.
 * ContentResolver Helper
 */
enum class ResolverHelper {
    INSTANCE;

    private fun getCR(): ContentResolver = MyApp.AppCtx.contentResolver

    /**添加照片详情页面地址条目*/
    fun addPhotoDetail(url: String) {
        Log.d(TAG, "addPhotoDetail() called with: url = [ $url ]")
        val uri = PaperInfoContract.PHOTO_DETAIL_URI
        getCR().insert(uri, ContentValues().apply {
            put(PaperInfoContract.Columns.PHOTO_DETAIL_URL, url)
            put(PaperInfoContract.Columns.USED, 0)
        })
    }

    /**获取未使用的照片*/
    fun getUnusedPhotos(): Cursor {
        return getCR().query(PaperInfoContract.UNUSED_PHOTOS_URI,
                null, null, null, null)
    }

    /**获取未下载的照片地址*/
    fun getUndownloadPhotos(): Observable<Pair<Int, String>> {
        return Observable.create<Pair<Int, String>> { emitter ->
            val cursor = getCR().query(PaperInfoContract.UNDOWNLOAD_PHOTOS_URI,
                    arrayOf(BaseColumns._ID, PaperInfoColumns.PHOTO_URL), null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    emitter.onNext(Pair(cursor.getInt(0), cursor.getString(1)))
                }
                emitter.onComplete()
            }
        }
    }

    fun setPhotoPath(id: Int, name: String): Int {
        val values = ContentValues(2).apply {
            put(PaperInfoColumns.PHOTO_NAME, name)
            put(PaperInfoColumns.DOWNLOAD, 1)
        }
        return getCR().update(PaperInfoContract.PHOTO_PATH_URI, values, "${BaseColumns._ID} == $id", null)
    }
}