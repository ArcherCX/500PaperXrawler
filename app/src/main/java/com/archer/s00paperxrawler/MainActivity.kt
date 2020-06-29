package com.archer.s00paperxrawler

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.archer.s00paperxrawler.view.SettingsFragment

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private var menuRes = -1
    private var menuActions: ((Int) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        val view = SettingsFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, view).commit()
    }

    fun requestResetMenu(menuRes: Int, menuActions: ((Int) -> Boolean)) {
        this.menuRes = menuRes
        this.menuActions = menuActions
        invalidateOptionsMenu()
    }

    fun invalidateMenu() {
        menuRes = -1
        menuActions = null
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menuRes != -1) {
            menuInflater.inflate(menuRes, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.itemId!!.let {
            return menuActions?.invoke(it) ?: false
        }
    }

}
