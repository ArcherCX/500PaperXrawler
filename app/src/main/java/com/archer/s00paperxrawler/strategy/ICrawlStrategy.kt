package com.archer.s00paperxrawler.strategy

import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.functions.Function
import java.io.File

/**选取有效策略*/
fun getCrawlStrategy(): ICrawlStrategy = StrategyFor500()

/**
 * Created by Chen Xin on 2018/8/21.
 * 网页图片爬取策略
 */
interface ICrawlStrategy {

    /**需要在解析HTML内容前执行的JS脚本*/
    fun evaluateJSBeforeParse(): Observable<String>

    /**
     * 解析HTML内容获取目标内容
     * @return 返回可用于RxJava流程的[Function],该[Function]解析网页并将解析结果emmit至下一个流程
     */
    fun parseHTML(): Function<File, Observable<String>>

    /**
     *处理由[parseHTML] emmit的解析结果
     */
    fun handleResult(): Function<String, Any>

    /**
     * Destroy之前需要执行的JS脚本，如清理JS变量，执行JS清理函数等
     */
    fun evaluateJSBeforeDestroy(webView: WebView)
}