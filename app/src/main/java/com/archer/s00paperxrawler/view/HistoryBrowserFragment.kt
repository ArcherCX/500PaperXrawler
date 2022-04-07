package com.archer.s00paperxrawler.view

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Parcelable
import android.provider.BaseColumns
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.ListFragment
import com.archer.s00paperxrawler.ACTIVITY_ACTION_HISTORY_DETAIL
import com.archer.s00paperxrawler.MainActivity
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.prefs
import com.bumptech.glide.Glide
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

private const val TAG = "HistoryBrowserFragment"

/**
 * Created by Chen Xin on 2020/6/25.
 */
class HistoryBrowserFragment : ListFragment() {
    private lateinit var cursorAdapter: SimpleCursorAdapter
    private lateinit var historyCursor: Cursor
    private lateinit var listviewState: Parcelable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!::cursorAdapter.isInitialized) {
            cursorAdapter = SimpleCursorAdapter(
                requireContext(), R.layout.history_item_layout, null,
                arrayOf(PaperInfoColumns.PHOTO_NAME, PaperInfoColumns.PH, PaperInfoColumns.PHOTO_ID),
                intArrayOf(R.id.photo_name_tv, R.id.photographer_tv, R.id.thumbnail_iv), 0
            ).apply {
                viewBinder = MyViewBinder()
            }
        }
        if (!::historyCursor.isInitialized) {
            Observable.create<Cursor> {
                historyCursor = ResolverHelper.INSTANCE.getWebHistory(arrayOf(BaseColumns._ID, PaperInfoColumns.PHOTO_NAME, PaperInfoColumns.PH, PaperInfoColumns.PHOTO_ID))
                Log.i(TAG, "onActivityCreated() called history cursor = $historyCursor")
                it.onNext(historyCursor)
                it.onComplete()
            }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).map {
                cursorAdapter.changeCursor(it)
                listAdapter = cursorAdapter
            }.subscribe()
        }
        if (::listviewState.isInitialized) {
            listView.onRestoreInstanceState(listviewState)
        }
    }

    override fun onDestroyView() {
        listviewState = listView.onSaveInstanceState()!!
        super.onDestroyView()
        Log.d(TAG, "onDestroyView() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cursorAdapter.isInitialized) {
            cursorAdapter.changeCursor(null)
        }
    }

    inner class MyViewBinder : SimpleCursorAdapter.ViewBinder {
        override fun setViewValue(view: View?, cursor: Cursor?, columnIndex: Int): Boolean {
            when (view!!.id) {
                R.id.photo_name_tv -> {
                    (view as TextView).text = cursor!!.getString(columnIndex)
                }
                R.id.photographer_tv -> {
                    (view as TextView).text = cursor!!.getString(columnIndex)
                }
                R.id.thumbnail_iv -> {
                    val photo = File("${prefs().photosHistoryPath}/${cursor!!.getLong(columnIndex)}")
                    Glide.with(this@HistoryBrowserFragment).load(photo).placeholder(R.drawable.empty_image_24).thumbnail(0.1f).fitCenter().into(view as ImageView)
                }
                else -> return false
            }
            return true
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        startActivity(Intent(context, MainActivity::class.java).apply {
            action = ACTIVITY_ACTION_HISTORY_DETAIL
            putExtras(Bundle().apply { putInt(EXTRA_PHOTO_POSITION, position) })
        })
    }
}