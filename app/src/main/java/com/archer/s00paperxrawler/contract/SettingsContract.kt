package com.archer.s00paperxrawler.contract

import android.content.DialogInterface
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.archer.s00paperxrawler.BasePresenter
import com.archer.s00paperxrawler.BaseView
import com.archer.s00paperxrawler.view.SettingsFragment

const val REQUEST_CODE_CHANGE_LIVE_WALLPAPER = 10
const val REQUEST_CODE_START_IMAGE_PICKER = 11

/**
 * Created by Chen Xin on 2019/1/28.
 */
interface SettingsContract {

    interface View : BaseView<Presenter> {
        /**显示Dialog*/
        fun showDialog(title: String, msg: String, positiveListener: DialogInterface.OnClickListener? = null, negativeListener: DialogInterface.OnClickListener? = null)

        /**显示Toast*/
        fun toast(content: String, duration: Int = Toast.LENGTH_SHORT)

        /**启动系统的图片选择器*/
        fun startImagePicker()

        /**
         * 模式切换后调整界面
         * @param mode 当前的模式，true:web mode, false:local mode
         */
        fun layoutAdjustForModeSwitch(mode: Boolean)
    }

    interface Presenter : BasePresenter {
        /**初始化操作*/
        fun init(fragment: Fragment)

        /**弹出清除缓存的确认Dialog前的准备操作*/
        fun onPrepareClearCacheDialog()

        /**弹出清除历史的确认Dialog前的准备操作*/
        fun onPrepareClearHistoryDialog()

        /**打开动态壁纸设置*/
        fun openLiveWallpaperConfig(fragment: Fragment)

        /**NSFW设置改变*/
        fun onNSFWChange(show: Boolean): Boolean

        /**仅通过Wifi下载设置改变*/
        fun onDownloadViaWifiChange(enable: Boolean)

        /**Feature、Category变化时执行操作*/
        fun onFeatureCategoryChange(preference: Preference?, newValue: Any?)

        /**
         * When Mode Changed
         * @param newMode true:web, false:local
         * @return Can mode allowed to be changed
         */
        fun onModeChange(newMode: Boolean): Boolean

        /**
         * Handle the directory uri returned by the picker
         */
        fun handlePhotoDir(data: Uri)

        /**Visit Github Repo*/
        fun onGithubVisit(fragment: Fragment)

        /**Open History Browser*/
        fun onHistoryBrowse(fragment: Fragment)
    }
}