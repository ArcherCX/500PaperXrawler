package com.archer.s00paperxrawler.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.PaperInfoContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.getLegacyApiUri
import com.archer.s00paperxrawler.utils.getLoadUri
import com.archer.s00paperxrawler.utils.prefs
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

private const val TAG = "DownloadService"

val okClient: OkHttpClient =
        OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .cookieJar(PersistentCookieJar(SetCookieCache(),SharedPrefsCookiePersistor(MyApp.AppCtx)))
                .build()

/**下载时异常*/
private data class DownloadException(val id: Int, val msg: String) : RuntimeException(msg)

/**异常重试次数*/
private const val RETRY_TIMES = 3L

class DownloadService : IntentService("DownloadService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_LOAD_PHOTOS_URL -> handleLoadPhotosUrl()
            ACTION_PHOTOS_DOWNLOAD -> handlePhotosDownload()
        }
    }

    private fun execHandler(e: Throwable) {
        Log.e(TAG, "execHandler: ", e)
        if (e is DownloadException) ResolverHelper.INSTANCE.setPhotoDownloaded(e.id, PaperInfoContract.DB_VALUE_CONSTANT.EXCEPTION)
    }

    @SuppressLint("CheckResult")
    private fun handlePhotosDownload() {
        if (prefs().isCacheEnough) return
        Log.d(TAG, "handlePhotosDownload() called")
        val builder = Request.Builder()
        val buf = ByteArray(1024)
        val dir = prefs().photosCachePath
        ResolverHelper.INSTANCE.getUndownloadPhotosUrl().flatMap { info ->
            return@flatMap Observable.just(info).map {
                Log.i(TAG, "start download img : ${it.url}")
                val request = builder.url(it.url)
                        .apply {
                            tag(java.lang.Integer::class.java, java.lang.Integer(it.id))
                            tag(java.lang.Long::class.java, java.lang.Long(it.photoId))
                        }.build()
                val response = okClient.newCall(request).execute()
                if (response.code() == 200) return@map response else throw DownloadException(it.id, "Exception Occurs When Download Photo With Resp Code ${response.code()} : ${it.url}")
            }.retry(RETRY_TIMES).onErrorResumeNext { e: Throwable ->
                execHandler(e)
                return@onErrorResumeNext Observable.empty<Response>()
            }
        }.flatMap {
            return@flatMap Observable.just(it).map { response: Response ->
                val file = File("$dir/${response.request().tag(java.lang.Long::class.java)}")
                val id = response.request().tag(java.lang.Integer::class.java)!!.toInt()
                response.body()?.byteStream()?.use { source ->
                    val outputStream = file.outputStream()
                    outputStream.use { fos ->
                        var read = 0
                        val loop = { read = source.read(buf);read }
                        while (loop() != -1) {
                            fos.write(buf, 0, read)
                        }
                    }
                    return@map id
                }
                throw DownloadException(id, "Exception Occurs When Read Response ByteStream")
            }.retry(RETRY_TIMES).onErrorResumeNext { e: Throwable ->
                execHandler(e)
                return@onErrorResumeNext Observable.empty<Int>()
            }
        }.subscribe({
            Log.d(TAG, "handlePhotosDownload() called onNext $it")
            ResolverHelper.INSTANCE.setPhotoDownloaded(it)
        }, {
            execHandler(it)
        }, {
            Log.d(TAG, "handlePhotosDownload() onComplete")
            if (ResolverHelper.INSTANCE.shouldLoadMoreInfo()) {
                handleLoadPhotosUrl()
            } else {
                prefs().isCacheEnough = ResolverHelper.INSTANCE.isCacheEnough()
            }
        })
    }

    /**加载更多的图片信息*/
    @SuppressLint("CheckResult")
    private fun handleLoadPhotosUrl() {
        Observable.create<String> { emitter: ObservableEmitter<String> ->
            val csrfToken = prefs().csrfToken
            if (TextUtils.isEmpty(csrfToken)) {
                val request = Request.Builder().url(getLoadUri()).build()
                val response = okClient.newCall(request).execute()
                if (response.body() == null) {
                    emitter.onError(Exception("CSRF Token request failed with null response body"))
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
        }.map { token ->
            val builder = Request.Builder()
            val request = builder.url(getLegacyApiUri(prefs().currentPage))
                    .header("X-CSRF-Token", token)
                    .build()
            return@map okClient.newCall(request).execute()
        }.map { response ->
            when (response.code()) {
                200 -> {
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
                    val token = response.request().header("X-CSRF-Token")
                    throw Exception("API Request Authentication Error, Http Status Code : ${response.code()} , csrf token = $token ")
                }
                else -> {
                    throw Exception("API Request Error, Http Status Code : ${response.code()}")
                }
            }
        }.retry(3).subscribe({}, {
            Log.e(TAG, "startLoadInfo: onError", it)
        }, {
            prefs().currentPage++
            Log.i(TAG, "handleLoadPhotosUrl: currentpage = ${prefs().currentPage}")
        })
    }

    companion object {
        private const val ACTION_LOAD_PHOTOS_URL = "com.archer.s00paperxrawler.service.action.LOAD_PHOTOS_URL"
        private const val ACTION_PHOTOS_DOWNLOAD = "com.archer.s00paperxrawler.service.action.PHOTOS_DOWNLOAD"

        /**下载照片, impl:[handlePhotosDownload]*/
        @JvmStatic
        fun startPhotosDownload() {
            startIntentService(ACTION_PHOTOS_DOWNLOAD)
        }

        /**从500px加载照片信息, impl:[handleLoadPhotosUrl]*/
        @JvmStatic
        fun startLoadPhotosUrl() {
            startIntentService(ACTION_LOAD_PHOTOS_URL)
        }

        @JvmStatic
        private fun startIntentService(action: String) {
            val context = MyApp.AppCtx
            val intent = Intent(context, DownloadService::class.java).apply { this.action = action }
            context.startService(intent)
        }
    }
}
