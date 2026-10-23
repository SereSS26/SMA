package com.example.demo1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demo1.R
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
        SettingsScreen(ThemeMode.SYSTEM, {}, false, {}, 2, {})
    }
}
