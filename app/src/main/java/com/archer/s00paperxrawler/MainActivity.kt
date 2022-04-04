package com.archer.s00paperxrawler

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.archer.s00paperxrawler.view.DoubleTapPhotoDetailFragment
import com.archer.s00paperxrawler.view.SettingsFragment

private const val TAG = "MainActivity"
const val ACTIVITY_ACTION_DOUBLE_TAP_PHOTO_DETAIL = "double_tap_photo_detail"

class MainActivity : AppCompatActivity() {
    private var menuRes = -1
    private var menuActions: ((Int) -> Boolean)? = null
    private var currentView: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        setContentFragment(intent?.action)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setContentFragment(intent?.action)
    }

    private fun setContentFragment(action: String?) {
        val replace: Boolean
        when (action) {
            ACTIVITY_ACTION_DOUBLE_TAP_PHOTO_DETAIL -> {
                replace = !DoubleTapPhotoDetailFragment::class.isInstance(currentView)
                if (replace) currentView = DoubleTapPhotoDetailFragment()
            }
            else -> {
                replace = !SettingsFragment::class.isInstance(currentView)
                if (replace) currentView = SettingsFragment()
            }
        }
        if (replace) supportFragmentManager.beginTransaction().replace(R.id.container, currentView!!).commit()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        item.itemId.let {
            return menuActions?.invoke(it) ?: false
        }
    }

}
