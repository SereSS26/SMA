package com.example.demo1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demo1.R
import com.example.demo1.data.SyncState
import com.example.demo1.data.remote.Account
import com.example.demo1.model.ThemeMode
import com.example.demo1.ui.theme.Demo1Theme

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    hideCompleted: Boolean,
    onHideCompletedChange: (Boolean) -> Unit,
    completedCount: Int,
    onClearCompleted: () -> Unit,
    syncState: SyncState,
    authBusy: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onRetrySync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(stringResource(R.string.settings_appearance))
        SettingsCard {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                colors = itemColors
            )
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                    ) {
                        Text(stringResource(mode.label))
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.settings_tasks))
        SettingsCard {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_hide_completed)) },
                supportingContent = { Text(stringResource(R.string.settings_hide_completed_desc)) },
                leadingContent = { Icon(Icons.Rounded.VisibilityOff, contentDescription = null) },
                trailingContent = { Switch(checked = hideCompleted, onCheckedChange = onHideCompletedChange) },
                colors = itemColors,
                modifier = Modifier.clickable { onHideCompletedChange(!hideCompleted) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            val canClear = completedCount > 0
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_clear_completed), color = MaterialTheme.colorScheme.error)
                },
                supportingContent = { Text(stringResource(R.string.settings_clear_completed_desc, completedCount)) },
                leadingContent = {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                colors = itemColors,
                modifier = Modifier
                    .alpha(if (canClear) 1f else 0.4f)
                    .clickable(enabled = canClear, onClick = onClearCompleted)
            )
        }

        SectionTitle(stringResource(R.string.settings_sync))
        SettingsCard {
            AccountSection(syncState, authBusy, onSignInWithGoogle, onSignOut, onRetrySync, itemColors)
        }

        SectionTitle(stringResource(R.string.settings_about))
        SettingsCard {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_title)) },
                supportingContent = { Text(stringResource(R.string.settings_version)) },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                colors = itemColors
            )
        }
    }
}

@Composable
private fun AccountSection(
    syncState: SyncState,
    authBusy: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    colors: ListItemColors,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val account = (syncState as? SyncState.Synced)?.account
    val failed = syncState is SyncState.Failed

    ListItem(
        headlineContent = {
            Text(
                when {
                    syncState is SyncState.NotConfigured -> stringResource(R.string.sync_local_only)
                    syncState is SyncState.Connecting -> stringResource(R.string.sync_connecting)
                    failed -> stringResource(R.string.sync_failed)
                    account!!.isAnonymous -> stringResource(R.string.account_guest)
                    else -> account!!.displayName ?: account.email ?: stringResource(R.string.account_google)
                }
            )
        },
        supportingContent = {
            Text(
                when (syncState) {
                    SyncState.NotConfigured -> stringResource(R.string.sync_local_only_desc)
                    SyncState.Connecting -> stringResource(R.string.sync_connecting_desc)
                    is SyncState.Failed -> stringResource(R.string.sync_failed_desc, syncState.message)
                    is SyncState.Synced -> if (syncState.account.isAnonymous) {
                        stringResource(R.string.account_guest_desc)
                    } else {
                        stringResource(R.string.account_google_desc, syncState.account.email ?: "")
                    }
                }
            )
        },
        leadingContent = {
            Icon(
                when {
                    syncState is SyncState.NotConfigured -> Icons.Rounded.CloudOff
                    syncState is SyncState.Connecting -> Icons.Rounded.CloudSync
                    failed -> Icons.Rounded.SyncProblem
                    account!!.isAnonymous -> Icons.Rounded.PersonOutline
                    else -> Icons.Rounded.AccountCircle
                },
                contentDescription = null,
                tint = when {
                    failed -> MaterialTheme.colorScheme.error
                    account != null && !account.isAnonymous -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = {
            if (failed) TextButton(onClick = onRetry) { Text(stringResource(R.string.sync_retry)) }
        },
        colors = colors
    )

    if (account != null) {
        val buttonModifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        if (account.isAnonymous) {
            Button(onClick = onSignInWithGoogle, enabled = !authBusy, modifier = buttonModifier) {
                if (authBusy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.login_google))
            }
        } else {
            OutlinedButton(onClick = { confirmSignOut = true }, enabled = !authBusy, modifier = buttonModifier) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.logout))
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    Demo1Theme {
        SettingsScreen(ThemeMode.SYSTEM, {}, false, {}, 2, {}, SyncState.Synced(Account("a1b2c3", isAnonymous = false, email = "ana@gmail.com", displayName = "Ana Pop")), false, {}, {}, {})
    }
}
