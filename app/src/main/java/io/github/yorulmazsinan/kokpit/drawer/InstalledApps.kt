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

    fun packages(): Set<String> = launchable().mapTo(mutableSetOf()) { it.packageName }

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)
}
