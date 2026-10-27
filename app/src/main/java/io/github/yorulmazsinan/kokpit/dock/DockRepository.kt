package io.github.yorulmazsinan.kokpit.dock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.yorulmazsinan.kokpit.drawer.InstalledApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DockRepository(
    private val store: DataStore<Preferences>,
    private val apps: InstalledApps,
    scope: CoroutineScope,
) {
    private val installed = MutableStateFlow(apps.packages())

    val targets: StateFlow<Map<DockSlot, String?>> =
        combine(store.data.map { prefs -> DockSlot.entries.associateWith { prefs[key(it)] } }, installed) { overrides, pkgs ->
            DockSlot.entries.associateWith { resolveDockTarget(it, pkgs, overrides[it]) }
        }.stateIn(scope, SharingStarted.Eagerly, DockSlot.entries.associateWith { null })

    suspend fun setOverride(slot: DockSlot, packageName: String?) {
        store.edit { prefs ->
            if (packageName == null) prefs.remove(key(slot)) else prefs[key(slot)] = packageName
        }
    }

    /** Call when apps may have been installed or removed. */
    fun refreshInstalled() {
        installed.value = apps.packages()
    }

    private fun key(slot: DockSlot) = stringPreferencesKey("dock_${slot.name.lowercase()}")
}
