package io.github.yorulmazsinan.kokpit.drawer

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import java.text.Collator

data class AppEntry(val packageName: String, val label: String, val component: ComponentName)

class InstalledApps(private val pm: PackageManager, private val ownPackage: String) {
    fun launchable(): List<AppEntry> {
        val collator = Collator.getInstance()
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != ownPackage }
            .map {
                AppEntry(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    component = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                )
            }
            .distinctBy { it.component }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** Package names only: no labels or sorting, cheap enough for every return to Home. */
    fun packages(): Set<String> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .mapTo(mutableSetOf()) { it.activityInfo.packageName }
            .apply { remove(ownPackage) }

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)
}

/** Launches an app in its own task, the way a launcher must (Home must not destroy it). */
fun launcherIntent(component: ComponentName): Intent =
    Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
