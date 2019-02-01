package com.archer.s00paperxrawler.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.SeekBar
import androidx.preference.PreferenceViewHolder
import androidx.preference.R
import androidx.preference.SeekBarPreference
import com.archer.s00paperxrawler.getMyAppCtx

private const val TAG = "MySeekBarPreference"

/**
 * Created by Chen Xin on 2019/1/22.
 */
class MySeekBarPreference(ctx: Context, attr: AttributeSet) : SeekBarPreference(ctx, attr) {
    private lateinit var mSeekBar: SeekBar

    override fun onBindViewHolder(view: PreferenceViewHolder?) {
        super.onBindViewHolder(view)
        if (!::mSeekBar.isInitialized) {
            val paddingTopIncrement = getMyAppCtx().resources.getDimensionPixelSize(com.archer.s00paperxrawler.R.dimen.seek_bar_padding_top_increment)
            mSeekBar = (view!!.findViewById(R.id.seekbar) as SeekBar).also {
                it.setPadding(it.paddingLeft, it.paddingTop + paddingTopIncrement, it.paddingRight, it.paddingBottom)
            }
            view!!.findViewById(R.id.seekbar_value).let {
                it.setPadding(it.paddingLeft, it.paddingTop + paddingTopIncrement, it.paddingRight, it.paddingBottom)
            }
        }
    }

    fun setProgress(x: Int) {
        if (::mSeekBar.isInitialized) {
            mSeekBar.progress = x - min
        }
    }

    /**
     * 根据即将保存的新值按步幅[mSeekBarIncrement]递增，使得最终保存的值的增幅为[mSeekBarIncrement]的倍数，如果增幅已经是[mSeekBarIncrement]
     * 的倍数，则直接返回，按照原流程保存，否则手动修改为[mSeekBarIncrement]的倍数后，手动保存并更新[mSeekBar]的进度
     * @return 是否可以按照原流程保存新值
     */
    fun setNewValueByStep(newValue: Int): Boolean {
        return if (newValue == max || newValue == min) {
            true
        } else {
            val increment = seekBarIncrement
            val remainder = (newValue - min) % increment
            if (remainder == 0) {
                true
            } else {
                value = newValue - remainder + increment
                Log.d(TAG, "setNewValueByStep() called with: newValue = [ $newValue ] , final value = $value , incre = $increment , remainder = $remainder")
                false
            }
        }
    }
}
