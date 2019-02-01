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
import com.archer.s00paperxrawler.contract.SettingsContract
import com.archer.s00paperxrawler.getMyString
import com.archer.s00paperxrawler.utils.MyPreferenceDataStore
import com.archer.s00paperxrawler.utils.prefs

private const val TAG = "SettingFragment"

/**
 * Created by Chen Xin on 2019/1/20.
 */
class SettingsFragment : PreferenceFragmentCompat(), SettingsContract.View, Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    override lateinit var presenter: SettingsContract.Presenter
    private val preferenceDataStore = MyPreferenceDataStore()
    private lateinit var dialog: AlertDialog

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference?.key) {
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
        (findPreference(getString(R.string.feature_key)) as ListPreference).let {
            it.setNewSummary(it.value)
            it.onPreferenceChangeListener = this
            it.preferenceDataStore = preferenceDataStore
        }
        (findPreference(getString(R.string.categories_key)) as MultiSelectListPreference).let {
            it.setNewSummary(it.values)
            it.onPreferenceChangeListener = this
            it.preferenceDataStore = preferenceDataStore
        }
        findPreference<SeekBarPreference>(getString(R.string.refresh_interval_key)).let {
            it.summary = getString(R.string.refresh_interval_summary, it.value * 0.5f)
            it.onPreferenceChangeListener = this
            it.preferenceDataStore = preferenceDataStore
        }
//        findPreference<SeekBarPreference>(getString(R.string.storage_cache_key))?.let { it.onPreferenceChangeListener = this }
        findPreference<SwitchPreference>(getString(R.string.download_via_wifi_key)).let {
            it.onPreferenceChangeListener = this
            presenter.onDownloadViaWifiChange(it.isChecked)
        }
        findPreference<Preference>(getString(R.string.clear_cache_key)).let { it.onPreferenceClickListener = this }
        findPreference<Preference>(getString(R.string.open_config_key)).let { it.onPreferenceClickListener = this }
        findPreference<SwitchPreference>(getString(R.string.show_nsfw_key)).let { it.onPreferenceChangeListener = this }
        findPreference<Preference>(getString(R.string.clear_history_key)).let { it.onPreferenceClickListener = this }
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

    override fun toast(content: String, duration: Int) {
        activity?.let { Toast.makeText(it, content, duration).show(); }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CHANGE_LIVE_WALLPAPER) {
            prefs().isCurrentWallPaper = resultCode == Activity.RESULT_OK
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::dialog.isInitialized) {
            dialog.dismiss()
        }
    }
}