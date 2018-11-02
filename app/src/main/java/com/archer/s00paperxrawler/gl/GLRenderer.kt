package com.archer.s00paperxrawler.gl

import android.opengl.GLSurfaceView

/**
 * Created by Chen Xin on 2018/11/2.
 */
interface GLRenderer : GLSurfaceView.Renderer {
    fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int)
}