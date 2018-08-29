package com.archer.s00paperxrawler.db

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.text.TextUtils
import com.archer.s00paperxrawler.BuildConfig

private typealias Segment = PaperInfoContract.PathSegment

class MyContentProvider : ContentProvider() {

    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val pairUnusedPhotos = Pair(Segment.UNUSED_PHOTOS, 0)
        private val pairUndownloadPhotos = Pair(Segment.UNDOWNLOAD_PHOTOS, 1)
        private val pairPaperInfo = Pair(Segment.PAPER_INFO, 2)

        private fun UriMatcher.addURI(vararg pairs: Pair<String, Int>) {
            for (pair in pairs) {
                addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, pair.first, pair.second)
            }
        }

        init {
            uriMatcher.addURI(
                    pairUnusedPhotos,
                    pairUndownloadPhotos,
                    pairPaperInfo
            )
        }
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val db = getReadableDB()
        when (uriMatcher.match(uri)) {
            pairUnusedPhotos.second -> return db.query(VIEWS.VIEW_UNUSED_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder)
            pairUndownloadPhotos.second -> return db.query(VIEWS.VIEW_UNDOWNLOAD_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder)
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val db = getWritableDB()
        var id = 0L
        when (uriMatcher.match(uri)) {
            pairPaperInfo.second -> id = db.insertWithOnConflict(TABLES.PAPER_INFO, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
        if (id < 0) {
            return null
        }
        return ContentUris.withAppendedId(uri, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        val db = getWritableDB()
        var effect = 0
        when (uriMatcher.match(uri)) {
            pairPaperInfo.second -> {
                effect = db.update(TABLES.PAPER_INFO, values, selection, selectionArgs)
            }
        }
        return effect
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        TODO("Implement this to handle requests to delete one or more rows")
    }

    override fun getType(uri: Uri): String? {
        TODO("Implement this to handle requests for the MIME type of the data" +
                "at the given URI")
    }
}
