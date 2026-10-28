package io.github.yorulmazsinan.kokpit.drawer

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import io.github.yorulmazsinan.kokpit.dock.DockTarget
import io.github.yorulmazsinan.kokpit.dock.parseDockTarget
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

    /** All installed package names (vendor screens live in apps without a launcher icon); cheap, no labels. */
    fun packages(): Set<String> =
        pm.getInstalledApplications(0).mapTo(mutableSetOf()) { it.packageName }.apply { remove(ownPackage) }

    /** Launch intent for a stored dock target (`package` or `package/class`). */
    fun launchIntent(target: String): Intent? = when (val t = parseDockTarget(target)) {
        is DockTarget.Package -> pm.getLaunchIntentForPackage(t.packageName)
        is DockTarget.Component -> launcherIntent(ComponentName(t.packageName, t.className))
    }

    /** The target app's launcher icon, small enough for a dock button. */
    fun icon(target: String): android.graphics.Bitmap? = runCatching {
        val drawable = when (val t = parseDockTarget(target)) {
            is DockTarget.Package -> pm.getApplicationIcon(t.packageName)
            is DockTarget.Component -> pm.getActivityIcon(ComponentName(t.packageName, t.className))
        }
        androidx.core.graphics.drawable.DrawableCompat.wrap(drawable).let { d ->
            val bitmap = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            d.setBounds(0, 0, 96, 96)
            d.draw(canvas)
            bitmap
        }
    }.getOrNull()

    /** Human label for a stored dock target: the screen's own label, or the app's. */
    fun label(target: String): String = runCatching {
        when (val t = parseDockTarget(target)) {
            is DockTarget.Package -> pm.getApplicationLabel(pm.getApplicationInfo(t.packageName, 0)).toString()
            is DockTarget.Component -> pm.getActivityInfo(ComponentName(t.packageName, t.className), 0).loadLabel(pm).toString()
        }
    }.getOrDefault(target)
}

/** Launches an app in its own task, the way a launcher must (Home must not destroy it). */
fun launcherIntent(component: ComponentName): Intent =
    Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
