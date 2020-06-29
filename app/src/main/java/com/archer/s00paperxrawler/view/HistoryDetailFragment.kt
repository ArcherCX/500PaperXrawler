package com.archer.s00paperxrawler.view

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.archer.s00paperxrawler.MainActivity
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.prefs
import com.bumptech.glide.Glide
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

private const val TAG = "HistoryDetailFragment"

/**
 * Created by Chen Xin on 2020/6/27.
 */
class HistoryDetailFragment(private val initPos: Int) : Fragment(R.layout.history_detail_fragment_layout) {
    private lateinit var cursor: Cursor
    private lateinit var onPageChangeCallback: ViewPager2.OnPageChangeCallback
    private var detailPageUrl = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Observable.create<Cursor> {
            cursor = ResolverHelper.INSTANCE.getWebHistory(arrayOf(BaseColumns._ID, PaperInfoColumns.PHOTO_NAME, PaperInfoColumns.PH, PaperInfoColumns.PHOTO_ID, PaperInfoColumns.PHOTO_DETAIL_URL))
            it.onNext(cursor)
            it.onComplete()
        }.subscribeOn(Schedulers.computation()).map {
            cursor.moveToPosition(initPos)
        }.observeOn(AndroidSchedulers.mainThread()).map {
            val vp = view as ViewPager2
            vp.adapter = MyPagerAdapter()
            vp.post {
                vp.setCurrentItem(initPos, false)
            }
            onPageChangeCallback = MyOnPageChangeCallback().also {
                vp.registerOnPageChangeCallback(it)
            }
            (requireActivity() as MainActivity).requestResetMenu(R.menu.history_detail_toolbar_menu, ::onOptionsItemSelected)
        }.subscribe()
    }

    private fun onOptionsItemSelected(menuId: Int): Boolean {
        when (menuId) {
            R.id.action_goto_500px -> {
                if (detailPageUrl.isNotEmpty()) {
                    startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(detailPageUrl) })
                    return true
                }
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: close cursor")
        (requireActivity() as MainActivity).invalidateMenu()
        if (::cursor.isInitialized) {
            cursor.close()
        }
        if (::onPageChangeCallback.isInitialized) {
            (view as ViewPager2).unregisterOnPageChangeCallback(onPageChangeCallback)
        }
    }

    inner class MyOnPageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            cursor.moveToPosition(position)
            detailPageUrl = "${prefs().baseUri}${cursor.getString(4)}"
        }
    }

    inner class MyPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = cursor.count

        override fun createFragment(position: Int): Fragment {
            Log.i(TAG, "createFragment() called with: position = $position")
            cursor.moveToPosition(position)
            val photoName = cursor.getString(1)
            val photographer = cursor.getString(2)
            val photoPath = "${prefs().photosHistoryPath}/${cursor.getLong(3)}"
            return HistoryDetailItemFragment(photoName, photographer, photoPath)
        }

    }
}

class HistoryDetailItemFragment(val photoName: String, val photographer: String, val photoPath: String) : Fragment(R.layout.history_detail_item_layout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.detail_photo_iv).let {
            Glide.with(this).load(photoPath).into(it)
        }
        view.findViewById<TextView>(R.id.detail_photo_name_tv).text = photoName
        view.findViewById<TextView>(R.id.detail_photographer_tv).text = photographer
    }

}