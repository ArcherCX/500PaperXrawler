package com.archer.s00paperxrawler.service

import android.opengl.GLES20
import android.opengl.Matrix
import android.text.TextUtils
import android.util.Log
import com.archer.s00paperxrawler.gl.GLPic
import com.archer.s00paperxrawler.gl.GLRenderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "MyRender"

/**
 * Created by Chen Xin on 2018/10/30.
 */
class MyRenderer : GLRenderer {
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)
    /**最近的屏幕偏移量*/
    private var xCurrentOffset: Float = 0F
    /**最近的屏幕偏移步幅*/
    private var xCurrentOffsetStep: Float = 0F
    var picPath = ""
        set(v) {
            field = v
            if (::pic.isInitialized) {
                pic.loadBitmap(v)
                if (xCurrentOffsetStep > 0F) pic.setXOffset(xCurrentOffset, xCurrentOffsetStep)
            }
        }
    private lateinit var pic: GLPic

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (!::pic.isInitialized) return
        pic.draw(mMVPMatrix)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged() called with: gl = [ $gl ], width = [ $width ], height = [ $height ]")
        GLES20.glViewport(0, 0, width, height)
        pic.viewRatio = width / height.toFloat()
        Matrix.orthoM(mProjectionMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        /*eye坐标是视点坐标，但是使用正交投影就无所谓eye.z的值，投射后大小不变，如果使用锥视投影frustum，配合lookAt的near，
        far可以产生不同大小的投影而不需要修改图片大小，center坐标是焦点中心，up坐标控制投影图的方向，可以产生图片的旋转效果*/
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated() called with: version = ${GLES20.glGetString(GLES20.GL_VERSION)}, gl = [ $gl ], config = [ $config ]")
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        if (::pic.isInitialized) pic.onDestroy()
        pic = GLPic().apply { if (!TextUtils.isEmpty(picPath)) loadBitmap(picPath) }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
        Log.d(TAG, "onOffsetsChanged() called with: xOffset = [ $xOffset ], yOffset = [ $yOffset ], xOffsetStep = [ $xOffsetStep ], yOffsetStep = [ $yOffsetStep ], xPixelOffset = [ $xPixelOffset ], yPixelOffset = [ $yPixelOffset ]")
        xCurrentOffset = xOffset
        xCurrentOffsetStep = xOffsetStep
        pic.setXOffset(xOffset, xOffsetStep)
    }

    override fun onDestroy() {
        pic.onDestroy()
    }
}