package com.archer.s00paperxrawler.db

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.archer.s00paperxrawler.db.PaperInfoContract.DB_VALUE_CONSTANT.FALSE
import com.archer.s00paperxrawler.db.PaperInfoContract.DB_VALUE_CONSTANT.TRUE
import com.archer.s00paperxrawler.getMyAppCtx

typealias PaperInfoColumns = PaperInfoContract.Columns
typealias TABLES = PaperInfoContract.Tables
typealias VIEWS = PaperInfoContract.Views

private const val DB_NAME = "paper_info.db"
private const val DB_VERSION = 2

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
class DBHelper private constructor() : SQLiteOpenHelper(getMyAppCtx(), DB_NAME, null, DB_VERSION) {
    object Singleton {
        val instance = DBHelper()
    }

    companion object {
        private const val SQL_CREATE_PAPER_INFO_TABLE =
                "CREATE TABLE IF NOT EXISTS ${TABLES.TABLE_PAPER_INFO} (" +
                        "${BaseColumns._ID}                     INTEGER PRIMARY KEY," +
                        "${PaperInfoColumns.PHOTO_DETAIL_URL}   VARCHAR(200) UNIQUE," +
                        "${PaperInfoColumns.ASPECT_RATIO}       REAL," +
                        "${PaperInfoColumns.PHOTO_URL}          TEXT," +
                        "${PaperInfoColumns.PHOTO_NAME}         VARCHAR(50)," +
                        "${PaperInfoColumns.PHOTO_ID}           INTEGER," +
                        "${PaperInfoColumns.PH}                 VARCHAR(50)," +
//                        "${PaperInfoColumns.FILE_NAME}          VARCHAR(20)," +
                        "${PaperInfoColumns.USED}               INTEGER DEFAULT $FALSE," +
                        "${PaperInfoColumns.DOWNLOAD}           INTEGER DEFAULT $FALSE," +
                        "${PaperInfoColumns.NSFW}               INTEGER DEFAULT $FALSE," +
                        "${PaperInfoColumns.SETTLED_DATE}       INTEGER)" +
                        ";"

        private const val SQL_CREATE_UNUSED_PHOTOS_VIEW =
                "CREATE VIEW IF NOT EXISTS ${VIEWS.VIEW_UNUSED_PHOTOS} AS SELECT " +
                        "${BaseColumns._ID}, " +
                        "${PaperInfoColumns.PHOTO_DETAIL_URL}," +
                        "${PaperInfoColumns.ASPECT_RATIO}, " +
                        "${PaperInfoColumns.PHOTO_ID}, " +
                        "${PaperInfoColumns.PHOTO_URL}, " +
                        "${PaperInfoColumns.NSFW} " +
                        "FROM ${TABLES.TABLE_PAPER_INFO} " +
                        "WHERE ${PaperInfoColumns.USED} == $FALSE AND ${PaperInfoColumns.DOWNLOAD} == $TRUE"

        private const val SQL_CREATE_HISTORY_VIEW =
                "CREATE VIEW IF NOT EXISTS ${VIEWS.VIEW_HISTORY} AS SELECT " +
                        "${BaseColumns._ID}, " +
                        "${PaperInfoColumns.PHOTO_DETAIL_URL}," +
                        "${PaperInfoColumns.ASPECT_RATIO}, " +
                        "${PaperInfoColumns.SETTLED_DATE}," +
                        "${PaperInfoColumns.PH}, " +
                        "${PaperInfoColumns.PHOTO_ID}, " +
                        "${PaperInfoColumns.NSFW}, " +
                        "${PaperInfoColumns.PHOTO_NAME} " +
                        "FROM ${TABLES.TABLE_PAPER_INFO} " +
                        "WHERE ${PaperInfoColumns.USED} == $TRUE " +
                        "ORDER BY ${PaperInfoColumns.SETTLED_DATE} DESC"

        private const val SQL_CREATE_UNDOWNLOAD_PHOTOS_VIEW =
                "CREATE VIEW IF NOT EXISTS ${VIEWS.VIEW_UNDOWNLOAD_PHOTOS} AS SELECT " +
                        "${BaseColumns._ID}, " +
                        "${PaperInfoColumns.PHOTO_URL}, " +
                        "${PaperInfoColumns.NSFW}, " +
                        "${PaperInfoColumns.PHOTO_ID} " +
                        "FROM ${TABLES.TABLE_PAPER_INFO} " +
                        "WHERE ${PaperInfoColumns.DOWNLOAD} == $FALSE"

        private const val SQL_CREATE_LOCAL_PHOTOS_INFO =
                "CREATE TABLE IF NOT EXISTS ${TABLES.TABLE_LOCAL_PAPER_INFO} (" +
                        "${BaseColumns._ID}                     INTEGER PRIMARY KEY," +
                        "${PaperInfoColumns.PHOTO_NAME}         VARCHAR(100)," +
                        "${PaperInfoColumns.LOCAL_FILE_OWNER}   INTEGER," +
                        "${PaperInfoColumns.IS_DIR}             INTEGER DEFAULT $FALSE," +
                        "${PaperInfoColumns.LOCAL_PHOTO_URI}         VARCHAR UNIQUE," +
                        "${PaperInfoColumns.USED}               INTEGER DEFAULT $FALSE" +
                        ");"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_PAPER_INFO_TABLE)
        db?.execSQL(SQL_CREATE_UNUSED_PHOTOS_VIEW)
        db?.execSQL(SQL_CREATE_UNDOWNLOAD_PHOTOS_VIEW)
        db?.execSQL(SQL_CREATE_HISTORY_VIEW)
        db?.execSQL(SQL_CREATE_LOCAL_PHOTOS_INFO)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) db?.execSQL(SQL_CREATE_LOCAL_PHOTOS_INFO)
    }

}