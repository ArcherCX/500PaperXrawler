package com.archer.s00paperxrawler.db

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.archer.s00paperxrawler.MyApp

typealias PaperInfoColumns = PaperInfoContract.Columns
typealias TABLES = TableViews.TABLES
typealias VIEWS = TableViews.VIEWS

private const val DB_NAME = "paper_info.db"
private const val DB_VERSION = 1

fun getWritableDB(): SQLiteDatabase {
    return DBHelper.Singleton.instance.writableDatabase
}

fun getReadableDB(): SQLiteDatabase {
    return DBHelper.Singleton.instance.readableDatabase
}

/**
 * Created by Chen Xin on 2018/8/15.
 * 数据库操作类
 */
class DBHelper private constructor() : SQLiteOpenHelper(MyApp.AppCtx, DB_NAME, null, DB_VERSION) {
    object Singleton {
        val instance = DBHelper()
    }

    companion object {
        private const val SQL_CREATE_PAPER_INFO_TABLE =
                "CREATE TABLE IF NOT EXISTS ${TABLES.PAPER_INFO} (" +
                        "${BaseColumns._ID}                     INTEGER PRIMARY KEY," +
                        "${PaperInfoColumns.PHOTO_DETAIL_URL}   VARCHAR(200) UNIQUE," +
                        "${PaperInfoColumns.ASPECT_RATIO}       REAL," +
                        "${PaperInfoColumns.PHOTO_URL}          TEXT," +
                        "${PaperInfoColumns.PHOTO_NAME}         VARCHAR(50)," +
                        "${PaperInfoColumns.PHOTO_ID}           INTEGER," +
                        "${PaperInfoColumns.PH}                 VARCHAR(50)," +
//                        "${PaperInfoColumns.FILE_NAME}          VARCHAR(20)," +
                        "${PaperInfoColumns.USED}               INTEGER DEFAULT 0," +
                        "${PaperInfoColumns.DOWNLOAD}           INTEGER DEFAULT 0," +
                        "${PaperInfoColumns.SETTLED_DATE}       INTEGER)" +
                        ";"

        private const val SQL_CREATE_UNUSED_PHOTOS_VIEW =
                "CREATE VIEW IF NOT EXISTS ${VIEWS.VIEW_UNUSED_PHOTOS} AS SELECT " +
                        "${BaseColumns._ID}, " +
                        "${PaperInfoColumns.PHOTO_DETAIL_URL}," +
                        "${PaperInfoColumns.ASPECT_RATIO}, " +
                        "${PaperInfoColumns.PHOTO_ID}, " +
                        "${PaperInfoColumns.PHOTO_URL} " +
                        "FROM ${TABLES.PAPER_INFO} " +
                        "WHERE ${PaperInfoColumns.USED} == 0 AND ${PaperInfoColumns.DOWNLOAD} == 1"

        private const val SQL_CREATE_HISTORY_VIEW =
                "CREATE VIEW IF NOT EXISTS ${VIEWS.VIEW_HISTORY} AS SELECT " +
                        "${BaseColumns._ID}, " +
                        "${PaperInfoColumns.PHOTO_DETAIL_URL}," +
                        "${PaperInfoColumns.ASPECT_RATIO}, " +
                        "${PaperInfoColumns.SETTLED_DATE}," +
                        "${PaperInfoColumns.PH}, " +
                        "${PaperInfoColumns.PHOTO_ID}, " +
                        "${PaperInfoColumns.PHOTO_NAME} " +
                        "FROM ${TABLES.PAPER_INFO} " +
                        "WHERE ${PaperInfoColumns.USED} == 1 " +
                        "ORDER BY ${PaperInfoColumns.SETTLED_DATE} DESC"

        private const val SQL_CREATE_UNDOWNLOAD_PHOTOS_VIEW =
                "CREATE VIEW IF NOT EXISTS ${VIEWS.VIEW_UNDOWNLOAD_PHOTOS} AS SELECT " +
                        "${BaseColumns._ID}, " +
                        "${PaperInfoColumns.PHOTO_URL}," +
                        "${PaperInfoColumns.PHOTO_ID} " +
                        "FROM ${TABLES.PAPER_INFO} " +
                        "WHERE ${PaperInfoColumns.DOWNLOAD} == 0"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_PAPER_INFO_TABLE)
        db?.execSQL(SQL_CREATE_UNUSED_PHOTOS_VIEW)
        db?.execSQL(SQL_CREATE_UNDOWNLOAD_PHOTOS_VIEW)
        db?.execSQL(SQL_CREATE_HISTORY_VIEW)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

}