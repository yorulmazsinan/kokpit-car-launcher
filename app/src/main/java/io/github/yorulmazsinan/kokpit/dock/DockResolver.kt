package io.github.yorulmazsinan.kokpit.dock

/** Package to launch for [slot]: the user's choice if still installed, else the first known candidate. */
fun resolveDockTarget(slot: DockSlot, installed: Set<String>, override: String?): String? {
    if (slot == DockSlot.APPS) return null
    if (override != null && override in installed) return override
    return KnownPackages.candidates[slot].orEmpty().firstOrNull { it in installed }
}
