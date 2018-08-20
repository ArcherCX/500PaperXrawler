package com.archer.s00paperxrawler.db

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.archer.s00paperxrawler.MyApp

private typealias PaperInfoColumns = PaperInfoContract.Columns

private const val DB_NAME = "paper_info.db"
private const val DB_VERSION = 1

object TABLES {
    const val PAPER_INFO = PaperInfoContract.TABLE_NAME
}

private const val SQL_CREATE_PAPER_INFO_TABLE =
        "CREATE TABLE IF NOT EXISTS ${TABLES.PAPER_INFO} (" +
                "${BaseColumns._ID}                     INTEGER PRIMARY KEY," +
                "${PaperInfoColumns.PHOTO_DETAIL_URL}   VARCHAR(200) UNIQUE," +
                "${PaperInfoColumns.ASPECT_RATIO}       REAL," +
                "${PaperInfoColumns.PHOTO_URL}          TEXT);"
/**
 * Created by Chen Xin on 2018/8/15.
 * 数据库操作类
 */
class DBHelper private constructor(): SQLiteOpenHelper(MyApp.AppCtx, DB_NAME, null, DB_VERSION) {
    object Singleton {
        val instance = DBHelper()
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_PAPER_INFO_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

}