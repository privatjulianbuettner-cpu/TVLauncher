package com.btx.tvlauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class SettingsActivity : Activity() {

    private lateinit var repo: AppRepository
    private lateinit var status: TextView
    private lateinit var btnColumns: Button

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        repo = AppRepository(this)

        window.setBackgroundDrawable(Backgrounds.drawable(repo.background))

        status = findViewById(R.id.status)
        btnColumns = findViewById(R.id.btn_columns)

        findViewById<Button>(R.id.btn_default).setOnClickListener { makeDefaultLauncher() }
        findViewById<Button>(R.id.btn_system).setOnClickListener { open(Settings.ACTION_SETTINGS) }
        findViewById<Button>(R.id.btn_wifi).setOnClickListener { open(Settings.ACTION_WIFI_SETTINGS) }
        findViewById<Button>(R.id.btn_background).setOnClickListener {
            repo.background = (repo.background + 1) % Backgrounds.count
            window.setBackgroundDrawable(Backgrounds.drawable(repo.background))
        }
        btnColumns.setOnClickListener {
            val next = repo.columns + 1
            repo.columns = if (next > AppRepository.MAX_COLUMNS) AppRepository.MIN_COLUMNS else next
            updateColumnsLabel()
        }
        findViewById<Button>(R.id.btn_hidden).setOnClickListener { showHiddenDialog() }

        findViewById<TextView>(R.id.version).text = getString(R.string.about_version, versionName())
        updateColumnsLabel()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }

    private fun updateColumnsLabel() {
        btnColumns.text = getString(R.string.btn_columns, repo.columns)
    }

    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    private fun updateStatus() {
        status.setText(if (isDefaultLauncher()) R.string.status_active else R.string.status_inactive)
    }

    /** "Make main launcher": Rollen-Dialog (Android 10+), sonst Home-Einstellungen. */
    private fun makeDefaultLauncher() {
        if (isDefaultLauncher()) {
            Toast.makeText(this, R.string.status_active, Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_ROLE_HOME
                    )
                    return
                } catch (_: Exception) {
                    // weiter mit Fallbacks
                }
            }
        }
        if (open(Settings.ACTION_HOME_SETTINGS)) return
        // Letzter Fallback: Home-Auswahldialog des Systems auslösen
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Toast.makeText(this, R.string.hint_default_manual, Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.hint_default_manual, Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Ergebnis des Rollen-Dialogs")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ROLE_HOME) updateStatus()
    }

    private fun open(action: String): Boolean = try {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
        false
    }

    private fun showHiddenDialog() {
        executor.execute {
            val apps = repo.loadApps()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (apps.isEmpty()) return@runOnUiThread
                val hidden = repo.hidden()
                val labels = apps.map { it.label }.toTypedArray()
                val checked = BooleanArray(apps.size) { apps[it].packageName in hidden }
                AlertDialog.Builder(this)
                    .setTitle(R.string.btn_hidden)
                    .setMultiChoiceItems(labels, checked) { _, index, isChecked ->
                        checked[index] = isChecked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val selected = apps.indices.filter { checked[it] }
                            .map { apps[it].packageName }.toSet()
                        repo.setHidden(selected)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    companion object {
        private const val REQUEST_ROLE_HOME = 1001
    }
}
