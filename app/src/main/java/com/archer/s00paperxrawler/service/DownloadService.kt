package com.archer.s00paperxrawler.service

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "DownloadService"
private const val ACTION_DOWNLOAD = "com.archer.s00paperxrawler.service.action.DOWNLOAD"

private val okClient =
        OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()

class DownloadService : IntentService("DownloadService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                handleActionDownload()
            }
        }
    }

    private fun handleActionDownload() {
        val builder = Request.Builder()
        val buf = ByteArray(1024)
        val dir = prefs().defaultCachePath
        ResolverHelper.INSTANCE.getUndownloadPhotos().map {
            Log.i(TAG, "start download img : ${it.second}")
            val url = builder.url(it.second).apply { tag(Int::class.java, it.first) }
            return@map okClient.newCall(url.build()).execute()
        }.map { response: Response ->
            Log.d(TAG, "handleActionDownload() called response code = ${response.code()}")
            val file = File("$dir/${System.currentTimeMillis()}")
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
                val id = response.request().tag(Int::class.java)
                ResolverHelper.INSTANCE.setPhotoPath(id!!, file.name)
            }
        }.subscribe()
    }

    companion object {
        @JvmStatic
        fun startActionDownload(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
            }
            context.startService(intent)
        }
    }
}
