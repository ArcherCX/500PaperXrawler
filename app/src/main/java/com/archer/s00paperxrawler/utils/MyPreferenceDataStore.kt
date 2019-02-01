package com.archer.s00paperxrawler.utils

import androidx.preference.PreferenceDataStore
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.getMyString

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

    override fun putStringSet(key: String?, values: MutableSet<String>?) {
        when (key) {
            getMyString(R.string.categories_key) -> pref.categories = values!!
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            getMyString(R.string.refresh_interval_key) -> pref.refreshInterval = value
        }
    }

}