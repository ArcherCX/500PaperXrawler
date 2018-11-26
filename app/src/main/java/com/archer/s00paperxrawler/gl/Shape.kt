package com.archer.s00paperxrawler.gl

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLU
import android.util.Log

private const val TAG = "Shape"
const val glsl_aPos = "aPos"
const val glsl_aTexturePos = "aTexturePos"
const val glsl_vTexturePos = "vTexturePos"
const val glsl_uMatrix = "uMatrix"
const val glsl_aColor = "aColor"
const val glsl_vColor = "vColor"
const val glsl_uColor = "uColor"
const val glsl_uSamplerTexture = "uSamplerTexture"

fun getGLError(msg: String = "0") {
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) Log.e(TAG, "getGLError() called : [msg : $msg], error code = $error , ${GLU.gluErrorString(error)}")
}
/**
 * Created by Chen Xin on 2018/10/22.
 */
abstract class Shape {
    private val vtxHandle: Int
    private val fragmentHandle: Int
    protected val programHandle: Int
    /**可展示区域的宽高比，以便于设置变换后的虚拟坐标空间的坐标*/
    open var viewRatio = 1F

    init {
        vtxHandle = loadShader(GL_VERTEX_SHADER, this.getVtxShaderSource())
        fragmentHandle = loadShader(GL_FRAGMENT_SHADER, this.getFragmentShaderSource())
        programHandle = if (vtxHandle == 0 || fragmentHandle == 0) 0
        else glCreateProgram().also {
            glAttachShader(it, vtxHandle)
            glAttachShader(it, fragmentHandle)
            glLinkProgram(it)
            //link完成即可detach 和 delete shader
            glDetachShader(it, vtxHandle)
            glDeleteShader(vtxHandle)
            glDetachShader(it, fragmentHandle)
            glDeleteShader(fragmentHandle)
            val linkRet = IntArray(1)
            glGetProgramiv(it, GL_LINK_STATUS, linkRet, 0)
            val ret = linkRet[0] == GL_TRUE
            Log.i(TAG, "init() called program link ret: $ret")
            if (!ret) {
                glDeleteProgram(it)
            }
        }
    }

    private fun loadShader(type: Int, shaderSource: String): Int {
        val shader = glCreateShader(type)
        if (shader == 0) getGLError("Create Shader failed with type : $type")
        glShaderSource(shader, shaderSource)
        glCompileShader(shader)
        val compileRet = IntArray(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, compileRet, 0)
        if (compileRet[0] != GL_TRUE) {
            getShaderInfoLog(shader)
            glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun getShaderInfoLog(shader: Int) {
        val len = IntArray(1)
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, len, 0)
        if (len[0] > 0) Log.e(TAG, "glGetShaderInfoLog: ${glGetShaderInfoLog(shader)}")
        else getGLError("There is no shader Info log")
    }

    /**生成Vertex Shader代码*/
    abstract fun getVtxShaderSource(): String

    /**生成Fragment Shader代码*/
    abstract fun getFragmentShaderSource(): String

    /**绑定Shader内各变量值*/
    abstract fun bindData(uMVPMatrix: FloatArray)

    /**执行OpenGL的绘制操作，考虑到绘制方式各异，自行调用对应的绘制方法：[glDrawElements] or [glDrawArrays]*/
    abstract fun doRealDraw(uMVPMatrix: FloatArray)

    fun draw(uMVPMatrix: FloatArray) {
        if (programHandle == 0) return
        glUseProgram(programHandle)
        bindData(uMVPMatrix)
        doRealDraw(uMVPMatrix)
    }

    open fun onDestroy() {
        glDeleteProgram(programHandle)
    }
}