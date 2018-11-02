package com.archer.s00paperxrawler.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.prefs
import okhttp3.*
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "DownloadService"

val okClient: OkHttpClient =
        OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .cookieJar(object : CookieJar {
                    private val cookieMap = mutableMapOf<String, MutableList<Cookie>>()
                    override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
                        Log.d(TAG, "saveFromResponse() called with: url = [ $url ], cookies = [ $cookies ]")
                        cookieMap[url.host()] = cookies
                    }

                    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
                        val host = url.host()
                        Log.i(TAG, "loadForRequest() called with: host = $host , url = [ $url ]")
                        return when (host) {
                            "api.500px.com" -> cookieMap["500px.com"] ?: mutableListOf()
                            else -> cookieMap[host] ?: mutableListOf()
                        }
                    }
                })
                .build()

class DownloadService : IntentService("DownloadService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PHOTOS_DOWNLOAD -> handlePhotosDownload()
        }
    }

    private fun handlePhotosDownload() {
        if (prefs().isCacheEnough) return
        Log.d(TAG, "handlePhotosDownload() called")
        val builder = Request.Builder()
        val buf = ByteArray(1024)
        val dir = prefs().photosCachePath
        ResolverHelper.INSTANCE.getUndownloadPhotos().map {
            Log.i(TAG, "start download img : ${it.url}")
            val url = builder.url(it.url)
                    .apply { tag(java.lang.Integer::class.java, java.lang.Integer(it.id)) }
                    .apply { tag(java.lang.Long::class.java, java.lang.Long(it.photoId)) }
            return@map okClient.newCall(url.build()).execute()
        }.map { response: Response ->
            Log.d(TAG, "download image response code = ${response.code()}")
            when (response.code()) {
                200 -> {
                    val file = File("$dir/${response.request().tag(java.lang.Long::class.java)}")
                    val byteStream = response.body()?.byteStream()
                    if (byteStream != null) {
                        val outputStream = file.outputStream()
                        outputStream.use {
                            var read = byteStream.read(buf)
                            while (read != -1) {
                                it.write(buf, 0, read)
                                read = byteStream.read(buf)
                            }
                        }
                        val id = response.request().tag(java.lang.Integer::class.java)
                        ResolverHelper.INSTANCE.setPhotoDownloaded(id!!.toInt())
                    }
                }
                404 -> TODO("处理图片已被删除的情况")
            }
        }.doOnComplete { prefs().isCacheEnough = true }.subscribe()
    }

    companion object {
        private const val ACTION_PHOTOS_DOWNLOAD = "com.archer.s00paperxrawler.service.action.PHOTOS_DOWNLOAD"

        /**下载照片*/
        @JvmStatic
        fun startPhotosDownload() {
            val context = MyApp.AppCtx
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PHOTOS_DOWNLOAD
            }
            context.startService(intent)
        }
    }
}
