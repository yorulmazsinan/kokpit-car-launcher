package io.github.yorulmazsinan.kokpit.system

import android.content.Context
import java.io.File

/** Head-unit vendor apps worth studying to drive radio, Bluetooth and CarPlay from Kokpit. */
private val VENDOR_PREFIXES = listOf("com.ts.", "com.autochips.", "com.suding.", "com.forfan.", "net.easyconn")

fun vendorPackagesToExport(all: List<String>): List<String> =
    all.filter { pkg -> VENDOR_PREFIXES.any { pkg.startsWith(it) } }.distinct().sorted()

fun exportFileName(packageName: String): String = "$packageName.apk"

/** Copies vendor APKs into `<dir>/vendor/`; returns how many were copied. */
fun exportVendorApks(context: Context, dirs: List<File>): Int {
    val pm = context.packageManager
    val packages = vendorPackagesToExport(pm.getInstalledApplications(0).map { it.packageName })
    var copied = 0
    dirs.forEach { dir ->
        val target = File(dir, "vendor").apply { mkdirs() }
        packages.forEach { pkg ->
            runCatching {
                File(pm.getApplicationInfo(pkg, 0).sourceDir).copyTo(File(target, exportFileName(pkg)), overwrite = true)
                copied++
            }
        }
    }
    return copied
}
