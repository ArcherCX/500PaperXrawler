package com.archer.s00paperxrawler

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.archer.s00paperxrawler.view.HistoryBrowserFragment
import com.archer.s00paperxrawler.view.HistoryDetailFragment
import com.archer.s00paperxrawler.view.PhotoDetailFragment
import com.archer.s00paperxrawler.view.SettingsFragment

private const val TAG = "MainActivity"
const val ACTIVITY_ACTION_PHOTO_DETAIL = "action_double_tap_photo_detail"
const val ACTIVITY_ACTION_HISTORY = "action_history"
const val ACTIVITY_ACTION_HISTORY_DETAIL = "action_history_detail"

class MainActivity : AppCompatActivity() {
    private var menuRes = -1
    private var menuActions: ((Int) -> Boolean)? = null
    private var currentView: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        setContentFragment(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setContentFragment(intent)
    }

    private fun setContentFragment(intent: Intent?) {
        if (intent == null) return
        var backstack = false
        var name = ""
        when (intent.action) {
            ACTIVITY_ACTION_PHOTO_DETAIL -> {
                currentView = PhotoDetailFragment()
                val extras = intent.extras
                if (extras != null) {
                    backstack = true//extras is not null means this action not from double-tap-wallpaper, need navigation.
                    name = ACTIVITY_ACTION_PHOTO_DETAIL
                }
                currentView!!.arguments = extras
            }
            ACTIVITY_ACTION_HISTORY -> {
                currentView = HistoryBrowserFragment()
                backstack = true
                name = ACTIVITY_ACTION_HISTORY
            }
            ACTIVITY_ACTION_HISTORY_DETAIL -> {
                currentView = HistoryDetailFragment()
                currentView!!.arguments = intent.extras
                backstack = true
                name = ACTIVITY_ACTION_HISTORY_DETAIL
            }
            else -> {
                currentView = SettingsFragment()
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, currentView!!).apply {
                if (backstack) addToBackStack(name)
            }.commit()

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
