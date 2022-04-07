package com.archer.s00paperxrawler

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File
import kotlin.system.exitProcess

fun getMyAppCtx(): Context = MyApp.AppCtx
fun getMyString(id: Int, vararg args: String): String = getMyAppCtx().getString(id, *args)
private fun getLocalBroadcastManager(): LocalBroadcastManager =
    LocalBroadcastManager.getInstance(getMyAppCtx())

fun sendLocalBroadcast(intent: Intent) = getLocalBroadcastManager().sendBroadcast(intent)
fun registerLocalBCR(rec: BroadcastReceiver, filter: IntentFilter) =
    getLocalBroadcastManager().registerReceiver(rec, filter)

fun unregisterLocalBCR(rec: BroadcastReceiver) = getLocalBroadcastManager().unregisterReceiver(rec)

/**
 * Created by Chen Xin on 2018/8/10.
 */
class MyApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var AppCtx: Context
    }

    override fun onCreate() {
        super.onCreate()
        AppCtx = applicationContext
        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                File(
                    getExternalFilesDir(null),
                    "${getMyString(R.string.app_name)}_crash.log"
                ).printWriter().use {
                    e.printStackTrace(it)
                }
                exitProcess(0)
            }
        }
    }
}