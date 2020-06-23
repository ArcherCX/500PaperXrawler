package com.archer.s00paperxrawler.view

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.*
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.contract.REQUEST_CODE_CHANGE_LIVE_WALLPAPER
import com.archer.s00paperxrawler.contract.REQUEST_CODE_START_IMAGE_PICKER
import com.archer.s00paperxrawler.contract.SettingsContract
import com.archer.s00paperxrawler.db.ResolverHelper
import com.archer.s00paperxrawler.getMyString
import com.archer.s00paperxrawler.presenter.SettingsPresenter
import com.archer.s00paperxrawler.service.DownloadService
import com.archer.s00paperxrawler.utils.MyPreferenceDataStore
import com.archer.s00paperxrawler.utils.prefs

private const val TAG = "SettingFragment"

/**
 * Created by Chen Xin on 2019/1/20.
 */
class SettingsFragment : PreferenceFragmentCompat(), SettingsContract.View, Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override lateinit var presenter: SettingsContract.Presenter

    init {
        SettingsPresenter(this)
    }

    private val preferenceDataStore = MyPreferenceDataStore()
    private lateinit var dialog: AlertDialog
    private lateinit var modePreference: SwitchPreference
    private lateinit var localPhotoDirPreference: Preference
    private val webModePreferenceSet = mutableSetOf<Preference>()
    private val localModePreferenceSet = mutableSetOf<Preference>()

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference?.key) {
            getString(R.string.mode_key) -> return presenter.onModeChange(newValue as Boolean)
            getString(R.string.feature_key), getString(R.string.categories_key) -> return { presenter.onFeatureCategoryChange(preference, newValue);false }()
            getString(R.string.show_nsfw_key) -> return presenter.onNSFWChange(newValue as Boolean)
            getString(R.string.refresh_interval_key) -> preference.summary = getString(R.string.refresh_interval_summary, newValue as Int * 0.5f)
            getString(R.string.download_via_wifi_key) -> presenter.onDownloadViaWifiChange(newValue as Boolean)
            getString(R.string.storage_cache_key) -> return (preference as MySeekBarPreference).setNewValueByStep(newValue!! as Int)
        }
        return true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        modePreference = findPreference<SwitchPreference>(getString(R.string.mode_key))!!.also {
            it.preferenceDataStore = preferenceDataStore
            it.onPreferenceChangeListener = this
        }
        localPhotoDirPreference = findPreference<Preference>(getString(R.string.photo_dirs_key))!!.also {
            localModePreferenceSet.add(it)
            val localPhotoDirNames = ResolverHelper.INSTANCE.getAllLocalDirNames()?.get(0)
                    ?: emptyList<String>()
            if (localPhotoDirNames.isNotEmpty()) it.summary = localPhotoDirNames.toString()
            it.onPreferenceClickListener = this
        }
        (findPreference<ListPreference>(getString(R.string.feature_key))!!).let {
            webModePreferenceSet.add(it)
            it.preferenceDataStore = preferenceDataStore
            it.setNewSummary(it.value)
            it.onPreferenceChangeListener = this
        }
        (findPreference<MultiSelectListPreference>(getString(R.string.categories_key))!!).let {
            webModePreferenceSet.add(it)
            it.preferenceDataStore = preferenceDataStore
            it.setNewSummary(it.values)
            it.onPreferenceChangeListener = this
        }
        findPreference<SeekBarPreference>(getString(R.string.refresh_interval_key))!!.let {
            it.preferenceDataStore = preferenceDataStore
            it.summary = getString(R.string.refresh_interval_summary, it.value * 0.5f)
            it.onPreferenceChangeListener = this
        }
//        findPreference<SeekBarPreference>(getString(R.string.storage_cache_key))?.let { it.onPreferenceChangeListener = this }
        findPreference<SwitchPreference>(getString(R.string.download_via_wifi_key))!!.let {
            webModePreferenceSet.add(it)
            it.onPreferenceChangeListener = this
            presenter.onDownloadViaWifiChange(it.isChecked)
        }
        findPreference<Preference>(getString(R.string.clear_cache_key))!!.let { it.onPreferenceClickListener = this }
        findPreference<Preference>(getString(R.string.open_config_key))!!.let { it.onPreferenceClickListener = this }
        findPreference<SwitchPreference>(getString(R.string.show_nsfw_key))!!.let {
            webModePreferenceSet.add(it)
            it.onPreferenceChangeListener = this
        }
        findPreference<Preference>(getString(R.string.clear_history_key))!!.let { it.onPreferenceClickListener = this }
        val currentMode = prefs().currentMode
        webModePreferenceSet.forEach { it.isVisible = currentMode }
        localModePreferenceSet.forEach { it.isVisible = !currentMode }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.init(this)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference?.key) {
            getString(R.string.clear_cache_key) -> presenter.onPrepareClearCacheDialog()
            getString(R.string.clear_history_key) -> presenter.onPrepareClearHistoryDialog()
            getString(R.string.open_config_key) -> presenter.openLiveWallpaperConfig(this)
            getString(R.string.photo_dirs_key) -> {
                val nameAndId = ResolverHelper.INSTANCE.getAllLocalDirNames()
                if (nameAndId == null) startImagePicker()
                else AlertDialog.Builder(context).apply {
                    val names = nameAndId[0]
                    val ids = nameAndId[1]
                    setMultiChoiceItems(names.toTypedArray(), null, null)
                    setPositiveButton(R.string.local_photo_dir_dialog_positive_btn) { dialog, _ ->
                        val checkedItemPositions = (dialog as AlertDialog).listView.checkedItemPositions
                        val size = checkedItemPositions.size()
                        if (size == 0) return@setPositiveButton
                        val checkedIds = arrayListOf<String>()
                        for (i in 0..size) {
                            if (checkedItemPositions.valueAt(i)) {
//                                val element = names[checkedItemPositions.keyAt(i)]
                                val idx = checkedItemPositions.keyAt(i)
                                checkedIds.add(ids[idx])
                                names.removeAt(idx)
                            }
                        }
                        ResolverHelper.INSTANCE.deleteLocalDirsAndPhotos(checkedIds)
                        localPhotoDirPreference.summary = if (size < ids.size) names.toString() else ""
                    }
                    setNegativeButton(R.string.local_photo_dir_dialog_negative_btn, null)
                    setNeutralButton(R.string.local_photo_dir_dialog_neutral_btn) { _, _ ->
                        startImagePicker()
                    }
                    show()
                }
            }
        }
        return false
    }

    override fun showDialog(title: String, msg: String, positiveListener: DialogInterface.OnClickListener?, negativeListener: DialogInterface.OnClickListener?) {
        if (!::dialog.isInitialized) {
            dialog = AlertDialog.Builder(context)
                    .setCancelable(true)
                    .setTitle(title)
                    .setMessage(msg).apply {
                        setPositiveButton(android.R.string.ok, positiveListener)
                        setNegativeButton(android.R.string.cancel, negativeListener)
                    }.create().apply { setCanceledOnTouchOutside(true) }
        } else {
            if (dialog.isShowing) return
            dialog.setTitle(title)
            dialog.setMessage(msg)
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, getMyString(android.R.string.ok), positiveListener)
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getMyString(android.R.string.cancel), negativeListener)
        }
        dialog.show()
    }

    override fun startImagePicker() {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }.let { startActivityForResult(it, REQUEST_CODE_START_IMAGE_PICKER) }
    }

    override fun toast(content: String, duration: Int) {
        activity?.let { Toast.makeText(it, content, duration).show(); }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_CHANGE_LIVE_WALLPAPER -> {
                val isOK = resultCode == Activity.RESULT_OK
                prefs().isCurrentWallPaper = isOK
                if (!isOK) DownloadService.cancelDownload()
            }
            REQUEST_CODE_START_IMAGE_PICKER -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        presenter.handlePhotoDir(data.data!!)
                    } else {
                        toast(getString(R.string.selection_error))
                    }
                }
            }
        }
    }

    override fun layoutAdjustForModeSwitch(mode: Boolean) {
        if (modePreference.isChecked != mode) {
            modePreference.isChecked = mode
            webModePreferenceSet.forEach { it.isVisible = mode }
            localModePreferenceSet.forEach { it.isVisible = !mode }
        }
        if (!mode) {
            val localPhotoDirNames = ResolverHelper.INSTANCE.getAllLocalDirNames()?.get(0)
                    ?: emptyList<String>()
            localPhotoDirPreference.summary = if (localPhotoDirNames.isNotEmpty()) localPhotoDirNames.toString() else ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dialog.isInitialized) {
            dialog.dismiss()
        }
    }
}