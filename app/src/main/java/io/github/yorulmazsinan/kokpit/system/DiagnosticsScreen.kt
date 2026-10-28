package io.github.yorulmazsinan.kokpit.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.storage.removableAppDirs
import io.github.yorulmazsinan.kokpit.storage.writeToDirs
import io.github.yorulmazsinan.kokpit.ui.components.ScreenScaffold
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens

@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val text = remember { DiagnosticsCollector(context).collect().toText() }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }.isSuccess
            Toast.makeText(context, if (ok) R.string.diagnostics_saved else R.string.diagnostics_save_failed, Toast.LENGTH_SHORT).show()
        }
    }
    ScreenScaffold(stringResource(R.string.diagnostics_title), onBack) {
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { copyToClipboard(context, text) }, modifier = Modifier.heightIn(min = Dimens.MinTouch)) {
                Text(stringResource(R.string.diagnostics_copy))
            }
            OutlinedButton(onClick = {
                val written = writeToDirs(removableAppDirs(context), "kokpit-diagnostics.txt", text)
                if (written.isNotEmpty()) {
                    Toast.makeText(context, R.string.diagnostics_saved_usb, Toast.LENGTH_LONG).show()
                } else {
                    runCatching { saveLauncher.launch("kokpit-diagnostics.txt") }
                        .onFailure { Toast.makeText(context, R.string.start_failed, Toast.LENGTH_SHORT).show() }
                }
            }, modifier = Modifier.heightIn(min = Dimens.MinTouch)) {
                Text(stringResource(R.string.diagnostics_save))
            }
        }
        val scope = rememberCoroutineScope()
        OutlinedButton(
            onClick = {
                scope.launch {
                    val dirs = removableAppDirs(context)
                    val count = withContext(Dispatchers.IO) { exportVendorApks(context, dirs) }
                    val message = if (dirs.isEmpty() || count == 0) context.getString(R.string.vendor_export_failed)
                    else context.getString(R.string.vendor_export_done, count)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = Dimens.MinTouch),
        ) { Text(stringResource(R.string.vendor_export)) }
        SelectionContainer {
            Text(
                text,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Kokpit diagnostics", text))
    Toast.makeText(context, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show()
}
