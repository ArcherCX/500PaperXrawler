package com.archer.s00paperxrawler.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.archer.s00paperxrawler.MyApp
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.prefs
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "DownloadService"

public val okClient: OkHttpClient =
        OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()

class DownloadService : IntentService("DownloadService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PHOTOS_DOWNLOAD -> handlePhotosDownload()
        }
    }

    private fun handlePhotosDownload() {
        if (prefs().isCacheFull) return
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
            Log.d(TAG, "handlePhotosDownload() called response code = ${response.code()}")
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
        }.subscribe()
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
