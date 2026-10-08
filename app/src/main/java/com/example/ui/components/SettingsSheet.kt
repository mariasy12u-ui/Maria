package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccentColor
import com.example.data.model.BrowserSettings
import com.example.data.model.SearchEngine
import com.example.data.model.StartupOption
import com.example.data.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: BrowserSettings,
    onUpdateSettings: (BrowserSettings) -> Unit,
    onClearAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Nova Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // SECTION: APPEARANCE
            SectionHeader(title = "Appearance", icon = Icons.Default.Palette)

            // Theme Mode
            Text("Theme Mode", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeMode.values().forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onUpdateSettings(settings.copy(themeMode = mode)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Accent Color
            Text("Accent Color", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AccentColor.values().forEach { color ->
                    val isSelected = settings.accentColor == color
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(color.hex))
                            .clickable { onUpdateSettings(settings.copy(accentColor = color)) }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Toggles
            SettingToggleRow(
                title = "Show Bookmarks Bar",
                subtitle = "Display quick bookmarks strip under address bar",
                checked = settings.showBookmarksBar,
                onCheckedChange = { onUpdateSettings(settings.copy(showBookmarksBar = it)) }
            )

            SettingToggleRow(
                title = "Show Home Button",
                subtitle = "Include home icon in the top navigation toolbar",
                checked = settings.showHomeButton,
                onCheckedChange = { onUpdateSettings(settings.copy(showHomeButton = it)) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // SECTION: SEARCH ENGINE
            SectionHeader(title = "Search Engine", icon = Icons.Default.Search)

            Text("Default Provider", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))

            SearchEngine.values().forEach { engine ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateSettings(settings.copy(searchEngine = engine)) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = settings.searchEngine == engine,
                        onClick = { onUpdateSettings(settings.copy(searchEngine = engine)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = engine.displayName, fontSize = 14.sp)
                }
            }

            SettingToggleRow(
                title = "Search Suggestions",
                subtitle = "Show query and URL suggestions when typing in omnibox",
                checked = settings.searchSuggestionsEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(searchSuggestionsEnabled = it)) }
            )

            SettingToggleRow(
                title = "Record Browsing History",
                subtitle = "Keep visit history for quick reopen and suggestions",
                checked = settings.searchHistoryEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(searchHistoryEnabled = it)) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // SECTION: STARTUP
            SectionHeader(title = "Startup Behavior", icon = Icons.Default.PowerSettingsNew)

            StartupOption.values().forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateSettings(settings.copy(startupOption = option)) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = settings.startupOption == option,
                        onClick = { onUpdateSettings(settings.copy(startupOption = option)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = option.displayName, fontSize = 14.sp)
                }
            }

            if (settings.startupOption == StartupOption.CUSTOM_URL) {
                OutlinedTextField(
                    value = settings.customStartupUrl,
                    onValueChange = { onUpdateSettings(settings.copy(customStartupUrl = it)) },
                    label = { Text("Custom URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // SECTION: PRIVACY & SECURITY
            SectionHeader(title = "Privacy & Security", icon = Icons.Default.Shield)

            SettingToggleRow(
                title = "Send \"Do Not Track\"",
                subtitle = "Request websites not to collect or track your browsing data",
                checked = settings.doNotTrack,
                onCheckedChange = { onUpdateSettings(settings.copy(doNotTrack = it)) }
            )

            SettingToggleRow(
                title = "Block Third-Party Cookies",
                subtitle = "Prevent cross-site trackers from identifying your session",
                checked = settings.blockThirdPartyCookies,
                onCheckedChange = { onUpdateSettings(settings.copy(blockThirdPartyCookies = it)) }
            )

            SettingToggleRow(
                title = "Enable JavaScript",
                subtitle = "Required for interactive web apps and media playback",
                checked = settings.javascriptEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(javascriptEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onClearAllData,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Browsing History, Cookies & Cache")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // SECTION: ABOUT NOVA BROWSER
            SectionHeader(title = "About Nova Browser", icon = Icons.Default.Security)

            Text(
                text = "Nova Browser v1.0.0",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Fast, modern web browser engineered with Jetpack Compose & Android WebKit.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
