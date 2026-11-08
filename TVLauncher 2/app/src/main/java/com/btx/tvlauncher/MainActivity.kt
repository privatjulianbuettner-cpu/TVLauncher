package com.btx.tvlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var repo: AppRepository
    private lateinit var root: View
    private lateinit var favLabel: TextView
    private lateinit var favList: RecyclerView
    private lateinit var appsList: RecyclerView

    private lateinit var favAdapter: AppAdapter
    private lateinit var appsAdapter: AppAdapter

    private val executor = Executors.newSingleThreadExecutor()
    private var allApps: List<AppEntry> = emptyList()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = AppRepository(this)

        root = findViewById(R.id.root)
        favLabel = findViewById(R.id.label_fav)
        favList = findViewById(R.id.list_fav)
        appsList = findViewById(R.id.list_apps)

        favAdapter = AppAdapter(170, ::launch, ::showMenu)
        appsAdapter = AppAdapter(null, ::launch, ::showMenu)

        favList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        favList.adapter = favAdapter
        appsList.adapter = appsAdapter

        findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        try {
            unregisterReceiver(packageReceiver)
        } catch (_: IllegalArgumentException) {
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        root.background = Backgrounds.drawable(repo.background)
        appsList.layoutManager = GridLayoutManager(this, repo.columns)
        refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // HOME-Taste auf dem Homescreen: nach oben springen
        appsList.scrollToPosition(0)
        favList.scrollToPosition(0)
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    /** Ein Launcher darf per Zurück-Taste nicht beendet werden. */
    @Deprecated("Launcher bleibt immer offen")
    override fun onBackPressed() {
        // absichtlich leer
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun refresh() {
        executor.execute {
            val apps = repo.loadApps()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                allApps = apps
                bind()
            }
        }
    }

    private fun bind() {
        val hidden = repo.hidden()
        val visible = allApps.filter { it.packageName !in hidden }
        val favorites = repo.favorites().mapNotNull { pkg -> visible.find { it.packageName == pkg } }

        favAdapter.submit(favorites)
        appsAdapter.submit(visible)

        val showFav = favorites.isNotEmpty()
        favLabel.visibility = if (showFav) View.VISIBLE else View.GONE
        favList.visibility = if (showFav) View.VISIBLE else View.GONE
    }

    private fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(entry.packageName, entry.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(R.string.error_launch)
        } catch (_: SecurityException) {
            toast(R.string.error_launch)
        }
    }

    private fun showMenu(entry: AppEntry) {
        val isFavorite = repo.favorites().contains(entry.packageName)
        val options = arrayOf(
            getString(R.string.menu_open),
            getString(if (isFavorite) R.string.menu_unfav else R.string.menu_fav),
            getString(R.string.menu_info),
            getString(R.string.menu_hide),
            getString(R.string.menu_uninstall)
        )
        AlertDialog.Builder(this)
            .setTitle(entry.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launch(entry)
                    1 -> {
                        repo.toggleFavorite(entry.packageName)
                        bind()
                    }
                    2 -> openAppInfo(entry.packageName)
                    3 -> {
                        repo.hide(entry.packageName)
                        bind()
                    }
                    4 -> uninstall(entry.packageName)
                }
            }
            .show()
    }

    private fun openAppInfo(packageName: String) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            toast(R.string.error_generic)
        }
    }

    private fun uninstall(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            toast(R.string.error_generic)
        }
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}
