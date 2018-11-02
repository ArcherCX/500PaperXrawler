package com.archer.s00paperxrawler.service

import android.database.Cursor
import android.provider.BaseColumns
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.text.TextUtils
import android.util.Log
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.gl.GLRenderer
import com.archer.s00paperxrawler.gl.OpenGLES2WallpaperService
import com.archer.s00paperxrawler.utils.getLegacyApiUri
import com.archer.s00paperxrawler.utils.getLoadUri
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "LiveWallService"

class LiveWallService : OpenGLES2WallpaperService() {

    private lateinit var disposable: Disposable

    /**加载图片信息次数统计，防止该方法过时后无限循环加载*/
    private var loadInfoCount = 0

    private val myRender by lazy { MyRenderer() }

    override fun getNewRenderer(): GLRenderer {
        return myRender
    }

    override fun getRenderMode() = GLEngine.RENDERMODE_WHEN_DIRTY

    override fun onCreateEngine(): Engine = MyEngine().also { init() }

    private fun init() {
        if (!::disposable.isInitialized && ResolverHelper.INSTANCE.shouldLoadMoreInfo()) {
            disposable = startLoadInfo()
        }
    }

    /**开始加载图片信息*/
    private fun startLoadInfo(): Disposable {
        return Observable.create<String> { emitter: ObservableEmitter<String> ->
            val csrfToken = prefs().csrfToken
            if (TextUtils.isEmpty(csrfToken)) {
                val request = Request.Builder().url(getLoadUri()).build()
                val response = okClient.newCall(request).execute()
                if (response.body() == null) {
                    emitter.onError(Exception("CSRF Token request failed with null response body"))
                    return@create
                } else {
                    val ret = response.body()?.string()!!
                    val start = "<meta name=\"csrf-token\" content=\""
                    val startIndex = ret.indexOf(start) + start.length
                    val csrf = ret.substring(startIndex, ret.indexOf('"', startIndex))
                    prefs().csrfToken = csrf
                    emitter.onNext(csrf)
                }
            } else emitter.onNext(csrfToken)
            emitter.onComplete()
        }.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).map { token ->
            val builder = Request.Builder()
            val request = builder.url(getLegacyApiUri(1))
                    .header("X-CSRF-Token", token)
                    .build()
            return@map okClient.newCall(request).execute()
        }.observeOn(Schedulers.computation()).map { response ->
            when (response.code()) {
                200 -> {
                    loadInfoCount = 0
                    val json = response.body()?.string()
                    if (!TextUtils.isEmpty(json)) {
                        val jsObject = JSONObject(json)
                        val array = jsObject.optJSONArray("photos")
                        if (array != null) {
                            val length = array.length() - 1
                            for (i in 0..length) {
                                val photo = array.optJSONObject(i)
                                val detailUrl = photo.optString("url")
                                val photoId = photo.optLong("id", -1L)
                                val photoName = photo.optString("name")
                                val images = photo.optJSONArray("images").takeIf { it != null && it.length() > 0 }
                                val photoURL = images?.let {
                                    val img = it.optJSONObject(0)
                                    return@let img.optString("https_url", null)
                                            ?: img.optString("url")
                                } ?: ""
                                val ph = photo.optJSONObject("user")?.run {
                                    var ph = "${optString("firstname")} ${optString("lastname")}"
                                    if (TextUtils.isEmpty(ph.trim())) ph = optString("fullname")
                                    return@run ph
                                } ?: ""
                                val w = photo.optInt("width", -1)
                                val h = photo.optInt("height", -1)
                                val aspect = if (w > 0 && h > 0) w.toFloat() / h else -1F
                                ResolverHelper.INSTANCE.addPhotoInfo(detailUrl, photoId, photoName, photoURL, ph, aspect)
                                DownloadService.startPhotosDownload()
                            }
                        } else {
                            throw Exception("API Json Response Format Changed, Can't Find \"photos\" JsonArray")
                        }
                    } else {
                        throw Exception("Not get photos info from 500px API")
                    }
                }
                401 -> {
                    prefs().csrfToken = ""
                    loadInfoCount++
                    if (loadInfoCount > 3) {
                        val token = response.request().header("X-CSRF-Token")
                        throw Exception("API Request Authentication Error, Http Status Code : ${response.code()} , csrf token = $token ")
                    } else {
                        startLoadInfo()
                    }
                }
                else -> {
                    throw Exception("API Request Error, Http Status Code : ${response.code()}")
                }
            }
        }.retry(3).subscribe({}, {
            Log.e(TAG, "startLoadInfo: onError", it)
        })
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        if (::disposable.isInitialized && !disposable.isDisposed) disposable.dispose()
    }

    inner class MyEngine : OpenGLES2Engine(), Loader.OnLoadCompleteListener<Cursor> {
        private val loader: CursorLoader = CursorLoader(this@LiveWallService, PaperInfoContract.UNUSED_PHOTOS_URI, arrayOf(PaperInfoColumns.PHOTO_ID, PaperInfoColumns.ASPECT_RATIO), null, null, "${BaseColumns._ID} LIMIT 1")
        private lateinit var timer: Disposable

        init {
            loader.registerListener(1, this)
            loader.startLoading()
        }

        override fun onLoadComplete(loader: Loader<Cursor>, data: Cursor?) {
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
            myRender.picPath = "${prefs().photosCachePath}/$photoId"
        }

        private fun refreshWallpaper() {
            timer = Observable.timer(prefs().updateInterval, TimeUnit.SECONDS).subscribe {
                val unusedPhotos = ResolverHelper.INSTANCE.getUnusedPhotos(1)
                unusedPhotos.use { cur ->
                    cur.moveToNext()
                    val id = cur.getLong(cur.getColumnIndex(PaperInfoColumns.PHOTO_ID))
                    val aspect = cur.getFloat(cur.getColumnIndexOrThrow(PaperInfoColumns.ASPECT_RATIO))
                    startDrawPaper(id, aspect)
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
