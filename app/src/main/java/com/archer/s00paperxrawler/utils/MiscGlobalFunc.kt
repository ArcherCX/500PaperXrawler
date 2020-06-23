package com.archer.s00paperxrawler.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.registerLocalBCR
import com.archer.s00paperxrawler.unregisterLocalBCR
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.net.URLDecoder

private const val TAG = "MiscGlobalFunc"
private var iterating = false
const val ACTION_LOCAL_PHOTO_DIRS_ITERATE_DONE = "iterate_done"

/**
 * Created by Chen Xin on 2020/6/19.
 *
 * Iterate all local photo directories, and need to call [Observable.subscribe] manually, just in case need more operations.
 *
 * Attention! ! ! send [ACTION_LOCAL_PHOTO_DIRS_ITERATE_DONE] broadcast to reset iterate flag
 *
 * @param uri DocumentFile Uri
 */
fun iterateLocalPhotoDir(uri: Uri, insertDirToDB: Boolean = false): Observable<Boolean>? {
    if (iterating) return null
    Log.d(TAG, "iterateLocalPhotoDir() called with: uri = $uri, insertDirToDB = $insertDirToDB")
    iterating = true
    registerLocalBCR(object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            iterating = false
            unregisterLocalBCR(this)
        }
    }, IntentFilter(ACTION_LOCAL_PHOTO_DIRS_ITERATE_DONE))
    val dirDF = DocumentFile.fromTreeUri(MyApp.AppCtx, uri)!!
    val resolver = ResolverHelper.INSTANCE
    var ownerId = resolver.doesDirExist(uri.toString())
    Log.i(TAG, "iterateLocalPhotoDir: does $uri exist: $ownerId")
    if (insertDirToDB && ownerId == -1L) {
        val split = URLDecoder.decode(uri.toString(), "UTF-8").split(":")
        val name = if (split.size == 3) split[2] else dirDF.name
        ownerId = resolver.addLocalPhotoInfo(uri.toString(), name, true)!!.lastPathSegment!!.toLong()
    }
    return Observable.fromArray(*dirDF.listFiles()).observeOn(Schedulers.io()).flatMap {
        if (it.isDirectory) return@flatMap Observable.fromArray(*it.listFiles()) else return@flatMap Observable.just(it)
    }.filter {
        return@filter it.type?.startsWith("image") ?: false
    }.map {
        Log.d(TAG, "Photo Name: ${it.name} && Uri : ${it.uri}")
        return@map resolver.addLocalPhotoInfo(it.uri.toString(), it.name, false, ownerId) != null
    }
}
