package com.archer.s00paperxrawler

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

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