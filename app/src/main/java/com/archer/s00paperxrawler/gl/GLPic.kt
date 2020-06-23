package com.archer.s00paperxrawler.gl

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.archer.s00paperxrawler.MyApp
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.roundToInt

private const val TAG = "GLPic"

/**
 * Created by Chen Xin on 2018/10/22.
 */
class GLPic : Shape() {
    private val posBuffer by lazy {
        Log.d(TAG, "posBuffer lazy allocated")
        ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .also { setPosBuffer(it) }
    }

    /**图片展示区域的x轴偏移量*/
    private var xOffset = 0F

    /**纹理应展示的图片宽度，该宽度基于纹理坐标系计算*/
    private var textureWidth = 1f

    /**纹理应展示的图片高度，该宽度基于纹理坐标系计算*/
    private var textureHeight = 1f

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

    /**Texture handle数组*/
    private val textures = IntArray(1)

    private val matrixLocation = glGetUniformLocation(programHandle, glsl_uMatrix)
    private val virtualPosLocation = glGetAttribLocation(programHandle, glsl_aPos)
    private val texturePosLocation = glGetAttribLocation(programHandle, glsl_aTexturePos)
    private val samplerTextureLocation = glGetUniformLocation(programHandle, glsl_uSamplerTexture)

    /**
     * [bmpRatio]和[viewRatio]的变化会导致[textureWidth]、[textureOffsetRange]、[posBuffer]都产生变化，
     * 在此统一调整
     */
    private fun adjustPosBuffer() {
        calTextureWidth()
        setPosBuffer()
    }

    /**
     * bitmap width/height abbreviated to bw/bh = [bmpRatio]
     *
     * view width/height abbreviated to vw/vh = [viewRatio]
     *
     * treat texture's max x/y coordinate as width/height abbreviated to tw/th
     *
     *  有两种情况：
     * * 1.[bmpRatio] < [viewRatio]，如果展示全高则宽不够填满view，此时转换为展示全宽（即tw = 1），以x为系数截取部分高,
     * 为了保持图片不变形，需要保证[bmpRatio]，所以：
     * bw/(x*bh) = [viewRatio] -> x = [bmpRatio]/[viewRatio]
     *
     * * 2.[bmpRatio] >= [viewRatio], 展示图片全高（即th = 1）后图片宽依然有富余，以x为系数截取部分宽，为了保持图片不变形，
     * 为了保持图片不变形，需要保证[bmpRatio]，所以：
     * x*bw/bh = [viewRatio] -> x = [viewRatio]/[bmpRatio]
     *
     * texture x/y coordinate 取值范围均为[0，1]，跟实际比例无关，所以上方的x系数即为tw/th的值
     *
     */
    private fun calTextureWidth() {
        if (bmpRatio < viewRatio) {
            textureWidth = 1F
            textureHeight = bmpRatio / viewRatio
            textureOffsetRange = 0F
        } else {
            textureWidth = viewRatio / bmpRatio
            textureHeight = 1F
            textureOffsetRange = 1 - textureWidth
        }
    }

    private val posArray = floatArrayOf(
            //x,y,s,t
            -1f, 1f, xOffset, 0f,
            -1f, -1f, xOffset, textureHeight,
            1f, 1f, textureWidth + xOffset, 0f,
            1f, -1f, textureWidth + xOffset, textureHeight
    )

    /**获取图片绘制坐标缓冲*/
    private fun getPosArray(): FloatArray {
        posArray[2] = xOffset
        posArray[6] = xOffset
        posArray[7] = textureHeight
        posArray[10] = textureWidth + xOffset
        posArray[14] = textureWidth + xOffset
        posArray[15] = textureHeight
        Log.i(TAG, "posArray[14] = ${posArray[14]}, textureWidth = $textureWidth, xOffset = $xOffset")
        return posArray
    }

    fun setXOffset(xOffset: Float, xOffsetStep: Float) {
        val additionalScreenNum = (1 / xOffsetStep).roundToInt()//额外的屏幕数量
        val additionalTextureWidth = additionalScreenNum * textureWidth
        val textureOffsetRange: Float
        textureOffsetRange = if ((additionalTextureWidth + textureWidth) < 1) {//所有屏幕数量也不足以展示完整图片，则只展示可展示部分
            additionalTextureWidth
        } else {
            this.textureOffsetRange
        }
        this.xOffset = textureOffsetRange * xOffset
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
        glUniformMatrix4fv(matrixLocation, 1, false, uMVPMatrix, 0)
        //vertices坐标赋值
        posBuffer.position(0)
        glVertexAttribPointer(virtualPosLocation, 2, GL_FLOAT, false, 16, posBuffer)
        glEnableVertexAttribArray(virtualPosLocation)
        //textures坐标赋值
        posBuffer.position(2)
        glVertexAttribPointer(texturePosLocation, 2, GL_FLOAT, false, 16, posBuffer)
        glEnableVertexAttribArray(texturePosLocation)
        //texture unit赋值
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textures[0])
        glUniform1i(samplerTextureLocation, 0)
    }

    override fun doRealDraw(uMVPMatrix: FloatArray) {
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, indices)
    }

    /**清理Texture*/
    private fun destroyTexture() {
        if (textures[0] != 0) {
            glDeleteTextures(1, textures, 0)
            textures[0] = 0
        }
    }

    fun loadBitmap(path: String) {
        val bitmap = decodeBitmap(path)
        if (bitmap == null) {
            textures[0] = 0
            return
        }
        destroyTexture()
        glGenTextures(1, textures, 0)
        glBindTexture(GL_TEXTURE_2D, textures[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
        Log.d(TAG, "loadBitmap() called with: path = [ $path ] , w = ${bitmap.width} , h =${bitmap.height}")
        bmpRatio = bitmap.width / bitmap.height.toFloat()
        bitmap.recycle()
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun decodeBitmap(path: String): Bitmap? =
            if (path.startsWith("content://")) {//local file uri string
                val contentResolver = MyApp.AppCtx.contentResolver
                val uri = Uri.parse(path)
                val parcelFileDescriptor: ParcelFileDescriptor =
                        contentResolver.openFileDescriptor(uri, "r")!!
                val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
                val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image
            } else
                BitmapFactory.decodeFile(path)


    override fun onDestroy() {
        super.onDestroy()
        destroyTexture()
    }
}