package com.archer.s00paperxrawler.service

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.DB_CONSTANT
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.sendLocalBroadcast
import com.archer.s00paperxrawler.utils.ACTION_LOCAL_PHOTO_DIRS_ITERATE_DONE
import com.archer.s00paperxrawler.utils.iterateLocalPhotoDir
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.internal.functions.Functions

private const val TAG = "LocalEngine"

/**
 * Created by Chen Xin on 2020/6/19.
 */
class LocalEngine(ctx: Context) : IEngineImpl {
    override val loader: CursorLoader = CursorLoader(ctx, PaperInfoContract.URI.LOCAL_PHOTO_INFO_URI, arrayOf(BaseColumns._ID, PaperInfoColumns.LOCAL_PHOTO_URI), "${PaperInfoColumns.USED} == ${DB_CONSTANT.FALSE} AND ${PaperInfoColumns.IS_DIR} == ${DB_CONSTANT.FALSE}", null, "${BaseColumns._ID} LIMIT 1")
    override fun init() {
        loader.registerListener(1, this)
        loader.startLoading()
    }

    override fun onDestroy() {
        if (loader.isStarted) {
            loader.reset()
        }
    }

    override fun onLoadComplete(loader: Loader<Cursor>, data: Cursor?) {
        Log.d(TAG, "onLoadComplete() called with: loader = $loader, data = $data, count = ${data?.count}")
        if (data != null) {
            if (data.count > 0) {
                if (data.moveToNext()) {
                    if (!doPreDrawPaper(data.getLong(0), data.getString(1))) {
                        return
                    }
                    sendLocalBroadcast(Intent(ACTION_REFRESH_WALLPAPER).apply { putExtra(INTENT_EXTRA_KEY_REFRESH_INTERVAL, prefs().refreshInterval.toLong()) })
                    this.loader.unregisterListener(this)
                }
            } else {//mean all local photos had been used, let's rescan local photo dir, if no more new, then reset all local photos USED flag
                rescanAndResetUsedFlag(null)
            }
        }
    }

    /**
     * Do some prepare operations before do the real draw
     * @return Can this passed photo be used
     */
    private fun doPreDrawPaper(photoId: Long, uri: String): Boolean {
        if (!DocumentFile.fromSingleUri(MyApp.AppCtx, Uri.parse(uri))!!.exists()) {
            ResolverHelper.INSTANCE.deleteUnExistLocalPhoto(photoId)
            Log.e(TAG, "doPreDrawPaper() called when this photo not exist: photoId = $photoId, uri = $uri")
            return false
        }
        onPreDrawPaper(Intent().apply {
            putExtras(Bundle().apply {
                putLong(BUNDLE_EXTRA_KEY_PHOTO_ID, photoId)
                putString(BUNDLE_EXTRA_KEY_PATH_URI, uri)
            })
        })
        return true
    }

    override fun onPostDrawPaper(extras: Bundle) {
        prefs().currentLocalPhotoId = extras.getLong(BUNDLE_EXTRA_KEY_PHOTO_ID, -1L)
    }

    private var rescanFailureCount = 0
    private val rescanFailureLimit = 3
    override fun onRefreshWallpaper() {
        Log.d(TAG, "onRefreshWallpaper() called")
        prefs().currentLocalPhotoId.also {
            if (it > -1) ResolverHelper.INSTANCE.setLocalPhotoUsed(it)
        }
        ResolverHelper.INSTANCE.getUnusedLocalPhoto().use {
            if (it.count > 0 && it.moveToNext()) {
                rescanFailureCount = 0
                if (!doPreDrawPaper(it.getLong(0), it.getString(1))) onRefreshWallpaper()
            } else {
                rescanFailureCount++
                /*If there is no photo in db and no more photos show up after rescan 3 times, then give up to avoid infinite loop until next refresh*/
                if (rescanFailureCount <= rescanFailureLimit) rescanAndResetUsedFlag(::onRefreshWallpaper)
                else rescanFailureCount = 0
            }
        }
    }

    /**
     * Rescan local photo directories, if there are new photos, use them, else, reset all photos USED flag
     */
    private fun rescanAndResetUsedFlag(afterFunc: (() -> Unit)?) {
        ResolverHelper.INSTANCE.getAllLocalDirs().use {
            while (it.moveToNext()) {
                val uri = Uri.parse(it.getString(0))
                val iterateObservable = iterateLocalPhotoDir(uri) ?: return
                var reset = true
                iterateObservable.subscribe(Consumer { addSuccessfully ->
                    if (addSuccessfully) reset = false
                }, Functions.ON_ERROR_MISSING, Action {
                    if (reset) {
                        val effectRows = ResolverHelper.INSTANCE.resetAllLocalPhotoUsedFlag()
                        Log.w(TAG, "rescanAndResetUsedFlag() called when no usable photo return and reset $effectRows photos USED flag")
                    }
                    if (afterFunc != null) afterFunc()
                    sendLocalBroadcast(Intent(ACTION_LOCAL_PHOTO_DIRS_ITERATE_DONE))
                })
            }
        }
    }
}