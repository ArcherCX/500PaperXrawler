package com.archer.s00paperxrawler.gl

import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.roundToInt

private const val TAG = "GLPic"

/**
 * Created by Chen Xin on 2018/10/22.
 */
class GLPic : Shape() {
    private var textureName = 0
    private val posBuffer by lazy {
        Log.d(TAG, "null() called")
        ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .also { setPosBuffer(it) }
    }
    /**图片展示区域的x轴偏移量*/
    private var xOffset = 0F
    /**纹理应展示的图片宽度，该宽度基于纹理坐标系计算*/
    private var textureWidth = 1f
    /**纹理滑动偏移量范围*/
    private var textureOffsetRange = 0f
    override var viewRatio: Float = 1f
        set(value) {
            field = value
            adjustPosBuffer()
        }

    /**要加载的图片宽高比*/
    private var bmpRatio = 1f
        set(value) {
            field = value
            adjustPosBuffer()
        }

    /**
     * [bmpRatio]和[viewRatio]的变化会导致[textureWidth]、[textureOffsetRange]、[posBuffer]都产生变化，
     * 在此统一调整
     */
    private fun adjustPosBuffer() {
        calTextureWidth()
        setPosBuffer()
    }

    /**
     * 设图片宽高为bw、bh，可展示区域宽高为sw、sh，纹理宽高为tw、th，则[bmpRatio]=bw/bh，[viewRatio]=sw/sh，
     * 由于固定展示全高，则纹理高th=1，且要保证纹理不变形，所以需要确定影响纹理宽的系数x，使得bw * x/bh=[viewRatio]，
     * 也就是x * [bmpRatio]=[viewRatio]，最终得到x=[viewRatio]/[bmpRatio]，因为纹理坐标系取值范围为[0,1]，所以
     * 纹理宽tw=1*x=x
     */
    private fun calTextureWidth() {
        textureWidth = viewRatio / bmpRatio
        textureOffsetRange = 1 - textureWidth
    }

    /**获取图片绘制坐标缓冲*/
    private fun getPosArray(): FloatArray {
        return floatArrayOf(
                //x,y,s,t
                -viewRatio, 1f, xOffset, 0f,
                -viewRatio, -1f, xOffset, 1f,
                viewRatio, 1f, textureWidth + xOffset, 0f,
                viewRatio, -1f, textureWidth + xOffset, 1f
        )
    }

    fun setXOffset(xOffset: Float, xOffsetStep: Float) {
        if (xOffsetStep == 0f) return//初始化WallpaperService时，Engine的onOffsetsChanged回调存在所有参数为0的情况，直接略过
        val additionalScreenNum = (1 / xOffsetStep).roundToInt()//额外的屏幕数量
        val additionalTextureWidth = additionalScreenNum * textureWidth
        val textureOffsetRange: Float
        if ((additionalTextureWidth + textureWidth) < 1) {//所有屏幕数量也不足以展示完整图片，则只展示可展示部分
            textureOffsetRange = additionalTextureWidth
            this.xOffset = textureOffsetRange * xOffset
        } else {
            textureOffsetRange = this.textureOffsetRange
            this.xOffset = textureOffsetRange * xOffset
        }
        setPosBuffer()
    }

    /**设置图片绘制坐标缓冲*/
    private fun setPosBuffer(buffer: FloatBuffer = posBuffer) {
        buffer.clear()
        buffer.put(getPosArray())
        buffer.position(0)
    }

    private val indices = ByteBuffer.allocateDirect(6)
            .order(ByteOrder.nativeOrder())
            .put(byteArrayOf(0, 1, 2, 2, 1, 3))
            .position(0)

    override fun getVtxShaderSource() = "" +
            "uniform mat4 $glsl_uMatrix;" +
            "attribute vec4 $glsl_aPos;" +
            "attribute vec2 $glsl_aTexturePos;" +
            "varying vec2 $glsl_vTexturePos;" +
            "void main(){" +
            "   $glsl_vTexturePos = $glsl_aTexturePos;" +
            "   gl_Position = $glsl_uMatrix * $glsl_aPos;" +
            "}"

    override fun getFragmentShaderSource() = "" +
            "precision mediump float;" +
            "varying vec2 $glsl_vTexturePos;" +
            "uniform sampler2D $glsl_uSamplerTexture;" +
            "void main(){" +
            "   gl_FragColor = texture2D($glsl_uSamplerTexture, $glsl_vTexturePos);" +
            "}"

    override fun bindData(uMVPMatrix: FloatArray) {
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureName)
        glGetUniformLocation(programHandle, glsl_uMatrix).also {
            glUniformMatrix4fv(it, 1, false, uMVPMatrix, 0)
        }
        /*当GLThread.guardRun()执行完后，进入新一轮的循环时，如果没有参数变化，则posBuffer的position依然会停留在
        * 上一轮绘制结束时的位置[2]上，导致此轮绘制的图片不在正常位置上，所以每次进入重新置位*/
        posBuffer.position(0)
        glGetAttribLocation(programHandle, glsl_aPos).also {
            glVertexAttribPointer(it, 2, GL_FLOAT, false, 16, posBuffer)
            glEnableVertexAttribArray(it)
        }
        glGetAttribLocation(programHandle, glsl_aTexturePos).also {
            posBuffer.position(2)
            glVertexAttribPointer(it, 2, GL_FLOAT, false, 16, posBuffer)
            glEnableVertexAttribArray(it)
        }
        glGetUniformLocation(programHandle, glsl_uSamplerTexture).also {
            glUniform1i(it, 0)
        }
    }

    override fun doRealDraw(uMVPMatrix: FloatArray) {
        Log.d(TAG, "doRealDraw() called with: uMVPMatrix = [ ${uMVPMatrix.joinToString()} ]")
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, indices)
    }

    fun loadBitmap(path: String) {
        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap == null) {
            textureName = 0
            return
        }
        val textures = IntArray(1)
        glGenTextures(1, textures, 0)
        glBindTexture(GL_TEXTURE_2D, textures[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
        Log.d(TAG, "loadBitmap() called with: path = [ $path ] , w = ${bitmap.width} , h =${bitmap.height}")
        bmpRatio = bitmap.width / bitmap.height.toFloat()
        bitmap.recycle()
        glBindTexture(GL_TEXTURE_2D, 0)
        textureName = textures[0]
    }
}