package com.archer.s00paperxrawler.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

private const val TAG = "GestureDetector"

private const val INVALID_FLAG: Byte = 0

//----------------- ⬇ Three Touch Event Constant ⬇ -------------------
private const val THREE_TOUCH_TIMEOUT = 100L

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
//----------------- ⬆ Three Touch Event Constant ⬆ -------------------

//----------------- ⬇ Double Tap Event Constant ⬇ -------------------
/**The valid interval between the first tap down and second tap up*/
private const val DOUBLE_TAP_TIMEOUT = 300L
private const val MSG_FIRST_TAP_DOWN = 6
private const val MSG_FIRST_TAP_UP = 7

//private const val MSG_SECOND_TAP_DOWN = 8
private const val MSG_SECOND_TAP_UP = 9
private const val MASK_FIRST_TAP_DOWN: Byte = 1
private const val MASK_FIRST_TAP_UP: Byte = 2
private const val MASK_SECOND_TAP_DOWN: Byte = 4
//----------------- ⬆ Three Tap Event Constant ⬆ -------------------


/**
 * Created by Chen Xin on 2019/2/2.
 */
class GestureDetector {
    private val onTouchEventListener: OnTouchEventListener

    constructor(onTouchEventListener: OnTouchEventListener) {
        this.onTouchEventListener = onTouchEventListener
        handler = GestureHandler()
    }

    constructor(onTouchEventListener: OnTouchEventListener, looper: Looper) {
        this.onTouchEventListener = onTouchEventListener
        handler = GestureHandler(looper)
    }

    private val handler: GestureHandler

    /**
     * 三指触摸事件Flag，5个bit，从左到右分别为：
     * 1、是否处于有效的三指触摸事件区域内
     * 2、三指触摸事件是否处于按下即Down的流程
     * 3、第三、二、一个手指是否按下
     */
    private var flagThreeTouch: Byte = INVALID_FLAG

    /**
     * Double tap flag，
     */
    private var flagDoubleTap: Byte = INVALID_FLAG

    private inner class GestureHandler : Handler {
        constructor() : super(Looper.myLooper()!!)
        constructor(looper: Looper) : super(looper)

        override fun handleMessage(msg: Message) {
            Log.w(TAG, "handleMessage: MSG what = ${msg.what}")
            when (msg.what) {
                MSG_THIRD_TOUCH_DOWN -> {
                    Log.w(TAG, "handleMessage: MSG_THIRD_TOUCH_DOWN")
                    val ev = msg.obj as MotionEvent
                    onTouchEventListener.onThreeTouchDown(ev)
                    onTouchEventListener.onThreeTouchEvent(ev)
                }
                MSG_FIRST_TAP_UP -> clearFlagDoubleTap()//reset flag for next double tap when no second tap coming in time
                MSG_SECOND_TAP_UP -> onTouchEventListener.onDoubleTap(msg.obj as MotionEvent)
            }
        }
    }

    private fun checkBit(mask: Byte, origin: Byte = flagThreeTouch): Boolean = origin and mask == mask

    private fun clearFlagThreeTouch() {
        flagThreeTouch = INVALID_FLAG
    }

    private fun clearFlagDoubleTap() {
        flagDoubleTap = INVALID_FLAG
        handler.removeMessages(MSG_FIRST_TAP_DOWN)
    }

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //handle double tap
                if (flagDoubleTap == INVALID_FLAG) {//it's first tap
                    Log.i(TAG, "onTouchEvent: Action Down flag = ${flagDoubleTap.toString(2)}")
                    flagDoubleTap = flagDoubleTap or MASK_FIRST_TAP_DOWN
                    handler.sendEmptyMessageDelayed(MSG_FIRST_TAP_DOWN, DOUBLE_TAP_TIMEOUT)
                } else if (checkBit(MASK_FIRST_TAP_DOWN or MASK_FIRST_TAP_UP, flagDoubleTap) && handler.hasMessages(MSG_FIRST_TAP_DOWN)) {//it's second tap
                    Log.i(TAG, "onTouchEvent: Action Down 2 flag = ${flagDoubleTap.toString(2)}")
                    flagDoubleTap = flagDoubleTap or MASK_SECOND_TAP_DOWN
                } else {
                    Log.w(TAG, "onTouchEvent: Action Down neither flag = ${flagDoubleTap.toString(2)}")
                    clearFlagDoubleTap()
                }
                //handle three fingers tap
                flagThreeTouch = flagThreeTouch or MASK_THREE_TOUCH_DOWN or MASK_FIRST_FINGER
                handler.sendEmptyMessageDelayed(MSG_FIRST_TOUCH_DOWN, THREE_TOUCH_TIMEOUT)

            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                //double tap event cancel when second finger down
                clearFlagDoubleTap()
                //handle three fingers tap
                val actionIndex = event.actionIndex
                Log.w(TAG, "ACTION_POINTER_DOWN: actionIndex = $actionIndex")
                if (handler.hasMessages(MSG_FIRST_TOUCH_DOWN)) {
                    Log.d(TAG, "ACTION_POINTER_DOWN: 1")
                    if (actionIndex == 1 && !checkBit(MASK_SECOND_FINGER)) {
                        Log.d(TAG, "ACTION_POINTER_DOWN: 2")
                        handler.sendEmptyMessageDelayed(MSG_SECOND_TOUCH_DOWN, THREE_TOUCH_TIMEOUT)
                    } else if (actionIndex == 2 && handler.hasMessages(MSG_SECOND_TOUCH_DOWN) && !checkBit(MASK_THIRD_FINGER)) {
                        Log.d(TAG, "ACTION_POINTER_DOWN: 3")
                        flagThreeTouch = flagThreeTouch or MASK_THIRD_FINGER or MASK_IN_THREE_TOUCH
                        handler.sendMessage(Message.obtain(handler, MSG_THIRD_TOUCH_DOWN, event))
                    } else {
                        Log.d(TAG, "ACTION_POINTER_DOWN: 4")
                        clearFlagThreeTouch()
                    }
                } else {
                    Log.d(TAG, "ACTION_POINTER_DOWN: 5")
                    clearFlagThreeTouch()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                //handle three fingers tap
                if (checkBit(MASK_IN_THREE_TOUCH)) {
                    onTouchEventListener.onThreeTouchEvent(event)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                //handle three fingers tap
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
                //handle double tap
                if (flagDoubleTap != INVALID_FLAG && checkBit(MASK_FIRST_TAP_DOWN, flagDoubleTap) && handler.hasMessages(MSG_FIRST_TAP_DOWN)) {
                    Log.d(TAG, "onTouchEvent: Action Up 1 flag = ${flagDoubleTap.toString(2)}")
                    if (checkBit(MASK_FIRST_TAP_UP or MASK_SECOND_TAP_DOWN, flagDoubleTap)) {
                        Log.d(TAG, "onTouchEvent: Action Up 2 flag = ${flagDoubleTap.toString(2)}")
                        handler.sendMessage(Message.obtain(handler, MSG_SECOND_TAP_UP, event))
                        clearFlagDoubleTap()
                    } else {
                        Log.d(TAG, "onTouchEvent: Action Up 3 flag = ${flagDoubleTap.toString(2)}")
                        flagDoubleTap = flagDoubleTap or MASK_FIRST_TAP_UP
                        handler.sendEmptyMessageDelayed(MSG_FIRST_TAP_UP, DOUBLE_TAP_TIMEOUT)
                    }
                } else {
                    clearFlagDoubleTap()
                }
                //handle three fingers tap
                if (checkBit(MASK_IN_THREE_TOUCH) && handler.hasMessages(MSG_FIRST_TOUCH_UP) && handler.hasMessages(MSG_SECOND_TOUCH_UP)) {
                    handler.sendMessageDelayed(Message.obtain(handler, MSG_THIRD_TOUCH_UP, event), THREE_TOUCH_TIMEOUT)
                }
                clearFlagThreeTouch()
            }
        }
    }

    /**三点触摸事件监听*/
    interface OnTouchEventListener {
        /**三点触摸按下时回调*/
        fun onThreeTouchDown(ev: MotionEvent): Boolean = false

        /**三点触摸放开时回调*/
        fun onThreeTouchUp(ev: MotionEvent): Boolean = false

        /**三点触摸发生内的所有事件，Down、Move、Up*/
        fun onThreeTouchEvent(ev: MotionEvent): Boolean = false

        /**Double Tap Event Callback*/
        fun onDoubleTap(ev: MotionEvent): Boolean = false
    }
}