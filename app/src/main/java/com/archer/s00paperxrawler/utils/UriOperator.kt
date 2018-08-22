package com.archer.s00paperxrawler.utils

/**
 * Created by Chen Xin on 2018/8/10.
 * 组合目标地址Uri操作类
 */

/**获取图片源Uri*/
fun getLoadUri(): String {
    val pref = prefs()
    return "${pref.baseUri}/${pref.feature}/${pref.categories}"
}