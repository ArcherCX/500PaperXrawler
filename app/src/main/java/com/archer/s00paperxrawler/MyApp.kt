package com.archer.s00paperxrawler

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager

fun getMyAppCtx(): Context = MyApp.AppCtx
fun getMyString(id: Int, vararg args: String): String = getMyAppCtx().getString(id, *args)
fun getLocalBroadcastManager(): LocalBroadcastManager = LocalBroadcastManager.getInstance(getMyAppCtx())

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
    }
}