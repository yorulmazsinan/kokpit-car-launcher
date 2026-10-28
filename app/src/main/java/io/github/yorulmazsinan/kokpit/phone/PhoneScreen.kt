package io.github.yorulmazsinan.kokpit.phone

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallMissed
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.calls.CallEntry
import io.github.yorulmazsinan.kokpit.calls.CallType
import io.github.yorulmazsinan.kokpit.system.safeStart
import io.github.yorulmazsinan.kokpit.ui.components.HeaderClock
import io.github.yorulmazsinan.kokpit.ui.components.ScreenScaffold
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitFonts
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PhoneTab(val label: Int) { RECENT(R.string.phone_recent), CONTACTS(R.string.phone_contacts), FAVOURITES(R.string.phone_favourites) }

@Composable
fun PhoneScreen(calls: List<CallEntry>, onBack: () -> Unit) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    var reload by remember { mutableStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { reload++ }
    LaunchedEffect(Unit) { permission.launch(Manifest.permission.READ_CONTACTS) }
    LaunchedEffect(reload) { contacts = withContext(Dispatchers.IO) { loadContacts(context) } }
    fun dial(number: String) {
        if (number.isNotBlank()) safeStart(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
    }
    PhoneContent(calls, contacts, onBack, ::dial)
}

@Composable
fun PhoneContent(calls: List<CallEntry>, contacts: List<Contact>, onBack: () -> Unit, onDial: (String) -> Unit) {
    var tab by remember { mutableStateOf(PhoneTab.RECENT) }
    var typed by remember { mutableStateOf("") }
    ScreenScaffold(stringResource(R.string.phone_title), onBack, actions = { HeaderClock() }) {
        Row(
            Modifier.fillMaxSize().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Gap),
        ) {
            PhoneCard(Modifier.weight(1.15f).fillMaxHeight()) {
                Column {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        PhoneTab.entries.forEachIndexed { i, t ->
                            SegmentedButton(
                                selected = tab == t,
                                onClick = { tab = t },
                                shape = SegmentedButtonDefaults.itemShape(i, PhoneTab.entries.size),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(t.label)) }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    LazyColumn(Modifier.testTag("phone_list")) {
                        when (tab) {
                            PhoneTab.RECENT -> if (calls.isEmpty()) item { Empty(R.string.phone_no_calls) } else items(calls) { CallRow(it, onDial) }
                            PhoneTab.CONTACTS -> if (contacts.isEmpty()) item { Empty(R.string.phone_no_contacts) } else items(contacts) { ContactRow(it, onDial) }
                            PhoneTab.FAVOURITES -> {
                                val starred = contacts.filter { it.starred }
                                if (starred.isEmpty()) item { Empty(R.string.phone_no_favourites) } else items(starred) { ContactRow(it, onDial) }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneCard(Modifier.fillMaxWidth()) {
                    Text(
                        typed.ifEmpty { " " },
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = KokpitFonts.condensed, letterSpacing = 2.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("dial_display"),
                        maxLines = 1,
                    )
                }
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "⌫")
                keys.chunked(3).forEach { rowKeys ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowKeys.forEach { key ->
                            Key(key, Modifier.weight(1f).fillMaxHeight()) {
                                typed = if (key == "⌫") dialBackspace(typed) else dialAppend(typed, key.single())
                            }
                        }
                    }
                }
                Surface(
                    onClick = { onDial(typed) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouch),
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Call, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.phone_call), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneCard(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier,
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Box(Modifier.padding(Dimens.CardPadding)) { content() } }
}

@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "⌫") {
                Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.phone_delete), tint = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(label, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = KokpitFonts.condensed), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CallRow(call: CallEntry, onDial: (String) -> Unit) {
    val (icon, tint) = when (call.type) {
        CallType.MISSED -> Icons.Outlined.CallMissed to MaterialTheme.colorScheme.error
        CallType.OUTGOING -> Icons.Outlined.CallMade to MaterialTheme.colorScheme.onSurfaceVariant
        CallType.INCOMING -> Icons.Outlined.CallReceived to MaterialTheme.colorScheme.primary
    }
    val time = Instant.ofEpochMilli(call.timeMs).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM HH:mm"))
    ListLine(icon, tint, call.displayName, time) { onDial(call.number) }
}

@Composable
private fun ContactRow(contact: Contact, onDial: (String) -> Unit) {
    ListLine(
        if (contact.starred) Icons.Outlined.Star else Icons.Outlined.Person,
        if (contact.starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        contact.name.ifBlank { contact.number },
        contact.number,
    ) { onDial(contact.number) }
}

@Composable
private fun ListLine(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, title: String, trailing: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouch).clickable(onClick = onClick).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Outlined.Call, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Empty(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
}
