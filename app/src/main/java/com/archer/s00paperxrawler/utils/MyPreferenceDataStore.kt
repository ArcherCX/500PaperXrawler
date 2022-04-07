package com.archer.s00paperxrawler.utils

import android.util.Log
import androidx.preference.PreferenceDataStore
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.getMyString

private const val TAG = "MyPreferenceDataStore"

/**
 * Created by Chen Xin on 2019/1/25.
 */
class MyPreferenceDataStore : PreferenceDataStore() {

    private val pref = prefs()

    override fun putString(key: String, value: String?) {
        when (key) {
            getMyString(R.string.feature_key) -> pref.feature = value!!
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        when (key) {
            getMyString(R.string.feature_key) -> pref.feature
        }
        return defValue
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?) {
        Log.d(TAG, "putStringSet() called with: key = $key, values = $values")
        when (key) {
            getMyString(R.string.categories_key) -> pref.categories = values!!
        }
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> {
        when (key) {
            getMyString(R.string.categories_key) -> pref.categories
        }
        return mutableSetOf()
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            getMyString(R.string.refresh_interval_key) -> pref.refreshInterval = value
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        when (key) {
            getMyString(R.string.refresh_interval_key) -> pref.refreshInterval
        }
        return defValue
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
            getMyString(R.string.mode_key) -> pref.currentMode = value
            getMyString(R.string.permanently_enable_custom_offset_key) -> pref.permanentlyEnableCustomOffset = value
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        when (key) {
            getMyString(R.string.mode_key) -> pref.currentMode
            getMyString(R.string.permanently_enable_custom_offset_key) -> pref.permanentlyEnableCustomOffset
        }
        return defValue
    }
}