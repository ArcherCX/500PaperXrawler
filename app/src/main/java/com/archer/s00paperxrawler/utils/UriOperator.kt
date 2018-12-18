package com.archer.s00paperxrawler.utils

import java.net.URLEncoder

/**
 * Created by Chen Xin on 2018/8/10.
 * 组合目标地址Uri操作类
 */

/**获取图片源Uri*/
fun getLoadUri(): String {
    val pref = prefs()
    return "${pref.baseUri}/${pref.feature}/${pref.categories.apply {
        replace(",", "-")
        replace(" ", "+")
    }}"
}

/**
 * 获取500px API URL
 * @param page 分页获取的页码，每页50张，从1开始
 */
fun getLegacyApiUri(page: Int): String {
    val prefs = prefs()
    val ret = if (page < 1) 1 else page
    return "${prefs.baseApiUri}rpp=50" +
            "&feature=${prefs.feature}" +
            "&image_size[]=2048" +
            "&formats=jpeg" +
            "&only=${prefs.categories.run { return@run URLEncoder.encode(this, "utf-8") }}" +
            "&page=$ret" +
            "&sort=&include_states=true&include_licensing=true&exclude=&personalized_categories="
}