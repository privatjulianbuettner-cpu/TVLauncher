package com.btx.tvlauncher

import android.content.Context
import android.content.Intent

/** Lädt installierte Apps und verwaltet Favoriten, ausgeblendete Apps und Einstellungen. */
class AppRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Alle startbaren Apps (Leanback bevorzugt), alphabetisch sortiert. */
    fun loadApps(): List<AppEntry> {
        val pm = context.packageManager
        val result = LinkedHashMap<String, AppEntry>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(query, 0)) {
                val activity = info.activityInfo ?: continue
                if (activity.packageName == context.packageName) continue
                if (result.containsKey(activity.packageName)) continue
                result[activity.packageName] = AppEntry(
                    label = info.loadLabel(pm).toString(),
                    packageName = activity.packageName,
                    activityName = activity.name,
                    icon = try { info.loadIcon(pm) } catch (_: Exception) { null },
                    banner = try { activity.loadBanner(pm) } catch (_: Exception) { null }
                )
            }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    // ---- Favoriten (Reihenfolge bleibt erhalten) ----

    fun favorites(): List<String> =
        (prefs.getString(KEY_FAVORITES, "") ?: "").split(",").filter { it.isNotEmpty() }

    fun toggleFavorite(packageName: String) {
        val list = favorites().toMutableList()
        if (!list.remove(packageName)) list.add(packageName)
        prefs.edit().putString(KEY_FAVORITES, list.joinToString(",")).apply()
    }

    // ---- Ausgeblendete Apps ----

    fun hidden(): Set<String> =
        HashSet(prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet())

    fun hide(packageName: String) = setHidden(hidden() + packageName)

    fun setHidden(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN, HashSet(packages)).apply()
    }

    // ---- Darstellung ----

    var columns: Int
        get() = prefs.getInt(KEY_COLUMNS, 6).coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        set(value) = prefs.edit().putInt(KEY_COLUMNS, value).apply()

    var background: Int
        get() = prefs.getInt(KEY_BACKGROUND, 0)
        set(value) = prefs.edit().putInt(KEY_BACKGROUND, value).apply()

    companion object {
        const val MIN_COLUMNS = 4
        const val MAX_COLUMNS = 8
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HIDDEN = "hidden"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_BACKGROUND = "background"
    }
}
