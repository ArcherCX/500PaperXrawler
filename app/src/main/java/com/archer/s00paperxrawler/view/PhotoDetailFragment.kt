package com.archer.s00paperxrawler.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.archer.s00paperxrawler.MainActivity
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.db.PaperInfoColumns
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.utils.prefs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import kotlin.math.absoluteValue

private const val TAG = "DoubleTapPhotoDetailFra"
const val EXTRAS_WALLPAPER_CHANGE_PERMANENTLY = "extras_wallpaper_change_permanently"

/**
 * Created by Chen Xin on 2020/7/1.
 */
class PhotoDetailFragment : Fragment(R.layout.photo_detail_layout) {
    private lateinit var detailPageUrl: String
    private lateinit var iv: ImageView
    private var scale = 1F
    private lateinit var target: Target<Drawable>
    private var permanent: Boolean = false

    @SuppressLint("ClickableViewAccessibility", "UseRequireInsteadOfGet")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val path = if (prefs().currentMode) {
            onViewCreatedForWebMode(view)
        } else {
            onViewCreatedForLocalMode(view)
        }
        if (arguments != null) {
            permanent = arguments!!.getBoolean(EXTRAS_WALLPAPER_CHANGE_PERMANENTLY)
        }
        if (permanent) {
            prefs().permanentCustomOffsetValue = 0F
        } else {
            prefs().temporarilyCustomOffsetValue = 0F
            prefs().temporarilyEnableCustomOffset = false
        }
        iv = view.findViewById(R.id.detail_photo_iv)
        val gestureDetector = GestureDetector(requireContext(), IvGestureListener())
        iv.setOnTouchListener { _, event ->
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }
        iv.post {
            Log.i(TAG, "onViewCreated: iv.post")
            val finalViewWidth = prefs().wallPaperViewRatio * iv.height
            target = Glide.with(this@PhotoDetailFragment)
                .load(path)
                .into(MyViewTarget(finalViewWidth, iv.height.toFloat(), iv))
            iv.layoutParams = iv.layoutParams.apply { width = finalViewWidth.toInt() }
        }
        if (prefs().isFirstInDoubleTapDetail) {
            prefs().isFirstInDoubleTapDetail = false
            Snackbar.make(
                view,
                R.string.first_in_double_tap_photo_detail_msg,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(android.R.string.ok) {}.show()
        }
    }

    private inner class MyViewTarget(
        val finalViewWidth: Float,
        val finalViewHeight: Float,
        iv: ImageView
    ) : CustomViewTarget<ImageView, Drawable>(iv) {
        override fun onLoadFailed(errorDrawable: Drawable?) {
            Toast.makeText(requireContext(), R.string.double_tap_photo_detail_load_res_failed, Toast.LENGTH_SHORT).show()
        }

        override fun onResourceCleared(placeholder: Drawable?) {
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            Log.d(
                TAG,
                "onResourceReady() called with:vw = ${iv.width}, vh = ${iv.height} w = ${resource.intrinsicWidth}, h = ${resource.intrinsicHeight}, resource = $resource, transition = $transition"
            )
            iv.setImageDrawable(resource)
            val currentPhotoWidth = resource.intrinsicWidth
            val currentPhotoHeight = resource.intrinsicHeight
            val photoRatio = currentPhotoWidth / currentPhotoHeight.toFloat()
            val viewRatio = prefs().wallPaperViewRatio
            scale =
                if (photoRatio < viewRatio) {//scale base width, make photo's width equal to view width
                    finalViewWidth / currentPhotoWidth
                } else {//scale base height, make photo's height equal to view height
                    finalViewHeight / currentPhotoHeight
                }
            iv.imageMatrix = Matrix().apply { setScale(scale, scale) }
        }

    }

    private fun onViewCreatedForWebMode(view: View): String {
        (requireActivity() as MainActivity).requestResetMenu(
            R.menu.history_detail_toolbar_menu,
            ::onOptionsItemSelected
        )
        ResolverHelper.INSTANCE.getCurrentWebPhotoDetail(
            arrayOf(
                PaperInfoColumns.PHOTO_DETAIL_URL,
                PaperInfoColumns.PHOTO_NAME,
                PaperInfoColumns.PH
            )
        ).use {
            it.moveToNext()
            detailPageUrl = "${prefs().baseUri}${it.getString(0)}"
            view.findViewById<TextView>(R.id.detail_photo_name_tv).text = it.getString(1)
            view.findViewById<TextView>(R.id.detail_photographer_tv).text = it.getString(2)
        }
        return "${prefs().photosCachePath}/${prefs().currentPhotoId}"
    }

    private fun onViewCreatedForLocalMode(view: View): String {
        ResolverHelper.INSTANCE.getCurrentLocalPhotoDetail(
            arrayOf(
                PaperInfoColumns.PHOTO_NAME,
                PaperInfoColumns.LOCAL_PHOTO_URI
            )
        ).use {
            it.moveToNext()
            view.findViewById<TextView>(R.id.detail_photo_name_tv).text = it.getString(0)
            return it.getString(1)
        }
    }

    private fun onOptionsItemSelected(menuId: Int): Boolean {
        when (menuId) {
            R.id.action_goto_500px -> {
                if (detailPageUrl.isNotEmpty()) {
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(detailPageUrl)
                    })
                    return true
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::target.isInitialized) {
            Glide.with(this).clear(target)
        }
    }

    private inner class IvGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val pref = prefs()
        private val matrix = Matrix()
        private var oldOffsetX = 0F
        private var oldOffsetY = 0F
        private var minOffsetX = 0F
        private val maxOffsetX = 0F
        private var minOffsetY = 0F
        private val maxOffsetY = 0F
        private var prepareDone = false
        private var adjustX = true
        private var drawableW = 0F
        private var drawableH = 0F
        private var preOffset = if (permanent) pref.permanentCustomOffsetValue else pref.temporarilyCustomOffsetValue
        private var newOffset: Float = 0F

        override fun onDown(e: MotionEvent?): Boolean {
            Log.d(TAG, "onDown() called with: dw = ${iv.drawable.intrinsicWidth}, dh = ${iv.drawable.intrinsicHeight}")
            scrollPrepare(iv.width, iv.height, iv.drawable.intrinsicWidth * scale, iv.drawable.intrinsicHeight * scale)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            Log.i(TAG, "onScroll() called with: distanceX = $distanceX, distanceY = $distanceY\", e1 = $e1, e2 = $e2")
            if (adjustX) {
                var newOffsetX = oldOffsetX - distanceX
                newOffsetX = if (newOffsetX < minOffsetX) minOffsetX else if (newOffsetX > maxOffsetX) maxOffsetX else newOffsetX
                if (newOffsetX != oldOffsetX) {
                    oldOffsetX = newOffsetX
                    iv.imageMatrix = matrix.apply { reset();setTranslate(newOffsetX, 0F);if (scale != 1F) preScale(scale, scale) }
                    if (newOffsetX != preOffset) {
                        newOffset = newOffsetX.absoluteValue / drawableW
                    }
                }
            } else {
                var newOffsetY = oldOffsetY - distanceY
                newOffsetY = if (newOffsetY < minOffsetY) minOffsetY else if (newOffsetY > maxOffsetY) maxOffsetY else newOffsetY
                if (newOffsetY != oldOffsetY) {
                    oldOffsetY = newOffsetY
                    iv.imageMatrix = matrix.apply { reset();setTranslate(0F, newOffsetY);if (scale != 1F) preScale(scale, scale) }
                    if (newOffsetY != pref.temporarilyCustomOffsetValue) {
                        newOffset = newOffsetY.absoluteValue / drawableH
                    }
                }
            }
            if (permanent) pref.permanentCustomOffsetValue = newOffset
            else {
                pref.temporarilyCustomOffsetValue = newOffset
                pref.temporarilyEnableCustomOffset = true
            }
            return true
        }

        private fun scrollPrepare(viewW: Int, viewH: Int, drawableW: Float, drawableH: Float) {
            if (!prepareDone) {
                this.drawableW = drawableW
                this.drawableH = drawableH
                prepareDone = true
                if (drawableW / drawableH < viewW / viewH.toFloat()) {
                    //photo's height is bigger than wallpaper show area, so user can adjust photo's Y coordinate
                    adjustX = false
                    minOffsetY = -(drawableH - viewH)
                } else {
                    //adjust X coordinate
                    adjustX = true
                    minOffsetX = -(drawableW - viewW)
                }
                if (permanent) pref.permanentCustomOffsetAxis = adjustX
                else pref.temporarilyCustomOffsetAxis = adjustX
            }
        }
    }

}