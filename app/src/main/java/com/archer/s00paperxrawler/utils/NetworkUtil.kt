package com.archer.s00paperxrawler.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log

private const val TAG = "NetworkUtil"
/**
 * Created by Chen Xin on 2019/1/25.
 */
private var networkCallback: Any? = null

/**注册Wifi状态变化回调*/
fun registerWifiCallback(ctx: Context) {
    if (networkCallback != null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "onAvailable() called with: network = [ $network ]")
                onWifiConnected()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "onLost() called with: network = [ $network ]")
                onWifiLost()
            }
        }
        connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                networkCallback as ConnectivityManager.NetworkCallback)
    } else {
        networkCallback = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "onReceive() called with: context = [ $context ], intent = [ $intent ]")
                val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if ((activeNetworkInfo?.type ?: -1 == ConnectivityManager.TYPE_WIFI) && activeNetworkInfo?.isConnected == true) {
                    onWifiConnected()
                } else {
                    onWifiLost()
                }
            }
        }
        ctx.registerReceiver(networkCallback as BroadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }
}

/**注销Wifi状态监听回调*/
fun unregisterWifiAction(ctx: Context) {
    if (networkCallback == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        (ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(networkCallback as ConnectivityManager.NetworkCallback)
    } else {
        ctx.unregisterReceiver(networkCallback as BroadcastReceiver)
    }
    networkCallback = null
}

private fun onWifiConnected() {
    prefs().wifiAvailable = true
}

private fun onWifiLost() {
    prefs().wifiAvailable = false
}