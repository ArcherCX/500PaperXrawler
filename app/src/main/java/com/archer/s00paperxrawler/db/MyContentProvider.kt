package com.archer.s00paperxrawler.db

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.text.TextUtils
import android.util.SparseArray
import com.archer.s00paperxrawler.BuildConfig

private typealias Segment = PaperInfoContract.PathSegment
private typealias PaperColumns = PaperInfoContract.Columns

private const val CP_CODE_INSERT_PHOTO_DETAIL = 0

class MyContentProvider : ContentProvider() {
    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val pairPhotoDetail = Pair(Segment.PHOTO_DETAIL, CP_CODE_INSERT_PHOTO_DETAIL)

        init {
            uriMatcher.addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, pairPhotoDetail.first, pairPhotoDetail.second)
        }
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val db = DBHelper.Singleton.instance.writableDatabase
        var id = 0L
        when (uriMatcher.match(uri)) {
            pairPhotoDetail.second -> {
                if (!TextUtils.isEmpty(values.getAsString(PaperColumns.PHOTO_DETAIL_URL)))
                    id = db.insertWithOnConflict(TABLES.PAPER_INFO, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
        if (id < 0) {
            return null
        }
        return ContentUris.withAppendedId(uri, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        TODO("Implement this to handle requests to update one or more rows.")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("Implement this to handle requests to delete one or more rows")
    }

    override fun getType(uri: Uri): String? {
        TODO("Implement this to handle requests for the MIME type of the data" +
                "at the given URI")
    }
}
