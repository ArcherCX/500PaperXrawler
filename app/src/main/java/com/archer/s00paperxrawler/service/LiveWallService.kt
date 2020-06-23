package com.archer.s00paperxrawler.service

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.archer.s00paperxrawler.gl.GLRenderer
import com.archer.s00paperxrawler.gl.OpenGLES2WallpaperService
import com.archer.s00paperxrawler.registerLocalBCR
import com.archer.s00paperxrawler.unregisterLocalBCR
import com.archer.s00paperxrawler.utils.GestureDetector
import com.archer.s00paperxrawler.utils.prefs
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

private const val TAG = "LiveWallService"

const val ACTION_REFRESH_WALLPAPER = "refresh_wallpaper"
const val ACTION_MONITOR_DB = "monitor_db"
const val ACTION_DRAW_PAPER = "draw_paper"
const val ACTION_MODE_SWITCH = "mode_switch"

const val BUNDLE_EXTRA_KEY_PATH_URI = "path_or_uri"
const val BUNDLE_EXTRA_KEY_PHOTO_ID = "photo_id"
const val INTENT_EXTRA_KEY_REFRESH_INTERVAL = "refresh_interval"

class LiveWallService : OpenGLES2WallpaperService() {
    private val myRender by lazy { MyRenderer() }

    override fun getNewRenderer(): GLRenderer {
        return myRender
    }

    override fun getRenderMode() = GLEngine.RENDERMODE_WHEN_DIRTY

    override fun onCreateEngine(): Engine = MyEngine()

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        prefs().isCurrentWallPaper = false
    }


    inner class MyEngine : OpenGLES2Engine(), GestureDetector.OnThreeTouchListener {
        private lateinit var engineImpl: IEngineImpl
        private lateinit var timer: Disposable
        private lateinit var webEngineImpl: IEngineImpl
        private lateinit var localEngineImpl: IEngineImpl
        private val receiver: BroadcastReceiver
        private val gestureDetector = GestureDetector().apply { onThreeTouchListener = this@MyEngine }

        init {
            val prefs = prefs()
            prefs.isCurrentWallPaper = WallpaperManager.getInstance(applicationContext).wallpaperInfo?.packageName == applicationContext.packageName ?: false
            prefs.currentPage = 1
            resetEngineImpl()
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.action!!.let {
                        when (it) {
                            ACTION_REFRESH_WALLPAPER -> refreshWallpaper(intent.getLongExtra(INTENT_EXTRA_KEY_REFRESH_INTERVAL, 0L))
                            ACTION_DRAW_PAPER -> startDrawPaper(intent.extras!!)
                            ACTION_MODE_SWITCH -> resetEngineImpl()
                        }
                    }
                }
            }
            registerLocalBCR(receiver, IntentFilter().apply {
                addAction(ACTION_REFRESH_WALLPAPER)
                addAction(ACTION_DRAW_PAPER)
                addAction(ACTION_MODE_SWITCH)
            })
        }

        private fun resetEngineImpl() {
            if (prefs().currentMode) {
                if (!::webEngineImpl.isInitialized) {
                    webEngineImpl = WebEngine(this@LiveWallService)
                }
                if (::engineImpl.isInitialized) {
                    if (engineImpl == webEngineImpl) return
                }
                if (::localEngineImpl.isInitialized) localEngineImpl.onDestroy()
                webEngineImpl.init()
                engineImpl = webEngineImpl
            } else {
                if (!::localEngineImpl.isInitialized) {
                    localEngineImpl = LocalEngine(this@LiveWallService)
                }
                if (::engineImpl.isInitialized) {
                    if (engineImpl == localEngineImpl) return
                }
                if (::webEngineImpl.isInitialized) webEngineImpl.onDestroy()
                localEngineImpl.init()
                engineImpl = localEngineImpl
            }
        }


        private fun startDrawPaper(extras: Bundle) {
            queueEvent { myRender.picPath = extras.getString(BUNDLE_EXTRA_KEY_PATH_URI)!! }
            requestRender()
            engineImpl.onPostDrawPaper(extras)
        }

        private fun refreshWallpaper(interval: Long = prefs().refreshInterval.toLong()) {
            Log.d(TAG, "refreshWallpaper() called with: interval = [ $interval ]")
            if (::timer.isInitialized) timer.dispose()
            timer = Observable.timer(interval, TimeUnit.SECONDS).subscribe {
                refreshWallpaper()
                engineImpl.onRefreshWallpaper()
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
        }

        override fun onThreeTouchDown(ev: MotionEvent): Boolean {
//            TODO("解决短时间多次调用的问题")
            refreshWallpaper(0L)
            return true
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterLocalBCR(receiver)
            engineImpl.onDestroy()
            if (::timer.isInitialized && !timer.isDisposed) timer.dispose()
        }
    }
}
