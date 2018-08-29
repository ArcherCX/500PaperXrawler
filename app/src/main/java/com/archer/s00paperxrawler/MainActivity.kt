package com.archer.s00paperxrawler

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.archer.s00paperxrawler.utils.prefs
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dir = File(prefs().photosCachePath)
        val files = dir.listFiles()
        var idx = 0
        btn.setOnClickListener {
            if (idx >= files.size) idx = 0
            if (preview.drawable is BitmapDrawable) {
                val bitmap = (preview.drawable as BitmapDrawable).bitmap
                preview.setImageBitmap(BitmapFactory.decodeFile(files[idx++].absolutePath, null))
                bitmap.recycle()
            }
        }
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (files.isNotEmpty()) {
                    if (idx >= files.size) idx = 0
                    val bitmap = BitmapFactory.decodeFile(files[idx++].absolutePath, null)
                    preview.setImageBitmap(bitmap)
                }
            } else {
                if (preview.drawable != null && preview.drawable is BitmapDrawable) {
                    Log.d(TAG, "toggle() called isBitmapDrawable")
                    val bitmap = (preview.drawable as BitmapDrawable).bitmap
                    preview.setImageDrawable(null)
                    bitmap.recycle()
                }
            }
        }
    }

}
