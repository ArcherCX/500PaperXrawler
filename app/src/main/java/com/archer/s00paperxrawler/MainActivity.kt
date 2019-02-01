package com.archer.s00paperxrawler

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.archer.s00paperxrawler.presenter.SettingsPresenter
import com.archer.s00paperxrawler.view.SettingsFragment

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = SettingsFragment()
        SettingsPresenter(view)
        supportFragmentManager.beginTransaction().replace(R.id.container, view).commit()
    }

}
