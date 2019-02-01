package com.archer.s00paperxrawler.view

import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import com.archer.s00paperxrawler.R
import com.archer.s00paperxrawler.getMyAppCtx
import java.util.*

/**
 * Created by Chen Xin on 2019/1/21.
 * Set Preference's summary depends on it's new selected value
 */
fun ListPreference.setNewSummary(newValue: Any?) {
    if (newValue is String) summary = entries[findIndexOfValue(newValue)]
}

fun MultiSelectListPreference.setNewSummary(newValues: Any?) {
    if (newValues !is Set<*>) return
    val summary = StringBuilder()
    newValues.iterator().let {
        while (it.hasNext()) {
            val next = it.next() as String
            val idx = Arrays.binarySearch(entryValues, next)
            if (idx < 0) {//由于最后一个Uncategorized不是按照自然顺序的，binarySearch无法找到，需要额外对比
                val lastIdx = entryValues.size - 1
                if (entryValues[lastIdx] == next) {
                    summary.append("${entries[lastIdx]} , ")
                }
            } else summary.append("${entries[idx]} , ")
        }
    }
    this.summary = if (summary.isNotEmpty()) summary.substring(0, summary.length - 3) else getMyAppCtx().getString(R.string.default_category)
}
