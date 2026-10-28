package io.github.yorulmazsinan.kokpit.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yorulmazsinan.kokpit.AppContainer
import io.github.yorulmazsinan.kokpit.BuildConfig
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.home.ThemeMode
import io.github.yorulmazsinan.kokpit.system.safeStart
import io.github.yorulmazsinan.kokpit.ui.components.HelpText
import io.github.yorulmazsinan.kokpit.ui.components.ListRow
import io.github.yorulmazsinan.kokpit.ui.components.ScreenScaffold
import io.github.yorulmazsinan.kokpit.ui.components.SectionHeader
import io.github.yorulmazsinan.kokpit.ui.components.ToggleRow
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens
import kotlinx.coroutines.launch

private const val DAVX5 = "at.bitfire.davdroid"
private const val REPO_URL = "https://github.com/yorulmazsinan/kokpit"

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onPickApp: (DockSlot) -> Unit,
    onOpenIntro: () -> Unit,
    onOpenSystemTools: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val dock by container.dock.targets.collectAsStateWithLifecycle()
    fun update(transform: (KokpitSettings) -> KokpitSettings) {
        scope.launch { container.settings.update(transform) }
    }

    ScreenScaffold(stringResource(R.string.settings_title), onBack) {
        LazyColumn {
            item { SectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                val modes = listOf(ThemeMode.DARK to R.string.theme_dark, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.AUTO to R.string.theme_auto)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    modes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { update { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                            modifier = Modifier.heightIn(min = Dimens.MinTouch),
                        ) { Text(stringResource(label)) }
                    }
                }
            }
            item { HelpText(stringResource(R.string.theme_auto_help)) }

            item { SectionHeader(stringResource(R.string.settings_dock)) }
            DockSlot.entries.filter { it != DockSlot.APPS }.forEach { slot ->
                item(key = "dock_${slot.name}") {
                    val target = dock[slot]
                    val label = target?.let(container.installedApps::label) ?: stringResource(R.string.settings_not_set)
                    ListRow(stringResource(slot.label), label) { onPickApp(slot) }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_connection)) }
            item {
                ToggleRow(stringResource(R.string.settings_auto_wifi), settings.autoWifi, stringResource(R.string.settings_auto_wifi_help)) { on ->
                    update { it.copy(autoWifi = on) }
                }
            }
            item {
                var ssid by remember(settings.hotspotSsid) { mutableStateOf(settings.hotspotSsid.orEmpty()) }
                var password by remember(settings.hotspotPassword) { mutableStateOf(settings.hotspotPassword.orEmpty()) }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(ssid, { ssid = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.settings_hotspot_name)) }, singleLine = true)
                    OutlinedTextField(
                        password, { password = it }, Modifier.weight(1f),
                        label = { Text(stringResource(R.string.settings_hotspot_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                container.settings.update {
                                    it.copy(hotspotSsid = ssid.trim().ifEmpty { null }, hotspotPassword = password.ifEmpty { null }, hotspotPasswordUnreadable = false)
                                }
                                container.wifi.reconnect(container.scope)
                            }
                        },
                        modifier = Modifier.heightIn(min = Dimens.MinTouch),
                    ) { Text(stringResource(R.string.save)) }
                }
            }
            if (settings.hotspotPasswordUnreadable) {
                item { HelpText(stringResource(R.string.settings_password_reenter)) }
            }
            item { HelpText(stringResource(R.string.settings_iphone_hotspot_help)) }
            item {
                ListRow(stringResource(R.string.settings_bt_auto_connect), stringResource(R.string.settings_bt_auto_connect_help)) {
                    dock[DockSlot.BLUETOOTH]?.let(container.installedApps::launchIntent)?.let { safeStart(context, it) }
                        ?: onPickApp(DockSlot.BLUETOOTH)
                }
            }

            item { SectionHeader(stringResource(R.string.dock_carplay)) }
            item {
                ToggleRow(stringResource(R.string.settings_carplay_usb), settings.autoCarPlayOnUsb, stringResource(R.string.settings_carplay_usb_help)) { on ->
                    update { it.copy(autoCarPlayOnUsb = on) }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_intro)) }
            item { ListRow(stringResource(R.string.settings_intro_manage), onClick = onOpenIntro) }

            item { SectionHeader(stringResource(R.string.settings_calendar)) }
            item { HelpText(stringResource(R.string.settings_calendar_help)) }
            item {
                ListRow(stringResource(R.string.settings_open_davx5)) {
                    val intent = container.installedApps.launchIntent(DAVX5)
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$DAVX5"))
                    safeStart(context, intent)
                }
            }

            item { SectionHeader(stringResource(R.string.settings_system)) }
            item { ListRow(stringResource(R.string.system_tools_title), onClick = onOpenSystemTools) }
            item { ListRow(stringResource(R.string.diagnostics_title), onClick = onOpenDiagnostics) }

            item { SectionHeader(stringResource(R.string.settings_about)) }
            item { ListRow("Kokpit ${BuildConfig.VERSION_NAME}", stringResource(R.string.settings_license)) { } }
            item { ListRow(stringResource(R.string.settings_source_code), REPO_URL) { safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) } }
        }
    }
}

