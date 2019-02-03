package com.archer.s00paperxrawler.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

private const val THREE_TOUCH_TIMEOUT = 50L
/**三指触摸事件Message What*/
private const val MSG_FIRST_TOUCH_DOWN = 0
private const val MSG_FIRST_TOUCH_UP = 1
private const val MSG_SECOND_TOUCH_DOWN = 2
private const val MSG_SECOND_TOUCH_UP = 3
private const val MSG_THIRD_TOUCH_DOWN = 4
private const val MSG_THIRD_TOUCH_UP = 5
/**第一根手指按下*/
private const val MASK_FIRST_FINGER: Byte = 1
/**第二根手指按下*/
private const val MASK_SECOND_FINGER: Byte = 2
/**第三根手指按下*/
private const val MASK_THIRD_FINGER: Byte = 4
/**三指触摸处于按下的过程中，0为放开的过程*/
private const val MASK_THREE_TOUCH_DOWN: Byte = 8
/**处于有效的三指触摸事件内*/
private const val MASK_IN_THREE_TOUCH: Byte = 16

/**
 * Created by Chen Xin on 2019/2/2.
 */
class GestureDetector {
    constructor() {
        handler = GestureHandler()
    }

    constructor(looper: Looper) {
        handler = GestureHandler(looper)
    }

    lateinit var onThreeTouchListener: OnThreeTouchListener

    private val handler: GestureHandler

    /**
     * 三指触摸事件Flag，5个bit，从左到右分别为：
     * 1、是否处于有效的三指触摸事件区域内
     * 2、三指触摸事件是否处于按下即Down的流程
     * 3、第三、二、一个手指是否按下
     */
    private var flagThreeTouch: Byte = 0

    private inner class GestureHandler : Handler {
        constructor() : super()
        constructor(looper: Looper) : super(looper)

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_THIRD_TOUCH_DOWN -> {
                    val ev = msg.obj as MotionEvent
                    onThreeTouchListener.onThreeTouchDown(ev)
                    onThreeTouchListener.onThreeTouchEvent(ev)
                }
            }
        }
    }

    private fun checkBit(mask: Byte, origin: Byte = flagThreeTouch): Boolean = origin and mask == mask

    private fun hasThreeTouchListener(): Boolean = ::onThreeTouchListener.isInitialized

    private fun clearFlagThreeTouch() {
        flagThreeTouch = 0
        handler.removeMessages(MSG_THIRD_TOUCH_DOWN)
        handler.removeMessages(MSG_THIRD_TOUCH_UP)
    }

    fun onTouchEvent(event: MotionEvent) {
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (hasThreeTouchListener()) {
                    flagThreeTouch = flagThreeTouch or MASK_THREE_TOUCH_DOWN or MASK_FIRST_FINGER
                    handler.sendEmptyMessageDelayed(MSG_FIRST_TOUCH_DOWN, THREE_TOUCH_TIMEOUT)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (hasThreeTouchListener()) {
                    val actionIndex = event.actionIndex
                    if (handler.hasMessages(MSG_FIRST_TOUCH_DOWN)) {
                        if (actionIndex == 1 && !checkBit(MASK_SECOND_FINGER)) handler.sendEmptyMessageDelayed(MSG_SECOND_TOUCH_DOWN, THREE_TOUCH_TIMEOUT)
                        else if (actionIndex == 2 && handler.hasMessages(MSG_SECOND_TOUCH_DOWN) && !checkBit(MASK_THIRD_FINGER)) {
                            flagThreeTouch = flagThreeTouch or MASK_THIRD_FINGER or MASK_IN_THREE_TOUCH
                            handler.sendMessageDelayed(Message.obtain(handler, MSG_THIRD_TOUCH_DOWN, event), THREE_TOUCH_TIMEOUT)
                        } else clearFlagThreeTouch()
                    } else clearFlagThreeTouch()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (checkBit(MASK_IN_THREE_TOUCH)) {
                    onThreeTouchListener.onThreeTouchEvent(event)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (checkBit(MASK_IN_THREE_TOUCH)) {
                    if (event.pointerCount == 3) {
                        flagThreeTouch = flagThreeTouch and MASK_THREE_TOUCH_DOWN.inv() and MASK_THIRD_FINGER.inv()
                        handler.sendEmptyMessageDelayed(MSG_FIRST_TOUCH_UP, THREE_TOUCH_TIMEOUT)
                    } else if (event.pointerCount == 2 && handler.hasMessages(MSG_FIRST_TOUCH_UP)) {
                        flagThreeTouch = flagThreeTouch and MASK_SECOND_FINGER.inv()
                        handler.sendEmptyMessageDelayed(MSG_SECOND_TOUCH_UP, THREE_TOUCH_TIMEOUT)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (checkBit(MASK_IN_THREE_TOUCH) && handler.hasMessages(MSG_FIRST_TOUCH_UP) && handler.hasMessages(MSG_SECOND_TOUCH_UP)) {
                    handler.sendMessageDelayed(Message.obtain(handler, MSG_THIRD_TOUCH_UP, event), THREE_TOUCH_TIMEOUT)
                }
                clearFlagThreeTouch()
            }
        }
    }

    /**三点触摸事件监听*/
    interface OnThreeTouchListener {
        /**三点触摸按下时回调*/
        fun onThreeTouchDown(ev: MotionEvent): Boolean = false

        /**三点触摸放开时回调*/
        fun onThreeTouchUp(ev: MotionEvent): Boolean = false

        /**三点触摸发生内的所有事件，Down、Move、Up*/
        fun onThreeTouchEvent(ev: MotionEvent): Boolean = false
    }
}