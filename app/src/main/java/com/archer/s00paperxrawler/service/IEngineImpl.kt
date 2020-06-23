package com.archer.s00paperxrawler.service

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.archer.s00paperxrawler.sendLocalBroadcast

/**
 * Created by Chen Xin on 2020/6/19.
 * Engine Real Operation Interface
 */
interface IEngineImpl : Loader.OnLoadCompleteListener<Cursor> {
    val loader: CursorLoader
    fun init()
    fun onDestroy()
    fun onPreDrawPaper(intent: Intent) {
        intent.action = ACTION_DRAW_PAPER
        sendLocalBroadcast(intent)
    }

    fun onPostDrawPaper(extras: Bundle) = Unit

    fun onRefreshWallpaper()
}