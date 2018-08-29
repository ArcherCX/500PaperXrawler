package com.archer.s00paperxrawler.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.archer.s00paperxrawler.strategy.ICrawlStrategy
import com.archer.s00paperxrawler.strategy.getCrawlStrategy
import com.archer.s00paperxrawler.js.JsCallback
import com.archer.s00paperxrawler.js.JsLog
import com.archer.s00paperxrawler.utils.getLoadUri
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.StringReader
import java.util.*

private const val TAG = "LiveWallService"

class LiveWallService : WallpaperService() {
    private lateinit var webView: WebView

    private lateinit var strategy: ICrawlStrategy

    private lateinit var disposable: Disposable

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateEngine(): Engine {
        init()
        return MyEngine()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init() {
        if (!::strategy.isInitialized) strategy = getCrawlStrategy()
        if (!::webView.isInitialized) {
            webView = WebView(this)
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.blockNetworkImage = true
            JsLog.INSTANCE.add(webView)
            JsCallback.INSTANCE.add(webView)
        }
        if (!::disposable.isInitialized) {
            disposable = Observable.create<String> { emitter: ObservableEmitter<String> ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        Log.e(TAG, "onReceivedError() called with: errorCode = [ $errorCode ], description = [ $description ], failingUrl = [ $failingUrl ]")
                    }

                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                        Log.e(TAG, "onReceivedHttpError() called with: errorResponse = [ statusCode = ${errorResponse?.statusCode} , ${errorResponse?.reasonPhrase} ]")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "onPageFinished() called with: view = [ $view ], url = [ $url ]")
                        webView.evaluateJavascript("document.children[0].innerHTML") { innerHTML ->
                            if (TextUtils.isEmpty(innerHTML)) {
                                emitter.onError(Exception("WebView not loaded anything"))
                            } else {
                                emitter.onNext(innerHTML)
                                emitter.onComplete()
                            }
                        }
                    }
                }
                webView.loadUrl(getLoadUri())
                Log.d(TAG, "after webView.loadUrl: ${webView.visibility == View.VISIBLE}")
            }.subscribeOn(AndroidSchedulers.mainThread()).zipWith(
                    strategy.evaluateJSBeforeParse(),
                    BiFunction<String, String, String> { innerHTML, script ->
                        //                    TODO("可能找不到grid-container,js脚本需要完善")
                        if (Looper.getMainLooper().thread == Thread.currentThread()) webView.evaluateJavascript(script, null)
                        else Handler(Looper.getMainLooper()).post { webView.evaluateJavascript(script, null) }
                        return@BiFunction innerHTML
                    }).observeOn(Schedulers.io()).map {
                //使用Properties类来将转义后的字符恢复回来
                val properties = Properties()
                properties.load(StringReader("key=$it"))
                val ret = properties.getProperty("key")
                val file = File(externalCacheDir, "ret.html")
                file.deleteOnExit()
                file.writeText(ret)
                return@map file
            }.observeOn(Schedulers.computation())
                    .flatMap(strategy.parseHTML())
                    .observeOn(Schedulers.io())
                    .map(strategy.handleResult())
                    .ignoreElements()
                    .retry(2).subscribe()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        if (::webView.isInitialized) webView.stopLoading()
        if (::strategy.isInitialized) strategy.evaluateJSBeforeDestroy(webView)
        if (::disposable.isInitialized && !disposable.isDisposed) disposable.dispose()
        if (::webView.isInitialized) {
            webView.stopLoading()
            JsLog.INSTANCE.remove(webView)
            JsCallback.INSTANCE.remove(webView)
        }

    }

    inner class MyEngine : Engine() {

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "${Thread.currentThread()} onSurfaceChanged() called with: holder = [ $holder ], format = [ $format ], width = [ $width ], height = [ $height ]")
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "onSurfaceDestroyed() called with: holder = [ $holder ]")
        }
    }
}
