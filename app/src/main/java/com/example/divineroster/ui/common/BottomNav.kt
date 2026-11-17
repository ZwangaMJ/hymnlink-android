package com.example.divineroster.ui.common

import android.app.Activity
import android.content.Intent
import com.example.divineroster.R
import com.example.divineroster.ui.dashboard.DashboardActivity
import com.example.divineroster.ui.music.MusicActivity
import com.example.divineroster.ui.tools.ToolsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Dashboard / Music / Tools tabs. The Dashboard always sits at the bottom of the back stack and at
 * most one other tab is open above it, so Back from Music or Tools returns to the Dashboard.
 */
object BottomNav {

    fun setup(activity: Activity, nav: BottomNavigationView, current: Int) {
        nav.menu.findItem(current).isChecked = true
        nav.setOnItemSelectedListener { item ->
            val target = item.itemId
            when {
                target == current -> Unit
                target == R.id.nav_dashboard -> activity.finish()
                else -> {
                    val destination = if (target == R.id.nav_music) MusicActivity::class.java else ToolsActivity::class.java
                    activity.startActivity(Intent(activity, destination))
                    if (current != R.id.nav_dashboard) activity.finish()
                }
            }
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
            true
        }
    }

    /** Re-highlight the tab without triggering navigation (used when returning to a screen). */
    fun markSelected(nav: BottomNavigationView, current: Int) {
        nav.menu.findItem(current).isChecked = true
    }
}
