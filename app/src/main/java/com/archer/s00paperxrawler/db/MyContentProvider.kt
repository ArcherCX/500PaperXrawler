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
import com.archer.s00paperxrawler.utils.prefs

private typealias Segment = PaperInfoContract.PathSegment
private typealias PaperColumns = PaperInfoContract.Columns

class MyContentProvider : ContentProvider() {

    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val pairPhotoDetail = Pair(Segment.PHOTO_DETAIL, 0)
        private val pairUnusedPhotos = Pair(Segment.UNUSED_PHOTOS, 1)
        private val pairUndownloadPhotos = Pair(Segment.UNDOWNLOAD_PHOTOS, 2)
        private val pairPhotoPath = Pair(Segment.PHOTO_PATH, 3)

        private fun UriMatcher.addURI(pair:Pair<String,Int>) {
            addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, pair.first, pair.second)
        }

        init {
            uriMatcher.addURI(pairPhotoDetail)
            uriMatcher.addURI(pairUnusedPhotos)
            uriMatcher.addURI(pairUndownloadPhotos)
            uriMatcher.addURI(pairPhotoPath)
        }
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val db = getReadableDB()
        when (uriMatcher.match(uri)) {
            pairUnusedPhotos.second -> return db.query(VIEWS.VIEW_UNUSED_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder)
            pairUndownloadPhotos.second -> return db.query(VIEWS.VIEW_UNDOWNLOAD_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder, "${prefs().maxCacheSize}")
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val db = getWritableDB()
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
        val db = getWritableDB()
        var effect = 0
        when (uriMatcher.match(uri)) {
            pairPhotoPath.second ->{
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
