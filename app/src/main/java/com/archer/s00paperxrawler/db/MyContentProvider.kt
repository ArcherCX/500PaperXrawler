package com.archer.s00paperxrawler.db

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.archer.s00paperxrawler.BuildConfig

private typealias Segment = PaperInfoContract.PathSegment
typealias DB_CONSTANT = PaperInfoContract.DB_VALUE_CONSTANT

private const val TAG = "MyContentProvider"

class MyContentProvider : ContentProvider() {

    @Suppress("SameParameterValue")
    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val pairUnusedPhotos = Pair(Segment.UNUSED_PHOTOS, 0)
        private val pairUndownloadPhotos = Pair(Segment.UNDOWNLOAD_PHOTOS, 1)
        private val pairPaperInfo = Pair(Segment.PAPER_INFO, 2)
        private val pairHistory = Pair(Segment.PAPER_HISTORY, 3)
        private val pairLocalPhotoInfo = Pair(Segment.LOCAL_PHOTO_INFO, 4)

        private fun UriMatcher.addURI(vararg pairs: Pair<String, Int>) {
            for (pair in pairs) {
                addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, pair.first, pair.second)
            }
        }

        init {
            uriMatcher.addURI(
                    pairUnusedPhotos,
                    pairUndownloadPhotos,
                    pairPaperInfo,
                    pairHistory,
                    pairLocalPhotoInfo
            )
        }
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val db = getReadableDB()
        when (uriMatcher.match(uri)) {
            pairPaperInfo.second -> return db.query(TABLES.TABLE_PAPER_INFO, projection, selection, selectionArgs, null, null, sortOrder)
            pairUnusedPhotos.second -> {
                return db.query(VIEWS.VIEW_UNUSED_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder)
                        .apply { setNotificationUri(context?.contentResolver, PaperInfoContract.URI.UNUSED_PHOTOS_URI) }
            }
            pairUndownloadPhotos.second -> return db.query(VIEWS.VIEW_UNDOWNLOAD_PHOTOS, projection, selection, selectionArgs, null, null, sortOrder)
            pairLocalPhotoInfo.second -> return db.query(TABLES.TABLE_LOCAL_PAPER_INFO, projection, selection, selectionArgs, null, null, sortOrder).apply {
                setNotificationUri(context?.contentResolver, PaperInfoContract.URI.LOCAL_PHOTO_INFO_URI)
            }
            pairHistory.second -> return db.query(VIEWS.VIEW_HISTORY, projection, selection, selectionArgs, null, null, sortOrder)
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = getWritableDB()
        var id = 0L
        when (uriMatcher.match(uri)) {
            pairPaperInfo.second -> id = db.insertWithOnConflict(
                TABLES.TABLE_PAPER_INFO,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            pairLocalPhotoInfo.second -> {
                id = db.insertWithOnConflict(
                    TABLES.TABLE_LOCAL_PAPER_INFO,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
                if (id > 0) context?.contentResolver?.notifyChange(
                    PaperInfoContract.URI.LOCAL_PHOTO_INFO_URI,
                    null
                )
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
            pairUndownloadPhotos.second -> {
                effect = db.update(TABLES.TABLE_PAPER_INFO, values, selection, selectionArgs)
                context?.contentResolver?.notifyChange(PaperInfoContract.URI.UNUSED_PHOTOS_URI, null)
            }
            pairUnusedPhotos.second -> effect = db.update(TABLES.TABLE_PAPER_INFO, values, selection, selectionArgs)
            pairLocalPhotoInfo.second -> effect = db.update(TABLES.TABLE_LOCAL_PAPER_INFO, values, selection, selectionArgs)

        }
        return effect
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = getWritableDB()
        when (uriMatcher.match(uri)) {
            pairUnusedPhotos.second -> return db.delete(TABLES.TABLE_PAPER_INFO,
                    "${PaperInfoColumns.USED} == ${DB_CONSTANT.FALSE}", null)
            pairHistory.second -> return db.delete(TABLES.TABLE_PAPER_INFO, "${PaperInfoColumns.USED} == ${DB_CONSTANT.TRUE}", null)
            pairUndownloadPhotos.second -> return db.delete(TABLES.TABLE_PAPER_INFO, "${PaperInfoColumns.DOWNLOAD} == ${DB_CONSTANT.FALSE}", null)
            pairLocalPhotoInfo.second -> {
                val delete = db.delete(TABLES.TABLE_LOCAL_PAPER_INFO, selection, selectionArgs)
                if (delete > 0) context?.contentResolver?.notifyChange(PaperInfoContract.URI.LOCAL_PHOTO_INFO_URI, null)
            }
        }
        return 0
    }

    override fun getType(uri: Uri): String? {
        TODO("Implement this to handle requests for the MIME type of the data" +
                "at the given URI")
    }
}
